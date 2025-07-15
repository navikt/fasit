package no.nav.aura.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.Environment;

import java.util.HashMap;
import java.util.Map;

public class VeraRestClient {

    public static final int MAX_RETRIES = 5;

    private static final Logger log = LoggerFactory.getLogger(VeraRestClient.class);
    private static final String UNDEPLOYMENT = "undeployment";
    private static final String DEPLOYMENT = "deployment";

    private String veraUrl;

    private final RestTemplate restTemplate;

    public VeraRestClient(String url) {
        this.restTemplate = new RestTemplate();
        veraUrl = url;
    }

    public void notifyVeraOfDeployment(ApplicationInstance appInstance, Environment environment) {
        String platform = appInstance.getCluster().getNodes().stream().findFirst()
                .map(node -> node.getPlatformType().toString().toLowerCase())
                .orElse("unknown");

        Map<String, String> map = new HashMap<>();
        map.put("application", appInstance.getApplication().getName());
        map.put("deployedBy", "aura (Service User (srvauraautodeploy))");
        map.put("deployTime", appInstance.getDeployDate().toString());
        map.put("deploymentSystem", "aura");
        map.put("environment", environment.getName());
        map.put("platform", platform);
        map.put("version", appInstance.getVersion());

        postToVera(DEPLOYMENT, veraUrl, map, 1);
    }

    public void notifyVeraOfUndeployment(String applicationName, String environmentName, String undeployBy) {
        Map<String, String> map = new HashMap<>();
        map.put("application", applicationName);
        map.put("environment", environmentName);
        map.put("deployedBy", undeployBy);

        postToVera(UNDEPLOYMENT, veraUrl, map, 1);
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
