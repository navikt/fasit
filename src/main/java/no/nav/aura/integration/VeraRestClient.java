package no.nav.aura.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class VeraRestClient {

    public static final int MAX_RETRIES = 5;

    private static final Logger log = LoggerFactory.getLogger(VeraRestClient.class);
    private static final String UNDEPLOYMENT = "undeployment";

    private String veraUrl;

    private final RestTemplate restTemplate;

    public VeraRestClient(String url) {
        log.info("Using " + url + " as vera endpoint url");
        this.restTemplate = new RestTemplate();
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
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Notified VERA of {} with info {} at endpoint {}", deploymentMode, requestBody, url);
            } else {
                log.warn("Unable to update VERA, got response {} {}", 
                        response.getStatusCodeValue(), response.getStatusCode().getReasonPhrase());
            }
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
        }
    }
}
