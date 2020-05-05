package no.nav.aura.integration;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.ok;
import static com.xebialabs.restito.semantics.Action.status;
import static com.xebialabs.restito.semantics.Action.stringContent;
import static com.xebialabs.restito.semantics.Condition.basicAuth;
import static com.xebialabs.restito.semantics.Condition.post;
import static com.xebialabs.restito.semantics.Condition.withPostBodyContaining;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import javax.ws.rs.WebApplicationException;

import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.xebialabs.restito.server.StubServer;

public class BastaRestClientTest {

    private StubServer server;
    private BastaRestClient client;

    @BeforeEach
    public void start() {
        server = new StubServer().run();
        String url = "http://localhost:" + server.getPort();
        client = new BastaRestClient(url, url, "user", "secret");
    }

    @AfterEach
    public void stop() {
        server.stop();
    }

    @Test
    public void stopHost() throws Exception {
        whenHttp(server)
                .match(basicAuth("user", "secret"), post("/api/orders/vm/stop"))
                .then(ok(), stringContent("{\"orderId\":5}"), contentType("application/json"));
        URI result = client.stop("host1");
        assertThat(result.toString(), endsWith("5"));
        verifyHttp(server)
        .once(post("/api/orders/vm/stop"),
                withPostBodyContaining("host1"));
    }
    
    @Test
    public void stopHostFailing() throws Exception {
        Assertions.assertThrows(WebApplicationException.class, () -> {
            whenHttp(server)
                    .match(post("/api/orders/vm/stop"))
                    .then(status(HttpStatus.BAD_REQUEST_400), stringContent("you fucked up"), contentType("text/plain"));
            client.stop("host1");
            verifyHttp(server)
                    .once(post("/api/orders/vm/stop"),
                            withPostBodyContaining("host1"));
        });
    }

    @Test
    public void deleteHost() throws Exception {
        whenHttp(server)
                .match(basicAuth("user", "secret"), post("/api/orders/vm/remove"))
                .then(ok(), stringContent("{\"orderId\":7}"), contentType("application/json"));
        URI result = client.decommision("host2");
        assertThat(result.toString(), endsWith("7"));
        verifyHttp(server)
        .once(post("/api/orders/vm/remove"),
                withPostBodyContaining("host2"));
    }

    @Test
    public void stopResource() throws Exception {
        whenHttp(server)
                .match(basicAuth("user", "secret"), post("/api/orders/serviceuser/stop"))
                .then(ok(), stringContent("{\"orderId\":13}"), contentType("application/json"));
        boolean result = client.stopCredential("t", "sbs", "myAlias");
        assertTrue(result);
        verifyHttp(server)
                .once(post("/api/orders/serviceuser/stop"),
                        withPostBodyContaining("myAlias"));
    }
    
    @Test
    public void stopResourceFailing() throws Exception {
        whenHttp(server)
                .match( post("/api/orders/serviceuser/stop"))
                .then(status(HttpStatus.BAD_REQUEST_400), stringContent("you fucked up"), contentType("text/plain"));
        boolean result = client.stopCredential("t", "sbs", "myAlias");
        assertFalse(result);
        verifyHttp(server)
                .once(post("/api/orders/serviceuser/stop"),
                        withPostBodyContaining("myAlias"));
    }
    
    @Test
    public void deleteResource() throws Exception {
        whenHttp(server)
                .match(basicAuth("user", "secret"), post("/api/orders/serviceuser/remove"))
                .then(ok(), stringContent("{\"orderId\":13}"), contentType("application/json"));
        boolean result = client.deleteCredential("t", "sbs", "myAlias");
        assertTrue(result);
        verifyHttp(server)
                .once(post("/api/orders/serviceuser/remove"),
                        withPostBodyContaining("myAlias"));
    }


}
