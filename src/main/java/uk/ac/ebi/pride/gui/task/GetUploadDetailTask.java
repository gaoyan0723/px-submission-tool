package uk.ac.ebi.pride.gui.task;

import org.apache.http.HttpHost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadMethod;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskAdapter;
import uk.ac.ebi.pride.web.util.template.SecureRestTemplateFactory;

import java.util.Properties;

/**
 * Task for getting upload details from PRIDE web service
 *
 * @author Rui Wang
 * @version $Id$
 */
public class GetUploadDetailTask extends TaskAdapter<UploadDetail, String> {

    private final RestTemplate restTemplate;
    private final UploadMethod method;

    /**
     * Constructor
     *
     * @param userName user name
     * @param password password
     */
    public GetUploadDetailTask(UploadMethod method, String userName, char[] password) {
        this.restTemplate = SecureRestTemplateFactory.getTemplate(userName, new String(password));
        this.method = method;
    }

    @Override
    protected UploadDetail doInBackground() throws Exception {

        DesktopContext context = App.getInstance().getDesktopContext();
        String baseUrl = context.getProperty("px.upload.detail.url");

        UploadDetail uploadDetail = null;
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
            uploadDetail = restTemplate.getForObject(baseUrl, UploadDetail.class, method.getMethod());
        } catch(Exception ex){
            // port blocked, dealt with later
        }
        return uploadDetail;
    }
}
