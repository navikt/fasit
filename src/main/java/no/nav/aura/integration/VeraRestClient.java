package no.nav.aura.integration;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VeraRestClient {

    public static final int MAX_RETRIES = 5;

    private static final Logger log = LoggerFactory.getLogger(VeraRestClient.class);
    private static final String UNDEPLOYMENT = "undeployment";

    private String veraUrl;

    private Client httpClient;

    public VeraRestClient(String url) {
        log.info("Using " + url + " as vera endpoint url");
        httpClient = ClientBuilder.newClient();
        veraUrl = url;
    }

    public void notifyVeraOfUndeployment(String applicationName, String environmentName, String undeployBy) {
        postToVera(UNDEPLOYMENT, veraUrl, createRequestBody(applicationName, null, environmentName, undeployBy, UNDEPLOYMENT), 1);
    }

    private Map<String, String> createRequestBody(String applicationName, String version, String environmentName, String user, String deploymentMode) {
        Map<String, String> map = new HashMap<>();
        map.put("application", applicationName);
        map.put("environment", environmentName);
        map.put("deployedBy", user);

        return map;
    }

    private void postToVera(String deploymentMode, String url, Map<String, String> requestBody, int retries) {
        Response response = null;
        try {
            response = httpClient.target(url).request().post(Entity.entity(requestBody, MediaType.APPLICATION_JSON));
            StatusType responseStatus = response.getStatusInfo();

            if (responseStatus.getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
                log.info("Notified VERA of {} with info {} at endpoint {}", deploymentMode, requestBody, url);
            } else {
                log.warn("Unable to update VERA, got response {} {}", responseStatus.getStatusCode(), responseStatus.getReasonPhrase());
            }
            response.close();

        } catch (Exception e) {
            if (retries < MAX_RETRIES) {
                try {
                    log.warn("Unable to update vera, will sleep and retry. " + retries + " / " + MAX_RETRIES);
                    Thread.sleep(3000);
                } catch (InterruptedException e1) {
                    log.error("Caught exception while sleeping thread", e1);
                }
                postToVera(deploymentMode, url, requestBody, retries + 1);
            } else {
                log.error("Unable to update Vera after " + MAX_RETRIES + " retries, giving up.");
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }

    }
}
