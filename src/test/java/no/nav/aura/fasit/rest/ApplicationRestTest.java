package no.nav.aura.fasit.rest;

import io.restassured.http.ContentType;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.rest.RestTest;
import no.nav.aura.fasit.repository.ApplicationRepository;
import no.nav.aura.fasit.repository.EnvironmentRepository;
import no.nav.aura.fasit.rest.model.ApplicationPayload;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.*;

public class ApplicationRestTest extends RestTest {


    @BeforeAll
    @Transactional
    public static void setUp() throws Exception {
        ApplicationRepository applicationRepository = jetty.getBean(ApplicationRepository.class);
       applicationRepository.save(new Application("tsys"));
       applicationRepository.save(new Application("gosys"));
       applicationRepository.save(new Application("deleteme"));
       Application fasit = applicationRepository.save(new Application("fasit", "artifact", "group"));
       
       // For å sjekke portkonflikt       
       Application app1 = applicationRepository.save(new Application("app1"));
       app1.setPortOffset(1);
       EnvironmentRepository environmentRepo = jetty.getBean(EnvironmentRepository.class);
       Environment u1 = environmentRepo.save(new Environment("u1", EnvironmentClass.u));
       Cluster cluster1 = u1.addCluster(new Cluster( "cluster1", Domain.Devillo));
       ApplicationInstance fasitAppInst = cluster1.addApplication(fasit);
       fasitAppInst.setVersion("1.0");
       cluster1.addApplication(app1);
       environmentRepo.save(u1);
    }


    @Test
    public void findApplicationAll() {
        given()
                .when()
                .get("/api/v2/applications")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", hasItems("tsys", "gosys", "fasit"));
    }

    @Test
    public void findApplicationWithQueryParams() {
        given()
            .queryParam("name", "fasit")
        .when()
            .get("/api/v2/applications")
        .then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("name",hasItems("fasit"));
    }
    
    @Test
    public void getApplicationByName() {
        given()
        .when()
            .get("/api/v2/applications/fasit")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("name",equalTo("fasit"))
            .body("artifactid",equalTo("artifact"))
            .body("groupid",equalTo("group"))
            .body("portoffset",equalTo(0));
    }
    
    @Test
    public void getApplicationByNameNotExisting() {
        given()
        .when()
            .get("/api/v2/applications/unknown")
        .then()
            .statusCode(404);
    }
    

    @Test
    public void createApplication() {
        ApplicationPayload app =new ApplicationPayload("newapp");
        given()
            .auth().basic("superuser", "superuser")
            .body(toJson(app))
            .contentType(ContentType.JSON)
        .when()
            .post("/api/v2/applications")
        .then()
            .statusCode(201)
            .header("location", containsString("applications/newapp"));
    }
    
    @Test
    public void createApplicationDuplicate() {
        ApplicationPayload app =new ApplicationPayload("fasit");
        given()
            .auth().basic("operation", "operation")
            .body(toJson(app))
            .contentType(ContentType.JSON)
        .when()
            .post("/api/v2/applications")
        .then()
            .statusCode(400)
            .body(containsString("allready exists"));
    }
    
    
    @Test
    public void updateApplication() {
        ApplicationPayload app =new ApplicationPayload("gosys");
        app.artifactId="newartifact";
        app.groupId="newgroup";
        app.portOffset=69;
        given()
            .auth().basic("operation", "operation")
            .body(toJson(app))
            .contentType(ContentType.JSON)
        .when()
            .put("/api/v2/applications/gosys")
        .then()
            .statusCode(200)
            .body("artifactid",equalTo("newartifact"))
            .body("groupid",equalTo("newgroup"))
            .body("portoffset",equalTo(69));
    }
    
    @Test
    public void changeNameOnApplicationIsNotAllowd() {
        ApplicationPayload app =new ApplicationPayload("fasit2");
        given()
            .auth().basic("operation", "operation")
            .body(toJson(app))
            .contentType(ContentType.JSON)
        .when()
            .put("/api/v2/applications/fasit")
        .then()
            .statusCode(400)
            .body(containsString("to change name of an application"));
    }
    
    @Test
    public void changePortOnAppWithPortConlict() {
        ApplicationPayload app =new ApplicationPayload("fasit");
        app.portOffset=1;
        given()
            .auth().basic("operation", "operation")
            .body(toJson(app))
            .contentType(ContentType.JSON)
        .when()
            .put("/api/v2/applications/fasit")
        .then()
            .statusCode(400)
            .body(containsString("Conflicting portoffset with application app1") );
    }
    
    @Test
    public void deleteApplication() {
        given()
            .auth().basic("operation", "operation")
            .contentType(ContentType.JSON)
        .when()
            .delete("/api/v2/applications/deleteme")
        .then()
            .statusCode(204);
        
        when()
        .get("/api/v2/applications/deleteme")
        .then()
            .statusCode(404);
    }
    
    @Test
    public void deleteApplicationWithApplicationInstancesIsNotAllowed() {
        given()
            .auth().basic("operation", "operation")
            .contentType(ContentType.JSON)
        .when()
            .delete("/api/v2/applications/fasit")
        .then()
            .statusCode(400)
            .body(containsString("deployed to 1 environment(s)"));
        
        
    }
       
}
