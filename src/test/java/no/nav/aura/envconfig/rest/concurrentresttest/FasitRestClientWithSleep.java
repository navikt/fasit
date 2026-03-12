package no.nav.aura.envconfig.rest.concurrentresttest;


import java.net.URI;

import org.springframework.web.util.UriComponentsBuilder;

import no.nav.aura.envconfig.client.FasitRestClient;


public class FasitRestClientWithSleep extends FasitRestClient {

    private final URI baseUrl;

    public FasitRestClientWithSleep(String baseUrl, String username, String password) {
        super(baseUrl, username, password);
        this.baseUrl = URI.create(baseUrl);
    }

    public String sleep(Integer milliseconds) {
    	URI uri = UriComponentsBuilder.fromUri(baseUrl)
				.path("/sleep")
				.queryParam("milliseconds", milliseconds)
				.build().toUri();
        try {
            return get(uri, String.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
