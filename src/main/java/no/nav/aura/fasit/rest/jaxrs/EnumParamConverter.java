package no.nav.aura.fasit.rest.jaxrs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

import org.springframework.stereotype.Component;

@Provider
@Component
public class EnumParamConverter implements ParamConverterProvider {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {

        if (rawType.isEnum()) {
            return (ParamConverter<T>) new IgnoreCaseEnumConverter(rawType);
        }
        return null;
    }

  

}
