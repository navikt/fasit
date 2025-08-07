package no.nav.aura.fasit.rest;

import no.nav.aura.envconfig.spring.User;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/currentuser")
public class UserRest {
    
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public User currentUser(){
     return User.getCurrentUser();    
    }
}
