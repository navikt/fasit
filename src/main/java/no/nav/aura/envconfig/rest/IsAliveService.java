package no.nav.aura.envconfig.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

@Path("/conf/isalive")
@Component
public class IsAliveService {
    @GET
    public Response isAlive() {
        return Response.ok().build();
    }
}
