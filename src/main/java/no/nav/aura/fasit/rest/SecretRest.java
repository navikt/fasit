package no.nav.aura.fasit.rest;

import java.net.URI;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;

import no.nav.aura.envconfig.model.secrets.Secret;
import no.nav.aura.envconfig.spring.User;
import no.nav.aura.fasit.repository.SecretRepository;
import no.nav.aura.fasit.rest.security.RestRoles;

@Component
@Path("api/v2/secrets")
public class SecretRest {

    @Inject
    private SecretRepository repo;

    public static URI secretUri(URI baseUri, long id) {
        return UriBuilder.fromUri(baseUri).path(SecretRest.class).path(SecretRest.class, "getSecret" ).build(id);
    }

    @GET
    @Path("{id : .+}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSecret(@PathParam("id") String idString) {
        if (idString.startsWith("vault/")) {
            return "TODO: this endpoint will eventually support Vault secrets.";
        } else {
            try {
                Long id = Long.valueOf(idString);
                Secret secret = repo.findById(id).orElseThrow(() -> new NotFoundException("Secret with id " + id + " not found"));
                checkAccess(secret);
                return secret.getClearTextString();
            } catch (NumberFormatException ex) {
                throw new NotFoundException("Could not get secret with id " + idString);
            }
        }
    }


    private void checkAccess(Secret secret) {
        if (!RestRoles.hasViewPasswordAccess(secret)) {
            throw new RuntimeException("No access to secret with id " + secret.getID() + " for user " + User.getCurrentUser().getIdentity());
        }
    }
}
