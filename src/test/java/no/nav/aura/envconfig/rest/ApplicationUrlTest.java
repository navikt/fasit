package no.nav.aura.envconfig.rest;

import com.google.common.collect.Lists;
import io.restassured.parsing.Parser;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.application.ApplicationGroup;
import no.nav.aura.envconfig.model.infrastructure.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response.Status;

import static io.restassured.RestAssured.expect;
import static io.restassured.path.xml.XmlPath.from;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ApplicationUrlTest extends RestTest {

    @BeforeAll
    public static void setUpData() throws Exception {
        String appName = "myApp";
        Environment env = new Environment("myTestEnv", EnvironmentClass.u);
        Cluster cluster = new Cluster("cluster", Domain.Devillo);
        env.addCluster(cluster);
        Application app = repository.store(new Application(appName, "myApp-appconfig", "no.nav.myApp"));
        Application app2 = repository.store(new Application("mySecondApp", "myApp-appconfig", "no.nav.myApp"));
        repository.store(
                new ApplicationGroup("myAppGroup", Lists.newArrayList(app, app2)));

        cluster.addApplication(app);
        cluster.addApplication(app2);
        Node node = new Node("myNewHost.devillo.no", "username", "pass");
        env.addNode(cluster, node);
        cluster.addNode(node);
        env.addCluster(cluster);
        env = repository.store(env);
        ApplicationInstance appInstance = env.findApplicationByName(appName);
        appInstance.setVersion("1.2.3");
        repository.store(appInstance);
    }

    @Test
    public void testFindApplications() {
        expect().
                statusCode(OK.getStatusCode())
                .body(containsString("myApp"))
                .when().get("/conf/applications");
    }

    @Test
    public void twoApplicationInApplicationGroup_shouldHaveDifferentPortOffsetReturnedByRest() {
        assertPortOffsetForApplication("myApp", 0);
        assertPortOffsetForApplication("mySecondApp", 1);
    }

    private void assertPortOffsetForApplication(String applicationName, int expectedPortOffset) {
        String xml = expect().defaultParser(Parser.XML).statusCode(Status.OK.getStatusCode())
                .when().get(String.format("/conf/applications/%s/", applicationName)).asString();
        assertEquals(expectedPortOffset, from(xml).getInt("application.portOffset"), "PortOffset");
    }

    @Test
    public void testFindApp() {
        expect().statusCode(OK.getStatusCode()).when().get("/conf/applications/myApp");
    }

    @Test
    public void testFindNonExistingApp() {
        expect().statusCode(NOT_FOUND.getStatusCode()).when().get("/conf/applications/spragleknas");
    }

    @Test
    public void applicationGetServices_shouldBeCaseInsensitive() throws Exception {
        expect().statusCode(OK.getStatusCode()).when().get("/conf/applications/MYAPP");
    }

}
