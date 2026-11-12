package no.nav.aura.fasit.rest;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import no.nav.aura.envconfig.spring.User;

@RestController
@RequestMapping("/api/v2/currentuser")
public class UserRest {
    
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public User currentUser(){
     return User.getCurrentUser();    
    }
}
