package no.nav.aura.fasit.rest;

import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.rest.RestTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@TestInstance(Lifecycle.PER_CLASS)
public class SecretRestTest extends RestTest {
    private final static Logger log = LoggerFactory.getLogger(SecretRestTest.class);

    private Resource savedResource;
    
    @BeforeAll
    @Transactional
    public void setUp() throws Exception {
        Resource db = new Resource("myDB", ResourceType.DataSource, new Scope(EnvironmentClass.u).domain(Domain.Devillo).envName("myEnv"));
        db.putPropertyAndValidate("url", "jdbc:url");
        db.putPropertyAndValidate("username", "user");
        db.putPropertyAndValidate("oemEndpoint", "test");
        db.putPropertyAndValidate("onsHosts", "test:6200,test1:6200");
        db.putSecretAndValidate("password", "secret");
        savedResource = repository.store(db);
    }

    @Transactional
    @AfterAll
    public void tearDown() throws Exception {
    	cleanupResources();
	}


    @Test
    public void findSecretAsAdmin() {
        given()
            .auth().preemptive().basic("prodadmin", "prodadmin")
            .pathParam("id", savedResource.getSecrets().get("password").getID())
        .when()
            .get("/api/v2/secrets/{id}" )
        .then()
            .statusCode(200)
            .body(equalTo("secret"));
    }
    
    @Test
    public void getSecretRequiresLogin() {
        given()
            .pathParam("id", savedResource.getSecrets().get("password").getID())
        .when()
           .get("/api/v2/secrets/{id}" )
        .then()
            .statusCode(401);
    }
    
    @Test
    public void getSecretNotFound() {
        given()
            .auth().basic("prodadmin", "prodadmin")
            .pathParam("id", 34533453)
        .when()
           .get("/api/v2/secrets/{id}" )
        .then()
            .statusCode(404);
    }
}
