package no.nav.aura.fasit.rest.spring;

import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class RestParameterNameResolver implements HandlerMethodArgumentResolver {

    private final Validator validator;

    public RestParameterNameResolver(Validator validator) {
        this.validator = validator;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // Define which parameters this resolver handles
        return parameter.hasParameterAnnotation(RequestParam.class) ||
               parameter.hasParameterAnnotation(PathVariable.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                 NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        String paramName = getParameterName(parameter);
        String value = null;
        
        if (parameter.hasParameterAnnotation(RequestParam.class)) {
            RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
            value = webRequest.getParameter(paramName);
            
            if (value == null && requestParam.required() && 
                requestParam.defaultValue().equals(ValueConstants.DEFAULT_NONE)) {
                throw new MissingServletRequestParameterException(paramName, parameter.getParameterType().getName());
            }
        } else if (parameter.hasParameterAnnotation(PathVariable.class)) {
            Map<String, String> uriVariables = 
                (Map<String, String>) webRequest.getAttribute(
                    HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, 
                    RequestAttributes.SCOPE_REQUEST);
            if (uriVariables != null) {
                value = uriVariables.get(paramName);
            }
        }
        
        // Convert the value to the target type
        Object convertedValue = convertValue(value, parameter);
        
        // Validate if needed
        validate(parameter, convertedValue);
        
        return convertedValue;
    }

    private String getParameterName(MethodParameter parameter) {
        if (parameter.hasParameterAnnotation(RequestParam.class)) {
            RequestParam annotation = parameter.getParameterAnnotation(RequestParam.class);
            if (!annotation.value().isEmpty()) {
                return annotation.value();
            }
            if (!annotation.name().isEmpty()) {
                return annotation.name();
            }
        } else if (parameter.hasParameterAnnotation(PathVariable.class)) {
            PathVariable annotation = parameter.getParameterAnnotation(PathVariable.class);
            if (!annotation.value().isEmpty()) {
                return annotation.value();
            }
            if (!annotation.name().isEmpty()) {
                return annotation.name();
            }
        }
        
        return parameter.getParameterName();
    }

    private Object convertValue(String value, MethodParameter parameter) {
        // Implement your conversion logic here or use Spring's conversion service
        // For now, returning the string value
        return value;
    }

    private void validate(MethodParameter parameter, Object value) {
        if (value != null && validator != null) {
            BeanPropertyBindingResult errors = new BeanPropertyBindingResult(
                value, parameter.getParameterName());
            validator.validate(value, errors);
            if (errors.hasErrors()) {
                // Handle validation errors
            }
        }
    }
}
