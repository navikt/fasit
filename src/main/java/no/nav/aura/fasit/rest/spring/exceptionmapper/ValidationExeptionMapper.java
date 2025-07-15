package no.nav.aura.fasit.rest.spring.exceptionmapper;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;


public class ValidationExeptionMapper{
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleConstraintViolationException(ConstraintViolationException exception) {
        String response = generateValidationString(exception);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.TEXT_PLAIN)
                .body(response);
    }

//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
//        String response = generateMethodArgumentValidationString(exception);
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .contentType(MediaType.TEXT_PLAIN)
//                .body(response);
//    }

//    private String generateValidationString(ResteasyViolationException exception) {
//        StringBuffer result= new StringBuffer("Input did not pass validation:\n\n");
//        List<ResteasyConstraintViolation> parameterViolations = exception.getViolations();
//        parameterViolations.forEach(v -> result.append(format(v) + "\n"));
//        return result.toString();
//    }
    private String generateValidationString(ConstraintViolationException exception) {
        StringBuffer result = new StringBuffer("Input did not pass validation:\n\n");
        exception.getConstraintViolations().forEach(v -> result.append(format(v) + "\n"));
        return result.toString();
    }

//    private String generateMethodArgumentValidationString(MethodArgumentNotValidException exception) {
//        StringBuffer result = new StringBuffer("Input did not pass validation:\n\n");
//        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();
//        fieldErrors.forEach(fe -> result.append(format(fe) + "\n"));
//        return result.toString();
//    }

    private String format(ConstraintViolation<?> v) {
        String parameter = v.getPropertyPath().toString();
        // Clean up parameter path
        if (parameter.contains(".arg0.")) {
            parameter = parameter.split("\\.arg0\\.")[1];
        }
        
        Object value = v.getInvalidValue();
        String valueStr = (value == null) ? "" : value.toString();
        
        if (valueStr.isEmpty()) {
            return String.format("%s: %s Reason: %s", "parameter", parameter.toLowerCase(), v.getMessage().toLowerCase());
        }
        
        return String.format("%s: %s Reason: %s Value: %s", "parameter", parameter, v.getMessage(), valueStr);
    }
    
//    private String format(FieldError fe) {
//        String parameter = fe.getField();
//        Object value = fe.getRejectedValue();
//        String valueStr = (value == null) ? "" : value.toString();
//        
//        if (valueStr.isEmpty()) {
//            return String.format("%s: %s Reason: %s", "field", parameter.toLowerCase(), fe.getDefaultMessage().toLowerCase());
//        }
//        
//        return String.format("%s: %s Reason: %s Value: %s", "field", parameter, fe.getDefaultMessage(), valueStr);
//    }
}