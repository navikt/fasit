package no.nav.aura.fasit.rest.jaxrs;

import static org.hibernate.validator.internal.util.CollectionHelper.newArrayList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.validation.ParameterNameProvider;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

public class RestAnnotationParamterNameProvider implements ParameterNameProvider {
    
    public RestAnnotationParamterNameProvider() {
        System.out.println("RestAnnotationParamterNameProvider");
    }
    
    @Override
    public List<String> getParameterNames(Constructor<?> constructor) {
        return getParameterNames( constructor.getParameterTypes().length );
    }

    @Override
    public List<String> getParameterNames(Method method) {
        Annotation[][] annotationsByParam = method.getParameterAnnotations();
        List<String> names = new ArrayList<>(annotationsByParam.length);
        for (Annotation[] annotations : annotationsByParam) {
            String name = getParamName(annotations);
            if (name == null)
                name = "arg" + (names.size() + 1);

            names.add(name);
        }

        return names;
    }

    private String getParamName(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == QueryParam.class) {
                return QueryParam.class.cast(annotation).value();
            } else if (annotation.annotationType() == PathParam.class) {
                return PathParam.class.cast(annotation).value();
            }
        }

        return null;
    }
    
    private List<String> getParameterNames(int parameterCount) {
        List<String> parameterNames = newArrayList();

        for ( int i = 0; i < parameterCount; i++ ) {
            parameterNames.add( getPrefix() + i );
        }

        return parameterNames;
    }

    /**
     * Returns the prefix to be used for parameter names. Defaults to {@code arg} as per
     * the spec. Can be overridden to create customized name providers.
     *
     * @return The prefix to be used for parameter names.
     */
    protected String getPrefix() {
        return "arg";
    }

   
}
