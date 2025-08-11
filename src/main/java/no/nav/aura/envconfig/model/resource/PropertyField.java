package no.nav.aura.envconfig.model.resource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PropertyField {

        public  enum Type {
        TEXT, SECRET, FILE, ENUM
    }

    public  enum ValidationType {
        HTTP_URL, LDAP_URL, URL_LIST, EMAIL
    }

    private final String name;
    private final Type type;
    private ValidationType validation;
    private Pattern validationPattern;
    private final List<String> values;
    private boolean optional = false;

    public PropertyField(String name, Type type, List<String> values) {
        this.name = name;
        this.type = type;
        this.values = values;
    }

    protected PropertyField(String name, Type type) {
        this(name, type, null);
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public static PropertyField text(String name) {
        return new PropertyField(name, Type.TEXT);
    }

    public static PropertyField file(String name) {
        return new PropertyField(name, Type.FILE);
    }

    public static PropertyField secret(String name) {
        return new PropertyField(name, Type.SECRET);
    }

    public static PropertyField enumeration(String name, Class<? extends Enum<?>> enumClass) {
        List<? extends Enum<?>> constants = Arrays.asList(enumClass.getEnumConstants());
        List<String> props = constants.stream()
				.map(input -> input.name())
				.collect(Collectors.toList());
        
        
        return new PropertyField(name, Type.ENUM, props);
    }

    public PropertyField optional() {
        this.optional = true;
        return this;
    }

    public PropertyField validate(ValidationType validation) {
        this.validation = validation;
        return this;
    }
    

    public PropertyField validate(String pattern) {
        this.validationPattern= Pattern.compile(pattern);
        return this;
    }

    public Optional<ValidationType> getValidation() {
        return Optional.ofNullable(validation);
    }

    public List<String> getValues() {
        return values;
    }

    public boolean isOptional() {
        return optional;
    }

    public Optional<Pattern> getValidationPattern() {
        return Optional.ofNullable(validationPattern);
    }
}
