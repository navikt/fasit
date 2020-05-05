package no.nav.aura.integration;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp;
import static com.xebialabs.restito.semantics.Action.ok;
import static com.xebialabs.restito.semantics.Condition.post;
import static com.xebialabs.restito.semantics.Condition.withPostBodyContaining;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.xebialabs.restito.server.StubServer;


public class VeraRestClientTest {

    private StubServer server;
    private VeraRestClient client;

    @BeforeEach
    public void start() {
        server = new StubServer().run();
        String veraUrl = "http://localhost:" + server.getPort();
        client = new VeraRestClient(veraUrl);
    }

    @AfterEach
    public void stop() {
        server.stop();
    }

    
    @Test
    public void undeployOk() throws Exception {
        whenHttp(server)
                .match(post("/"))
                .then(ok());

        client.notifyVeraOfUndeployment("myApp", "env", "junit");

        verifyHttp(server)
                .once(post("/"), 
                        withPostBodyContaining("myApp"));

    }
    
}
