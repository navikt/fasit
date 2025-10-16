package no.nav.aura.fasit.rest.spring;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class StringToEnumConverterFactory implements ConditionalGenericConverter {

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return sourceType.getType() == String.class && targetType.getType().isEnum();
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(String.class, Enum.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }

        String value = (String) source;
        Class<?> enumType = targetType.getType();
        
        Enum<?>[] enumConstants = (Enum<?>[]) enumType.getEnumConstants();
        for (Enum<?> constant : enumConstants) {
            if (constant.name().equalsIgnoreCase(value.trim())) {
                return constant;
            }
        }
        
        String validValues = Arrays.stream(enumConstants)
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            String.format("Value: %s is not valid for enum %s. Use %s",
                value, enumType.getSimpleName(), validValues));
    }
}