package no.nav.aura.fasit.rest;

import io.restassured.http.ContentType;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.rest.RestTest;
import no.nav.aura.fasit.repository.EnvironmentRepository;
import no.nav.aura.fasit.rest.model.EnvironmentPayload;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class EnvironmentRestTest extends RestTest {

    @BeforeAll
    @Transactional
    public static void setUp() throws Exception {
        EnvironmentRepository environmentRepo = jetty.getBean(EnvironmentRepository.class);
        environmentRepo.save(new Environment("u1", EnvironmentClass.u));
        environmentRepo.save(new Environment("deleteme", EnvironmentClass.u));
        environmentRepo.save(new Environment("changeme", EnvironmentClass.u));
        Environment environment = new Environment("q1", EnvironmentClass.q);

        environmentRepo.save(environment);
        environmentRepo.save(new Environment("t1", EnvironmentClass.t));
        environmentRepo.save(new Environment("t2", EnvironmentClass.t));
    }

    @Test
    public void findAllEnvironments() {
        given()
                .when()
                .get("/api/v2/environments")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", hasItems("u1", "q1", "t2", "t1"));
    }

    @Test
    public void findByEnvironmentClass() {
        given()
                .queryParam("environmentclass", "q")
                .when()
                .get("/api/v2/environments")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(1))
                .body("name", hasItems("q1"));
    }

    @Test
    public void getByName() {
        given()
                .when()
                .get("/api/v2/environments/t1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", equalTo("t1"))
                .body("environmentclass", equalTo("t"))
                .body("links.clusters", containsString("t1/clusters"));
    }

    @Test
    public void notFound() {
        given()
                .when()
                .get("/api/v2/environments/unknown")
                .then()
                .statusCode(404)
                .body(containsString("unknown does not exist"));

    }

    @Test
    public void createNewEnvironment() {
        EnvironmentPayload environment = new EnvironmentPayload("newEnv", EnvironmentClass.u);
        given()
                .auth().basic("user", "user")
                .body(toJson(environment))
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/environments")
                .then()
                .statusCode(201)
                .header("location", containsString("environments/newEnv"));
    }

    @Test
    public void createDuplicateEnvironment() {
        EnvironmentPayload environment = new EnvironmentPayload("u1", EnvironmentClass.u);
        given()
                .auth().basic("user", "user")
                .body(toJson(environment))
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/environments")
                .then()
                .statusCode(400)
                .body(containsString("u1 already exists"));
    }

    @Test
    public void createEnvironmentNoAccess() {
        EnvironmentPayload environment = new EnvironmentPayload("noaccess", EnvironmentClass.q);
        given()
                .auth().basic("user", "user")
                .body(toJson(environment))
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/environments")
                .then()
                .statusCode(403);
    }

    @Test
    public void renameEnvironment() {
        EnvironmentPayload environment = new EnvironmentPayload("newname", EnvironmentClass.u);
        given()
                .auth().basic("user", "user")
                .body(toJson(environment))
                .contentType(ContentType.JSON)
                .when()
                .put("/api/v2/environments/changeme")
                .then()
                .statusCode(200)
                .body("name", equalTo("newname"))
                .body("environmentclass", equalTo("u"));
    }

    @Test
    public void changeStatus() {
        EnvironmentPayload environment = new EnvironmentPayload("u1", EnvironmentClass.u);
        environment.lifecycle.status = LifeCycleStatus.STOPPED;
        given()
                .auth().basic("user", "user")
                .body(toJson(environment))
                .contentType(ContentType.JSON)
                .when()
                .put("/api/v2/environments/u1")
                .then()
                .statusCode(200)
                .body("lifecycle.status", equalTo("stopped"));
    }

    @Test
    public void changeEnvironmentClassShouldBeIllegal() {
        EnvironmentPayload environment = new EnvironmentPayload("u1", EnvironmentClass.t);
        given()
                .auth().basic("user", "user")
                .body(toJson(environment))
                .contentType(ContentType.JSON)
                .when()
                .put("/api/v2/environments/u1")
                .then()
                .statusCode(400)
                .body(containsString("not possible to change environmentclass on an environment"));
    }

    @Test
    public void deleteEnvironment() {
        given()
                .auth().basic("user", "user")
                .contentType(ContentType.JSON)
                .when()
                .delete("/api/v2/environments/deleteme")
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/api/v2/environments/deleteme")
                .then()
                .statusCode(404);
    }

}
