package no.nav.aura.envconfig.rest;

import io.restassured.parsing.Parser;
import io.restassured.path.xml.XmlPath;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.application.ApplicationGroup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response.Status;

import static io.restassured.RestAssured.expect;
import static io.restassured.path.xml.XmlPath.from;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class ApplicationGroupRestServiceTest extends RestTest {

    @BeforeAll
    public static void setup() throws Exception {
        Application firstApp = repository.store(new Application("myFirstApp"));
        Application secondApp = repository.store(new Application("mySecondApp"));

        ApplicationGroup applicationGroup = new ApplicationGroup("myApplicationGroup");
        applicationGroup.addApplication(firstApp);
        applicationGroup.addApplication(secondApp);

        ApplicationGroup emptyApplicationGroup = repository.store(new ApplicationGroup("myEmptyApplicationGroup"));
        repository.store(applicationGroup);
        repository.store(emptyApplicationGroup);
    }

    @Test
    public void applicationGroupGetServices_shouldBeCaseInsensitive() throws Exception {
        expect().statusCode(Status.OK.getStatusCode()).when().get("/conf/applicationGroups/myApplicationGroup");
        expect().statusCode(Status.OK.getStatusCode()).when().get("/conf/applicationGroups/MYaPPLiCAtiOnGrOuP");
    }

    @Test
    public void getApplicationGroups() {
        String xml = expect().defaultParser(Parser.XML).statusCode(Status.OK.getStatusCode()).when().get("/conf/applicationGroups/").asString();
        XmlPath path = from(xml);
        assertThat("applicationGroups", path.getInt("collection.applicationGroup.size()"), is(2));
        assertThat(path.<String> getList("collection.applicationGroup.name"), containsInAnyOrder("myApplicationGroup", "myEmptyApplicationGroup"));
        assertThat(path.<String> getList("collection.applicationGroup.application.name"), containsInAnyOrder("myFirstApp", "mySecondApp"));
    }

    @Test
    public void getApplicationGroup() {
        String xml = expect().defaultParser(Parser.XML).statusCode(Status.OK.getStatusCode()).when().get("/conf/applicationGroups/myApplicationGroup").asString();
        XmlPath path = from(xml);

        assertThat(path.getString("applicationGroup.name"), is("myApplicationGroup"));
        assertThat(path.getInt("applicationGroup.application.size()"), is(2));
        assertThat(path.<String> getList("applicationGroup.application.name"), containsInAnyOrder("myFirstApp", "mySecondApp"));
    }

    @Test
    public void getEmptyApplicationGroup() {
        String xml = expect().defaultParser(Parser.XML).statusCode(Status.OK.getStatusCode()).when().get("/conf/applicationGroups/myEmptyApplicationGroup").asString();
        XmlPath path = from(xml);
        assertThat(path.getList("applicationGroup.applications"), empty());
    }

    @Test
    public void getApplicationGroup_noApplicationGroupFound() {
        expect().statusCode(Status.NOT_FOUND.getStatusCode()).when().get("/conf/applicationGroups/nonExistingApplicationGroup");
    }
}
