package no.nav.aura.fasit.rest.jaxrs;

import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ResteasyViolationException;
import org.jboss.resteasy.api.validation.ResteasyViolationExceptionMapper;
import org.springframework.stereotype.Component;


@Provider
@Component
public class ValidationExeptionMapper extends ResteasyViolationExceptionMapper {
    
    protected Response buildViolationReportResponse(ResteasyViolationException exception, Status status)
    {
      Response response = super.buildViolationReportResponse(exception, status);
      if(response.getMediaType().equals(MediaType.TEXT_PLAIN_TYPE)){
         return Response.fromResponse(response).entity(generateValidationString(exception)).build();
      }
      return response;
    }


    private String generateValidationString(ResteasyViolationException exception) {
        StringBuffer result= new StringBuffer("Input did not pass validation:\n\n");
        List<ResteasyConstraintViolation> parameterViolations = exception.getViolations();
        parameterViolations.forEach(v -> result.append(format(v) + "\n"));
        return result.toString();
    }


    private String format(ResteasyConstraintViolation v) {
        String parameter = v.getPath();
        // hack siden vi ikke kan sette vår egen path transformer
        if(parameter.contains(".arg0.")){
            parameter=parameter.split(".arg0.")[1];
        }
        if(v.getValue()== null || v.getValue().isEmpty()){
            return String.format("%s: %s Reason: %s ", v.getConstraintType().name().toLowerCase(), parameter.toLowerCase(), v.getMessage().toLowerCase() );
        }
        
        return String.format("%s: %s Reason: %s Value: %s",v.getConstraintType().name().toLowerCase(), parameter, v.getMessage(), v.getValue() );
    }
    
    

}