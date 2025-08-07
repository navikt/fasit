package no.nav.aura.envconfig.rest;

import io.restassured.parsing.Parser;
import io.restassured.path.xml.XmlPath;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.application.ApplicationGroup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import static io.restassured.RestAssured.expect;
import static io.restassured.path.xml.XmlPath.from;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@TestInstance(Lifecycle.PER_CLASS)
public class ApplicationGroupRestServiceTest extends RestTest {
    private ApplicationGroup applicationGroup;
    private ApplicationGroup emptyApplicationGroup;

    @BeforeAll
    public void setup() throws Exception {
        Application firstApp = repository.store(new Application("myFirstApp"));
        Application secondApp = repository.store(new Application("mySecondApp"));

        applicationGroup = new ApplicationGroup("myApplicationGroup");
        applicationGroup.addApplication(firstApp);
        applicationGroup.addApplication(secondApp);

        emptyApplicationGroup = repository.store(new ApplicationGroup("myEmptyApplicationGroup"));
        repository.store(applicationGroup);
        repository.store(emptyApplicationGroup);
    }
    
    @AfterAll
    @Transactional
    void tearDown() {
        cleanupEnvironments();
        cleanupApplicationGroup();
        cleanupApplications();
	}

    @Test
    public void applicationGroupGetServices_shouldBeCaseInsensitive() throws Exception {
        expect().statusCode(HttpStatus.OK.value()).when().get("/conf/applicationGroups/myApplicationGroup");
        expect().statusCode(HttpStatus.OK.value()).when().get("/conf/applicationGroups/MYaPPLiCAtiOnGrOuP");
    }

    @Test
    public void getApplicationGroups() {
        String xml = expect().defaultParser(Parser.XML).statusCode(HttpStatus.OK.value()).when().get("/conf/applicationGroups/").asString();
        XmlPath path = from(xml);
        assertThat("applicationGroups", path.getInt("collection.applicationGroup.size()"), is(2));
        assertThat(path.<String> getList("collection.applicationGroup.name"), containsInAnyOrder("myApplicationGroup", "myEmptyApplicationGroup"));
        assertThat(path.<String> getList("collection.applicationGroup.application.name"), containsInAnyOrder("myFirstApp", "mySecondApp"));
    }

    @Test
    public void getApplicationGroup() {
        String xml = expect().defaultParser(Parser.XML).statusCode(HttpStatus.OK.value()).when().get("/conf/applicationGroups/myApplicationGroup").asString();
        XmlPath path = from(xml);

        assertThat(path.getString("applicationGroup.name"), is("myApplicationGroup"));
        assertThat(path.getInt("applicationGroup.application.size()"), is(2));
        assertThat(path.<String> getList("applicationGroup.application.name"), containsInAnyOrder("myFirstApp", "mySecondApp"));
    }

    @Test
    public void getEmptyApplicationGroup() {
        String xml = expect().defaultParser(Parser.XML).statusCode(HttpStatus.OK.value()).when().get("/conf/applicationGroups/myEmptyApplicationGroup").asString();
        XmlPath path = from(xml);
        assertThat(path.getList("applicationGroup.applications"), empty());
    }

    @Test
    public void getApplicationGroup_noApplicationGroupFound() {
        expect().statusCode(HttpStatus.NOT_FOUND.value()).when().get("/conf/applicationGroups/nonExistingApplicationGroup");
    }
}
