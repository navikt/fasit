package no.nav.aura.fasit.rest.jaxrs;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Provider
public class WebApplicationExceptionMapper  implements ExceptionMapper<WebApplicationException>{

    private Logger log = LoggerFactory.getLogger(WebApplicationExceptionMapper.class);
    
    @Override
    public Response toResponse(WebApplicationException exception) {
        log.info(exception.getMessage());
        return Response.fromResponse(exception.getResponse()).entity(exception.getMessage()).header("Content-Type", "text/plain").build();
    }

}
