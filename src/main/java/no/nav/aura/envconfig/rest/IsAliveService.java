package no.nav.aura.envconfig.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(path = "/conf/isalive")
public class IsAliveService {
    @GetMapping
    public ResponseEntity<?> isAlive() {
        return ResponseEntity.ok().build();
    }
}
