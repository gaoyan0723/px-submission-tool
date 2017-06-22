package uk.ac.ebi.pride.gui.task;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.archiverepomongo.model.User;
import uk.ac.ebi.pride.archive.dataprovider.person.Title;
import uk.ac.ebi.pride.archive.submission.model.user.ContactDetail;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskAdapter;
import uk.ac.ebi.pride.web.util.template.SecureRestTemplateFactory;

import java.util.Arrays;
import java.util.Properties;

/**
 * GetPrideUserDetailTask retrieves pride user details using pride web service
 *
 * @author Rui Wang
 * @version $Id$
 */
public class GetPrideUserDetailTask extends TaskAdapter<ContactDetail, String> {

    private static final Logger logger = LoggerFactory.getLogger(GetPrideUserDetailTask.class);

    private RestTemplate restTemplate;
    private String username;
    private String password;

    /**
     * Constructor
     *
     * @param userName pride user name
     * @param password pride password
     */
    public GetPrideUserDetailTask(String userName, char[] password) {
        this.restTemplate = SecureRestTemplateFactory.getTemplate(userName, new String(password));
        this.username = userName;
        this.password = String.valueOf(password);
    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     * <p/>
     * <p/>
     * Note that this method is executed only once.
     * <p/>
     * <p/>
     * Note: this method is executed in a background thread.
     *
     * @return the computed result
     * @throws Exception if unable to compute a result
     */
    @Override
    protected ContactDetail doInBackground() throws Exception {
        ContactDetail result = attemptNewUserWsLogin();
        if (result==null) { // old User WS
            DesktopContext context = App.getInstance().getDesktopContext();
            String baseUrl = context.getProperty("pride.user.detail.url");
            try {
                // set proxy
                Properties props = System.getProperties();
                String proxyHost = props.getProperty("http.proxyHost");
                String proxyPort = props.getProperty("http.proxyPort");
                if (proxyHost != null && proxyPort != null) {
                    logger.info("Using proxy server {} and port {}", proxyHost, proxyPort);
                    HttpComponentsClientHttpRequestFactory factory = ((HttpComponentsClientHttpRequestFactory) restTemplate.getRequestFactory());
                    DefaultHttpClient defaultHttpClient = (DefaultHttpClient) factory.getHttpClient();
                    HttpHost proxy = new HttpHost(proxyHost.trim(), Integer.parseInt(proxyPort));
                    defaultHttpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
                }
                return restTemplate.getForObject(baseUrl, ContactDetail.class);
            } catch (Exception ex) {
                publish("Failed to login, please check user name or password");
            }
        }
        return result;
    }

    private ContactDetail attemptNewUserWsLogin() {
        ContactDetail result = null;
        try {
            User user = loginUser(username, password);
            if (user!=null && !StringUtils.isEmpty(user.getEmail())) {
                result = new ContactDetail();
                result.setEmail(user.getEmail());
                result.setAffiliation(user.getAffiliation());
                result.setTitle(Title.fromString(user.getTitle()));
                result.setFirstName(user.getFirstName());
                result.setLastName(user.getSecondName());
            } else {
                throw new Exception();
            }
        } catch (Exception ex) {
            // publish("Failed to login, please check user name or password");
            // do nothing for now, until migrated fully to new User WS
        }
        return result;
    }

    static User loginUser(String username, String password) throws Exception{
        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        Properties props = System.getProperties(); // set proxy
        String proxyHost = props.getProperty("http.proxyHost");
        String proxyPort = props.getProperty("http.proxyPort");
        if (proxyHost != null && proxyPort != null) {
            logger.info("Using proxy server {} and port {}", proxyHost, proxyPort);
            HttpComponentsClientHttpRequestFactory factory = ((HttpComponentsClientHttpRequestFactory) restTemplate.getRequestFactory());
            DefaultHttpClient defaultHttpClient = (DefaultHttpClient) factory.getHttpClient();
            HttpHost proxy = new HttpHost(proxyHost.trim(), Integer.parseInt(proxyPort));
            defaultHttpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }
        User loginUser = new User();
        loginUser.setEmail(username);
        loginUser.setPassword(password);
        return restTemplate.postForObject(
            App.getInstance().getDesktopContext().getProperty("pride.user.detail.url.new") + "/loginuser",
            new HttpEntity<>(loginUser),
            User.class);
    }
}

