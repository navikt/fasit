package no.nav.aura.envconfig.rest;

import no.nav.aura.envconfig.spring.AccessException;
import no.nav.aura.envconfig.spring.User;
import org.jboss.resteasy.spi.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.NoResultException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;

@Provider
public class SimpleExceptionMapper implements ExceptionMapper<RuntimeException> {

    private static final Logger logger = LoggerFactory.getLogger(SimpleExceptionMapper.class);

    @Override
    public Response toResponse(RuntimeException e) {
        int status;
        String message;
        if (e instanceof Failure) {
            // Errors from RestEasy
            Failure f = (Failure) e;
            status = f.getErrorCode();
            message = f.getMessage();
            logger.warn("Rest returned code: {} reason: {}", status, message);
        } else if (e instanceof NoResultException || e instanceof NotFoundException) {
            status = Response.Status.NOT_FOUND.getStatusCode();
            message = e.getMessage();
            logger.error("No result found in database", e);
        } else if (e instanceof AccessException || e instanceof NotAuthorizedException) {
            status = Response.Status.UNAUTHORIZED.getStatusCode();
            message = e.getMessage();
            logger.warn("Access error for user " + User.getCurrentUser().getIdentity(), e);
        } else if (e instanceof IllegalArgumentException || e instanceof BadRequestException) {
            status = Response.Status.BAD_REQUEST.getStatusCode();
            message = e.getMessage();
            logger.warn("Bad request", e);
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
            message = e.getMessage();
            logger.error("Internal error", e);
        }
        return Response.status(status).entity(message).type(TEXT_PLAIN_TYPE).build();
    }
}
