package no.nav.aura.fasit.rest.jaxrs;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ext.ParamConverter;

public class IgnoreCaseEnumConverter<T extends Enum<T>> implements ParamConverter<T> {

    private Class<T> enumType;

    public IgnoreCaseEnumConverter(Class<T> enumType) {
        this.enumType = enumType;

    }

    @Override
    public T fromString(String value) {

        for (T e : enumType.getEnumConstants()) {
            if (e.name().equalsIgnoreCase(value)) {
                return e;
            }
        }
        
        Set<String> validNames = Arrays.asList(enumType.getEnumConstants()).stream()
                .map(e -> e.name())
                .collect(Collectors.toSet());
        throw new BadRequestException(String.format("Value: %s is not valid for enum %s. Use %s ", value, enumType.getSimpleName(), validNames));
    }

    @Override
    public String toString(T value) {
        return value.name().toLowerCase();
    }
}
