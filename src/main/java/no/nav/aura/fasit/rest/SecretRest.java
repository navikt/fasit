package no.nav.aura.fasit.rest;

import java.net.URI;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import no.nav.aura.envconfig.model.secrets.Secret;
import no.nav.aura.envconfig.spring.AccessException;
import no.nav.aura.envconfig.spring.User;
import no.nav.aura.fasit.repository.SecretRepository;
import no.nav.aura.fasit.rest.security.RestRoles;

@RestController
@RequestMapping(path = "/api/v2/secrets")
public class SecretRest {

    @Inject
    private SecretRepository repo;

    public static URI secretUri(URI baseUri, long id) {
        return UriComponentsBuilder.fromUri(baseUri)
                .path("/api/v2/secrets/{id}")
                .buildAndExpand(id)
                .toUri();
    }

    @GetMapping(path = "{id:.+}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getSecret(@PathVariable(name = "id") String idString) {
        if (idString.startsWith("vault/")) {
            return "TODO: this endpoint will eventually support Vault secrets.";
        } else {
            try {
                Long id = Long.valueOf(idString);
                Secret secret = repo.findById(id).orElseThrow(() ->
                	new ResponseStatusException(HttpStatus.NOT_FOUND, "Secret with id " + id + " not found"));
                checkAccess(secret);
                return secret.getClearTextString();
            } catch (NumberFormatException ex) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not get secret with id " + idString, ex);
            }
        }
    }


    private void checkAccess(Secret secret) {
        if (!RestRoles.hasViewPasswordAccess(secret)) {
            throw new AccessException("No access to secret with id " + secret.getID() + " for user " + User.getCurrentUser().getIdentity());
        }
    }
}
