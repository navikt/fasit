package no.nav.aura.fasit.rest;

import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.rest.RestTest;
import no.nav.aura.fasit.repository.ResourceRepository;
import no.nav.aura.fasit.repository.SecretRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class SecretRestTest extends RestTest {


    private static Resource savedResource;

    @BeforeAll
    @Transactional
    public static void setUp() throws Exception {
        ResourceRepository repo = jetty.getBean(ResourceRepository.class);
        Resource db = new Resource("myDB", ResourceType.DataSource, new Scope(EnvironmentClass.u).domain(Domain.Devillo).envName("myEnv"));
        db.putPropertyAndValidate("url", "jdbc:url");
        db.putPropertyAndValidate("username", "user");
        db.putPropertyAndValidate("oemEndpoint", "test");
        db.putPropertyAndValidate("onsHosts", "test:6200,test1:6200");
        db.putSecretAndValidate("password", "secret");
        savedResource = repo.save(db);
    }


    @Test
    public void findSecretAsAdmin() {
        ResourceRepository repo = jetty.getBean(ResourceRepository.class);
        SecretRepository repo2 = jetty.getBean(SecretRepository.class);
        given()
            .auth().basic("prodadmin", "prodadmin")
            .pathParam("id", savedResource.getID())
                // .pathParam("id", savedResource.getSecrets().get("password").getID())
        .when()
            .get("/api/v2/secrets/{id}" )
        .then()
            .statusCode(200)
            .body(equalTo("secret"));
    }
    
    @Test
    public void getSecretRequiresLogin() {
        given()
            .pathParam("id", savedResource.getID())
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
