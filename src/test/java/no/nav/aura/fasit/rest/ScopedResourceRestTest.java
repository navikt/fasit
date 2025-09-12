package no.nav.aura.fasit.rest;

import io.restassured.http.ContentType;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.rest.RestTest;
import no.nav.aura.fasit.repository.ApplicationRepository;
import no.nav.aura.fasit.repository.EnvironmentRepository;
import no.nav.aura.fasit.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

public class ScopedResourceRestTest extends RestTest {

    private static ResourceRepository resourceRepo;

    @BeforeAll
    @Transactional
    public static void setUp() throws Exception {
        resourceRepo = jetty.getBean(ResourceRepository.class);
        EnvironmentRepository environmentRepo = jetty.getBean(EnvironmentRepository.class);
        ApplicationRepository applicationRepository = jetty.getBean(ApplicationRepository.class);

        Application app = applicationRepository.save(new Application("app"));
        applicationRepository.save(new Application("otherapp"));
        Environment env = environmentRepo.save(new Environment("env", EnvironmentClass.t));
        environmentRepo.save(new Environment("otherenv", EnvironmentClass.t));

        Resource resourceEnvClass = new Resource("somealias", ResourceType.BaseUrl, new Scope(EnvironmentClass.t));
        resourceEnvClass.putProperty("url", "envclass");
        resourceRepo.save(resourceEnvClass);

        Resource resourceEnvClassDomain = new Resource("somealias", ResourceType.BaseUrl, new Scope(EnvironmentClass.t).domain(Domain.TestLocal));
        resourceEnvClassDomain.putProperty("url", "envclass.domain");
        resourceRepo.save(resourceEnvClassDomain);

        Resource resourceEnvClassDomainEnv = new Resource("somealias", ResourceType.BaseUrl, new Scope(EnvironmentClass.t).domain(Domain.TestLocal).environment(env));
        resourceEnvClassDomainEnv.putProperty("url", "envclass.domain.env");
        resourceRepo.save(resourceEnvClassDomainEnv);

        Resource resourceEnvClassDomainEnvApp = new Resource("somealias", ResourceType.BaseUrl, new Scope(EnvironmentClass.t).domain(Domain.TestLocal).environment(env).application(app));
        resourceEnvClassDomainEnvApp.putProperty("url", "envclass.domain.env.app");
        resourceRepo.save(resourceEnvClassDomainEnvApp);

        Resource duplicateResource1 = new Resource("duplicate", ResourceType.BaseUrl, new Scope(EnvironmentClass.t));
        duplicateResource1.putProperty("url", "duplicate");
        resourceRepo.save(duplicateResource1);

        Resource duplicateResource2 = new Resource("duplicate", ResourceType.BaseUrl, new Scope(EnvironmentClass.t));
        duplicateResource2.putProperty("url", "duplicate");
        resourceRepo.save(duplicateResource2);
    }

    @Test
    public void missingRequiredParamsYieldsBadRequest() {
        given()
                .when()
                .get("/api/v2/scopedresource")
                .then()
                .statusCode(400);
    }

    @Test
    public void invalidApplicationYieldsBadRequest() {
        given()
                .when()
                .get("/api/v2/scopedresource?alias=somealias&application=tull&environment=env&type=baseurl&zone=fss")
                .then()
                .statusCode(400);
    }

    @Test
    public void invalidEnvironmentYieldsBadRequest() {
        given()
                .when()
                .get("/api/v2/scopedresource?alias=somealias&application=app&environment=tull&type=baseurl&zone=fss")
                .then()
                .statusCode(400);
    }

    @Test
    public void noMatchesYieldsNotFound() {
        given()
                .when()
                .get("/api/v2/scopedresource?alias=tull&application=app&environment=env&type=baseurl&zone=fss")
                .then()
                .statusCode(404);
    }

    @Test
    public void findsBestMatchForEnvClassDomainEnvApp() {
        given()
                .when()
                .get("/api/v2/scopedresource?alias=somealias&application=app&environment=env&type=baseurl&zone=fss")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("properties.url", equalTo("envclass.domain.env.app"));
    }

    @Test
    public void findsBestMatchForEnvClassDomainEnv() {
        given()
                .when()
                .get("/api/v2/scopedresource?alias=somealias&application=otherapp&environment=env&type=baseurl&zone=fss")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("properties.url", equalTo("envclass.domain.env"));
    }

    @Test
    public void findsBestMatchForEnvClassDomainApp() {
        given()
                .when()
                .get("/api/v2/scopedresource?alias=somealias&application=app&environment=otherenv&type=baseurl&zone=fss")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("properties.url", equalTo("envclass.domain"));
    }

    @Test
    public void findsBestMatchForEnvClassDomain() {
        given()
                .when()
                .get("/api/v2/scopedresource?alias=somealias&application=otherapp&environment=otherenv&type=baseurl&zone=fss")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("properties.url", equalTo("envclass.domain"));
    }

    @Test
    public void findsBestMatchForEnvClass() {
        given()
                .when()
                .get("/api/v2/scopedresource?alias=somealias&application=otherapp&environment=otherenv&type=baseurl&zone=sbs")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("properties.url", equalTo("envclass"));
    }

    @Test
    public void yieldsBadRequestWhenUnableToDecide() {
        given()
                .when()
                .get("/api/v2/scopedresource?alias=duplicate&application=app&environment=env&type=baseurl&zone=fss")
                .then()
                .statusCode(400)
                .body(startsWith("Unable to decide"));
    }

}
