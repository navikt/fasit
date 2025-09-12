package no.nav.aura.fasit.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import no.nav.aura.envconfig.spring.User;

@Component
@Path("api/v2/currentuser")
public class UserRest {
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public User currentUser(){
     return User.getCurrentUser();    
    }
    
    

}

