package no.nav.aura.envconfig.rest;

import no.nav.aura.envconfig.spring.AccessException;
import no.nav.aura.envconfig.spring.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.NoResultException;
import java.util.List;

@ControllerAdvice
public class SimpleExceptionMapper {

    private static final Logger logger = LoggerFactory.getLogger(SimpleExceptionMapper.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException e) {
        logger.warn("Rest returned code: {} reason: {}", e.getStatus().value(), e.getReason());
        return ResponseEntity.status(e.getStatus())
                .contentType(MediaType.TEXT_PLAIN)
                .body(e.getReason());
    }

    @ExceptionHandler(NoResultException.class)
    public ResponseEntity<String> handleNoResultException(NoResultException e) {
        logger.error("No result found in database", e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.TEXT_PLAIN)
                .body(e.getMessage());
    }

    @ExceptionHandler(AccessException.class)
    public ResponseEntity<String> handleAccessException(AccessException e) {
        logger.warn("Access error for user " + User.getCurrentUser().getIdentity(), e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.TEXT_PLAIN)
                .body(e.getMessage());
    }

	@ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
	
	@ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<String> handleException(Exception e) {
        logger.error("Internal error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.TEXT_PLAIN)
                .body(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String response = generateMethodArgumentValidationString(exception);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.TEXT_PLAIN)
                .body(response);
    }

    private String generateMethodArgumentValidationString(MethodArgumentNotValidException exception) {
        StringBuffer result = new StringBuffer("Input did not pass validation:\n\n");
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();
        fieldErrors.forEach(fe -> result.append(format(fe)).append("\n"));
        return result.toString();
    }
    
    private String format(FieldError fe) {
        String parameter = fe.getField();
        Object value = fe.getRejectedValue();
        String valueStr = (value == null) ? "" : value.toString();
        
        if (valueStr.isEmpty()) {
            return String.format("%s: %s Reason: %s", "field", parameter.toLowerCase(), fe.getDefaultMessage().toLowerCase());
        }
        
        return String.format("%s: %s Reason: %s Value: %s", "field", parameter, fe.getDefaultMessage(), valueStr);
    }
}
