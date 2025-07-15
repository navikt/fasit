package no.nav.aura.envconfig.rest;

import com.google.common.collect.Lists;
import io.restassured.parsing.Parser;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.application.ApplicationGroup;
import no.nav.aura.envconfig.model.infrastructure.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;


import static io.restassured.RestAssured.expect;
import static io.restassured.path.xml.XmlPath.from;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(Lifecycle.PER_CLASS)
public class ApplicationUrlTest extends RestTest {
    private ApplicationGroup myAppGroup;

    @BeforeAll
    public void setUpData() throws Exception {
        String appName = "myApp";
        Environment env = new Environment("myTestEnv", EnvironmentClass.u);
        Cluster cluster = new Cluster("cluster", Domain.Devillo);
        env.addCluster(cluster);
        Application app = repository.store(new Application(appName, "myApp-appconfig", "no.nav.myApp"));
        Application app2 = repository.store(new Application("mySecondApp", "myApp-appconfig", "no.nav.myApp"));
        myAppGroup = repository.store(
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
    
    @AfterAll
    @Transactional
    void tearDownData() {
    	cleanupEnvironments();
        cleanupApplicationGroup();
    	cleanupApplications();
	}

    @Test
    public void testFindApplications() {
        expect().
                statusCode(HttpStatus.OK.value())
                .body(containsString("myApp"))
                .when().get("/conf/applications");
    }

    @Test
    public void twoApplicationInApplicationGroup_shouldHaveDifferentPortOffsetReturnedByRest() {
        assertPortOffsetForApplication("myApp", 0);
        assertPortOffsetForApplication("mySecondApp", 1);
    }

    private void assertPortOffsetForApplication(String applicationName, int expectedPortOffset) {
        String xml = expect().defaultParser(Parser.XML).statusCode(HttpStatus.OK.value())
                .when().get(String.format("/conf/applications/%s/", applicationName)).asString();
        assertEquals(expectedPortOffset, from(xml).getInt("application.portOffset"), "PortOffset");
    }

    @Test
    public void testFindApp() {
        expect().statusCode(HttpStatus.OK.value()).when().get("/conf/applications/myApp");
    }

    @Test
    public void testFindNonExistingApp() {
        expect().statusCode(HttpStatus.NOT_FOUND.value()).when().get("/conf/applications/spragleknas");
    }

    @Test
    public void applicationGetServices_shouldBeCaseInsensitive() throws Exception {
        expect().statusCode(HttpStatus.OK.value()).when().get("/conf/applications/MYAPP");
    }

}
