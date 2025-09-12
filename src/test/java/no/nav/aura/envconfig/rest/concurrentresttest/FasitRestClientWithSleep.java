package no.nav.aura.envconfig.rest.concurrentresttest;


import no.nav.aura.envconfig.client.FasitRestClient;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;


public class FasitRestClientWithSleep extends FasitRestClient {

    private final String baseUrl;

    public FasitRestClientWithSleep(String baseUrl, String username, String password) {
        super(baseUrl, username, password);
        this.baseUrl = baseUrl;
    }

    public String sleep(Integer milliseconds) {
        URI uri = UriBuilder.fromUri(baseUrl).path("sleep").queryParam("milliseconds", milliseconds).build();
        try {
            String s = get(uri, String.class);
            return s;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
