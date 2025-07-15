package no.nav.aura.envconfig.rest;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.model.secrets.Secret;
import no.nav.aura.envconfig.spring.User;
import no.nav.aura.fasit.conf.security.RestRoles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.NoResultException;

/**
 * API for å hente ut hemmeligheter(feks passord) fra databasen
 */
@RestController
@RequestMapping(path = "/conf/secrets")
public class SecretRestService {

    private final FasitRepository repo;
    
    private final static Logger log = LoggerFactory.getLogger(SecretRestService.class);

    public SecretRestService(FasitRepository repo) {
        this.repo = repo;
    }

    /**
     * Henter en hemmelighet
     *
     * @param combinedId
     *            id på formatet 'secret-[0-9]+'
     * @return hemmeligheten som tekst
     * @HTTP 401 Ved manglende tilgang
     * @HTTP 404 ved manglende resultat
     */
    @GetMapping(path = "/{id:.+}", produces = "text/plain")
    public ResponseEntity<String> get(
    		@PathVariable("id") String combinedId, 
    		@RequestHeader(name = "showsecret", required = false, defaultValue = "false") boolean showSecret) {
        if (!showSecret) {
            log.info("Missing header param for user {}", User.getCurrentUser().getIdentity());
            // return
            // Response.status(Status.BAD_REQUEST).entity("you are not allowed to view a password from the browser").build();
        }

        if (!combinedId.startsWith("secret-")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find secret for id " + combinedId);
        }
        String[] parts = combinedId.split("-");
        if (parts[1].startsWith("vault/")) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
					.body("This endpoint is not yet implemented for Vault secrets. Please check back later.");
        } else {
            if (!combinedId.matches("secret-[0-9]+")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find secret for id " + combinedId);

            }
            Long id = Long.valueOf(parts[1]);
            String password;
            String type = parts[0];
            if ("secret".equals(type)) {
            	try {
            		Secret secret = repo.getById(Secret.class, id);
            		checkAccess(secret);
            		password = secret.getClearTextString();
            	} catch (NoResultException e) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find secret for id " + combinedId);

				} 
            	
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unpossible " + combinedId);
            }
            return ResponseEntity.ok(password);
        }
    }

    private void checkAccess(Secret secret) {
        if (!RestRoles.hasViewPasswordAccess(secret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, 
                    "No access to secret with id " + secret.getID() + " for user " + User.getCurrentUser().getIdentity());
        }
    }

    public static String createPath(Secret secret) {
        return "/conf/secrets/secret-" + secret.getID();
    }

}
