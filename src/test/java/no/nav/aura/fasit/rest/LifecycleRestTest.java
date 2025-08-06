package no.nav.aura.fasit.rest;

import io.restassured.http.ContentType;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.rest.RestTest;
import no.nav.aura.fasit.repository.ApplicationRepository;
import no.nav.aura.fasit.rest.model.LifecyclePayload;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import jakarta.inject.Inject;

import static io.restassured.RestAssured.given;

public class LifecycleRestTest extends RestTest {

    private static Application alertedApplication;
    
    @Inject
    private ApplicationRepository applicationRepository;

    @BeforeAll
    @Transactional
    public void setUp() throws Exception {
        alertedApplication = new Application("tsys");
        alertedApplication.setLifeCycleStatus(LifeCycleStatus.ALERTED);
        applicationRepository.save(alertedApplication);
    }
    
    @AfterAll
    @Transactional
    public void tearDown() {
		cleanupApplications();
	}

    @Test
    public void errorIsThrownWhenUnknownEntityNameInPath() {
        final String unknownEntityName = "thisIsNotAnentityWeKnowAnythingAbout";
        given()
                .body("{\"status\": \"running\"}")
                .auth().basic("user", "user")
                .contentType(ContentType.JSON)
                .when()
                .put("/api/v1/lifecycle/" + unknownEntityName + "/" + alertedApplication.getID()).then().statusCode(400);
    }

    @Test
    public void unknownEntityIdGivesNotFoundError() {
        given()
        		.body("{\"status\": \"running\"}")
                .auth().basic("user", "user")
                .contentType(ContentType.JSON)
                .when()
                .put("/api/v1/lifecycle/application/" + 696969).then().statusCode(404);
    }
    
    @Test
    public void existingApplicationWithCorrectLifecycle() {
        given()
        		.body("{\"status\": \"running\"}")
                .auth().basic("user", "user")
                .log().all()
                .contentType(ContentType.JSON)
                .when()
                .put("/api/v1/lifecycle/application/" + alertedApplication.getID()).then().statusCode(200);
    }
}
