package no.nav.aura.envconfig.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/** Unngå notfound execption når restapi brukes med browser */
@RestController
@RequestMapping(path = "/favicon.ico")
public class FaviconRestService {

    /**
     * Dummytjeneste for å unngå notfound når restapi brukes av en browser
     */
    @GetMapping
    public ResponseEntity<Object> favico() {
    	return ResponseEntity.noContent().build();
    }
}
