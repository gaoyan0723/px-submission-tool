package uk.ac.ebi.pride.gui.task;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.archiverepomongo.model.ProjectReference;
import uk.ac.ebi.pride.archive.archiverepomongo.model.User;
import uk.ac.ebi.pride.archive.dataprovider.person.Title;
import uk.ac.ebi.pride.archive.submission.model.project.ProjectDetail;
import uk.ac.ebi.pride.archive.submission.model.project.ProjectDetailList;
import uk.ac.ebi.pride.archive.submission.model.user.ContactDetail;
import uk.ac.ebi.pride.gui.util.Constant;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.web.util.template.SecureRestTemplateFactory;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Task for get PX submission details using given user name and password
 *
 * @author Rui Wang
 * @version $Id$
 */
public class GetPXSubmissionDetailTask extends AbstractWebServiceTask<Set<String>> {
    private static final Logger logger = LoggerFactory.getLogger(GetPXSubmissionDetailTask.class);
    private RestTemplate restTemplate;
    private String username, password;

    public GetPXSubmissionDetailTask(String userName, String password) {
        this.restTemplate = SecureRestTemplateFactory.getTemplate(userName, password);
        this.username = userName;
        this.password = password;
    }

    @Override
    protected Set<String> doInBackground() throws Exception {
        Set<String> pxAccessions = attemptNewUserWsGetAccessions();
        if (pxAccessions==null) {
            pxAccessions = new LinkedHashSet<String>();
            String baseUrl = App.getInstance().getDesktopContext().getProperty("px.submission.detail.url");
            try {
                Properties props = System.getProperties();
                String proxyHost = props.getProperty("http.proxyHost");
                String proxyPort = props.getProperty("http.proxyPort");

                if (proxyHost != null && proxyPort != null) {
                    HttpComponentsClientHttpRequestFactory factory = ((HttpComponentsClientHttpRequestFactory) restTemplate.getRequestFactory());
                    DefaultHttpClient defaultHttpClient = (DefaultHttpClient) factory.getHttpClient();
                    HttpHost proxy = new HttpHost(proxyHost.trim(), Integer.parseInt(proxyPort));
                    defaultHttpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
                }
                ProjectDetailList projectDetailList = restTemplate.getForObject(baseUrl, ProjectDetailList.class);

                for (ProjectDetail projectDetail : projectDetailList.getProjectDetails()) {
                    String accession = projectDetail.getAccession();
                    Matcher matcher = Constant.PX_ACC_PATTERN.matcher(accession);
                    if (matcher.matches()) {
                        pxAccessions.add(accession);
                    }
                }
            } catch (Exception ex) {
                failedToLogin(ex);
            }
        }
        return pxAccessions;
    }

    private Set<String> attemptNewUserWsGetAccessions() {
        Set<String> pxAccessions = null;
        try {
            User user = GetPrideUserDetailTask.loginUser(username, password);
            if (user != null && !StringUtils.isEmpty(user.getEmail())) {
                pxAccessions = new LinkedHashSet<>();
                if (user.getProjectReferences() != null && user.getProjectReferences().length > 0) {
                    for (ProjectReference projectReference : user.getProjectReferences()) {
                        Matcher matcher = Constant.PX_ACC_PATTERN.matcher(projectReference.getAccession());
                        if (matcher.matches()) {
                            pxAccessions.add(projectReference.getAccession());
                        }
                    }
                }
            } else {
                throw new Exception();
            }
        } catch (Exception ex) {
            // failedToLogin(ex);
            // do nothing for now, until migrated fully to new User WS
        }
        return pxAccessions;
    }

    private void failedToLogin(Exception ex) {
        logger.warn("Failed to login to retrieve project details", ex);
        Runnable eventDispatcher = () -> {
            App app = (App) App.getInstance(); // show warning dialog
            JOptionPane.showConfirmDialog(app.getMainFrame(),
                app.getDesktopContext().getProperty("pride.login.resubmission.error.message"),
                app.getDesktopContext().getProperty("pride.login.error.title"),
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE);
        };
        EventQueue.invokeLater(eventDispatcher);
    }
}
