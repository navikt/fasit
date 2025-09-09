package no.nav.aura.integration;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;


public class VeraRestClientTest {

    private VeraRestClient client;
    private RestTemplate mockRestTemplate = mock(RestTemplate.class);
    private String veraUrl = "http://localhost:1234";

    @BeforeEach
    public void start() {
        client = new VeraRestClient(veraUrl, mockRestTemplate);
    }

    @Test
    public void undeployOk() throws Exception {
    	when(mockRestTemplate.postForEntity(anyString(), any(), eq(String.class)))
			.thenReturn(ResponseEntity.ok("OK")); // Simulate a successful response

        client.notifyVeraOfUndeployment("myApp", "env", "junit");

        verify(mockRestTemplate).postForEntity(
                eq(veraUrl),
                any(HttpEntity.class),
                eq(String.class)
            );
    }
    
}
