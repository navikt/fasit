package no.nav.aura.envconfig.rest;

import io.restassured.parsing.Parser;
import io.restassured.path.xml.XmlPath;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response.Status;

import static io.restassured.RestAssured.expect;
import static io.restassured.path.xml.XmlPath.from;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

public class EnvironmentsUrlTest extends RestTest {

    @BeforeAll
    public static void setup(){
        repository.store(new Environment("test", EnvironmentClass.u));
        repository.store(new Environment("ChEvY_cAsE", EnvironmentClass.u));
    }

    @Test
    public void getEnvironments() {
        String xml = expect().defaultParser(Parser.XML).statusCode(Status.OK.getStatusCode())
                .when().get("/conf/environments").asString();
        XmlPath path = from(xml);
        assertEquals(2, path.getInt("collection.environment.size()"), "environments");
    }

    @Test
    public void getEnvironment() {
        String xml = expect().defaultParser(Parser.XML).statusCode(Status.OK.getStatusCode())
                .when().get("/conf/environments/test").asString();
        XmlPath path = from(xml);
        assertEquals("test", path.getString("environment.name"), "environment name");
        assertEquals("u", path.getString("environment.envClass"), "environment class");
        assertThat("applicationref", path.getString("environment.applicationsRef"), Matchers.endsWith("/environments/test/applications"));
    }

    @Test
    public void gettingNonExistingEnvironment_shouldGiveNotFound() throws Exception {
        expect().statusCode(Status.NOT_FOUND.getStatusCode()).when().get("/conf/environments/imnothere");
    }

    @Test
    public void applicationGetServices_shouldBeCaseInsensitive() throws Exception {
        expect().statusCode(Status.OK.getStatusCode()).when().get("/conf/environments/chevy_case").asString();
        expect().statusCode(Status.OK.getStatusCode()).when().get("/conf/environments/?envClass=U").asString();
    }


}
