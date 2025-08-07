package no.nav.aura.envconfig.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.secrets.Secret;
import no.nav.aura.envconfig.spring.User;
import no.nav.aura.fasit.conf.security.RestRoles;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * API for å hente ut hemmeligheter(feks passord) fra databasen
 */
@Component
@Path("/conf/secrets")
public class SecretRestService {

    private final FasitRepository repo;
    
    private final static Logger log = LoggerFactory.getLogger(SecretRestService.class);

    @Autowired
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
    @GET
    @Path("/{id : .+}")
    public Response get(@PathParam("id") String combinedId, @HeaderParam("showsecret") boolean showSecret) {
        if (!showSecret) {
            log.info("Missing header param for user {}", User.getCurrentUser().getIdentity());
            // return
            // Response.status(Status.BAD_REQUEST).entity("you are not allowed to view a password from the browser").build();
        }

        if (!combinedId.startsWith("secret-")) {
            throw new NotFoundException("Unable to find secret for id " + combinedId);
        }
        String[] parts = combinedId.split("-");
        if (parts[1].startsWith("vault/")) {
            return Response.ok("TODO: this endpoint will eventually support Vault secrets.", MediaType.TEXT_PLAIN_TYPE).build();
        } else {
            if (!combinedId.matches("secret-[0-9]+")) {
                throw new NotFoundException("Unable to find secret for id " + combinedId);
            }
            Long id = Long.valueOf(parts[1]);
            String password;
            String type = parts[0];
            if ("secret".equals(type)) {
                Secret secret = repo.getById(Secret.class, id);
                checkAccess(secret);
                password = secret.getClearTextString();
            } else {
                throw new BadRequestException("Unpossible " + combinedId);
            }
            return Response.ok(password, MediaType.TEXT_PLAIN_TYPE).build();
        }
    }

    private void checkAccess(Secret secret) {
        if (!RestRoles.hasViewPasswordAccess(secret)) {
            throw new NotAuthorizedException("No access to secret with id " + secret.getID() + " for user " + User.getCurrentUser().getIdentity());
        }
    }

    public static String createPath(Secret secret) {
        return "/conf/secrets/secret-" + secret.getID();
    }

}
