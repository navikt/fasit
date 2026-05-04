package no.nav.aura.fasit.rest;

import no.nav.aura.envconfig.rest.RestTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.*;

@TestInstance(Lifecycle.PER_CLASS)
public class SpringDocApiTest extends RestTest {

    @Test
    void shouldServeOpenApiSpec() {
        when()
            .get("/api/services.json")
        .then()
        	.log().ifValidationFails()
            .statusCode(200)
            .contentType("application/json")
            .body("openapi", notNullValue())
            .body("paths", notNullValue());
    }
}
