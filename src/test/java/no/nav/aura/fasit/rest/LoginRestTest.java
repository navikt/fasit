package no.nav.aura.fasit.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import no.nav.aura.envconfig.rest.RestTest;
import org.junit.jupiter.api.Test;

import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

public class LoginRestTest extends RestTest {
    
    private final String SESSIONCOOKIE = "JSESSIONID";
    
    
    @Test
    public void formBasedloginGivesASession() {
        given()
        .when()
            .get("/api/v2/currentuser")
        .then()
            .statusCode(200)
            .body("authenticated", is(false))
            .contentType(ContentType.JSON)
            ;
        
       
        String sessionId = given()
            .formParam("username", "user")
            .formParam("password", "user")
        .when()
            .post("/api/login")
        .then()
//            .log().all()
            .cookie(SESSIONCOOKIE)
            .statusCode(200)
            .extract().sessionId();
        
        given()
            .sessionId(sessionId)
        .when()
            .get("/api/v2/currentuser")
        .then()
            .statusCode(200)
            .body("authenticated", is(true));
    }
    
    @Test
    public void loginWithBadCredentialsGives401() {
        given()
            .formParam("username", "user")
            .formParam("password", "feilpassord")
        .when()
            .post("/api/login")
        .then()
            .statusCode(401);
    }
    
    @Test
    public void logout() {
        String sessionId = given()
                .formParam("username", "user")
                .formParam("password", "user")
            .when()
                .post("/api/login")
            .then()
                .cookie(SESSIONCOOKIE)
                .extract().sessionId();
            
            ExtractableResponse<Response> response = given()
                .sessionId(sessionId)
            .when()
                .post("/api/logout")
            .then()
//                .log().all()
                .statusCode(200)
                .extract();
            
            assertThat(response.cookie(SESSIONCOOKIE), emptyString());
    }
    
    @Test
    public void basicAuthLoginGivesASession() {
        given()
            .auth().preemptive().basic("user", "user")
        .when()
            .get("/api/v2/currentuser")
        .then()
            .statusCode(200)
            .body("authenticated", is(true))
            .cookie(SESSIONCOOKIE);
    }
    
    @Test
    public void normalGetDontCreateASeession() {
        ExtractableResponse<Response> response = 
         given()
        .when()
            .get("/api/v2/currentuser")
        .then()
            .statusCode(200)
            .body("authenticated", is(false))
            .extract();
        
        assertThat(response.cookie(SESSIONCOOKIE), nullValue());
    }
    

}
