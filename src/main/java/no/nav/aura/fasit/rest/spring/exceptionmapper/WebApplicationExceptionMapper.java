package no.nav.aura.fasit.rest.spring.exceptionmapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@Component
@ControllerAdvice
public class WebApplicationExceptionMapper {

    private Logger log = LoggerFactory.getLogger(WebApplicationExceptionMapper.class);
    
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException exception) {
        log.info(exception.getReason());
        return ResponseEntity
                .status(exception.getStatusCode().value())
                .header("Content-Type", "text/plain")
                .body(exception.getReason());
    }

}
