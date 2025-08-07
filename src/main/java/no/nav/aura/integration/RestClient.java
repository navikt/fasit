package no.nav.aura.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Base64;

public class RestClient {

    private static final Logger log = LoggerFactory.getLogger(RestClient.class);

    private final RestTemplate restTemplate;

    public RestClient(String username, String password) {
        
        this.restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new NoOpResponseErrorHandler());
        
        // Configure basic authentication
        restTemplate.getInterceptors().add((request, body, execution) -> {
            String auth = username + ":" + password;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, authHeader);
            return execution.execute(request, body);
        });
    }

    <T> T get(URI url, Class<T> returnType) {
        ResponseEntity<T> response = restTemplate.getForEntity(url, returnType);

        log.debug("Calling url {}", url);
        checkResponseAndThrowExeption(response, url);
        return response.getBody();
    }

    void checkResponseAndThrowExeption(ResponseEntity<?> response, URI requestUrl) {
        HttpStatus status = response.getStatusCode();
        if (status == HttpStatus.FORBIDDEN) {
            throw new SecurityException("Access forbidden to " + requestUrl);
        }
        if (status == HttpStatus.UNAUTHORIZED) {
            throw new SecurityException("Unauthorized access to " + requestUrl);
        }
        if (status == HttpStatus.NOT_FOUND) {
            throw new IllegalArgumentException("Not found " + requestUrl);
        }
        if (status.is4xxClientError() || status.is5xxServerError()) {
            String entity = response.hasBody() ? response.getBody().toString() : null;
            throw new RuntimeException("Error calling " + requestUrl + " " + entity + ", status: " + status.value());
        }
    }
    
    // This error handler prevents RestTemplate from automatically throwing exceptions
    private static class NoOpResponseErrorHandler extends DefaultResponseErrorHandler {
        @Override
        public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
            return false;
        }
    }
}
