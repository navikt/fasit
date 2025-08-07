package no.nav.aura.envconfig.rest;

import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/** Unngå notfound execption når restapi brukes med browser */
@Path("/conf/favicon.ico")
@Component
public class FaviconRestService {

    /**
     * Dummytjeneste for å unngå notfound når restapi brukes av en browser
     */
    @GET
    public void favico() {
    }
}
