package no.nav.aura.fasit.rest.converter;

import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.resource.FileEntity;
import no.nav.aura.envconfig.model.resource.PropertyField;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.fasit.rest.helpers.ValidationHelpers;
import no.nav.aura.fasit.rest.model.ResourcePayload;
import no.nav.aura.fasit.rest.model.ScopePayload;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.RegexValidator;

import javax.ws.rs.BadRequestException;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.stream.Collectors.*;
import static no.nav.aura.envconfig.model.resource.PropertyField.Type.ENUM;
import static no.nav.aura.envconfig.model.resource.PropertyField.Type.SECRET;
import static no.nav.aura.envconfig.model.resource.PropertyField.ValidationType;

public class Payload2ResourceTransformer extends FromPayloadTransformer<ResourcePayload, Resource> {

    private ValidationHelpers validationHelpers;
    private final Optional<Resource> defaultValue;

    public Payload2ResourceTransformer(ValidationHelpers validationHelpers, Resource defaultValue) {
        this.validationHelpers = validationHelpers;
        this.defaultValue = Optional.ofNullable(defaultValue);
    }

    @Override
    protected Resource transform(ResourcePayload payload) {
        ScopePayload payloadScope = payload.scope;
        Resource resource;
        Scope scope = buildScope(payloadScope);

        validatePropertiesForType(payload);
        validatePropertyContent(payload);

        String trimmedAlias = payload.alias.trim();

        if(defaultValue.isPresent()) {
            resource = defaultValue.get();
            resource.setScope(scope);
            resource.setAlias(trimmedAlias);
        }
        else {
            resource = new Resource(trimmedAlias, payload.type, scope);
        }


        payload.properties.forEach((key, value) -> resource.putProperty(key, value));
        payload.secrets.forEach((key, secretPayload) -> {
            if (secretPayload.vaultpath != null) {
                resource.putSecretWithVaultPath(key, secretPayload.vaultpath);
            } else {
                resource.putSecretWithValue(key, secretPayload.value);
            }
        });

        payload.files.forEach((propertyName, filePayload) -> {
            String fileContent = payload.files.get(propertyName).fileContent.split(",")[1];
            byte[] decode = Base64.getDecoder().decode(fileContent);
            resource.putFile(propertyName, new FileEntity(payload.files.get(propertyName).filename, new ByteArrayInputStream(decode)));
        });

        // and remember ad groups

        optional(payload.dodgy).ifPresent(dodgy -> resource.markAsDodgy(dodgy));

        return resource;
    }

    private Scope buildScope(ScopePayload payloadScope) {
        Scope scope = new Scope().envClass(payloadScope.environmentclass);
        optional(payloadScope.zone).ifPresent(z -> scope.domain(Domain.from(payloadScope.environmentclass, payloadScope.zone)));

        if (payloadScope.environment != null) {
            scope.environment(validationHelpers.getEnvironment(payloadScope.environment));
        }

        if (payloadScope.application != null) {
            scope.application(validationHelpers.getApplication(payloadScope.application));
        }
        return scope;
    }


    private void validatePropertyContent(ResourcePayload payload) {
        payload.properties.keySet().forEach(propertyKey -> {

            payload.type.findResourcePropertyField(propertyKey).ifPresent(resourcePropertyField -> {
                String property = Optional.ofNullable(payload.properties.get(propertyKey)).orElse("");
                resourcePropertyField.getValidation().ifPresent(validationType -> validateByType(validationType, property, resourcePropertyField));
                resourcePropertyField.getValidationPattern().ifPresent(pattern -> validateByRegExpPattern(pattern, propertyKey, property));

                if (resourcePropertyField.getType().equals(ENUM)) {
                    validateEnum(resourcePropertyField, property);
                }
            });
        });
    }

    private void validatePropertiesForType(ResourcePayload payload) {
        List<String> validationMessages = new ArrayList<>();
        validationMessages.addAll(validateRequiredProps("properties", payload.type.getProperties(), payload.properties.keySet()));
        validationMessages.addAll(validateRequiredProps("secrets", payload.type.getFieldsBy(SECRET), payload.secrets.keySet()));

        if (!validationMessages.isEmpty()) {
            throw new BadRequestException(join(validationMessages, "\n"));
        }
    }

    private List<String> validateRequiredProps(String propType, Set<PropertyField> supportedFields, Set<String> payloadProperties) {


        List<String> validationMessages = new ArrayList<>();
        Set<String> supportedFieldNames = supportedFields.stream().map((field) -> field.getName()).collect(toSet());

        List<String> missingRequiredProperties = supportedFields.stream().
                filter(field -> !field.isOptional()).
                filter(field -> !payloadProperties.contains(field.getName())).
                map(field -> format("Missing required key in %s: %s", propType, field.getName())).
                collect(toList());

        List<String> unsupportedProperties = payloadProperties.stream().
                filter(propertyKey -> !supportedFieldNames.contains(propertyKey)).
                map(propertyKey -> format("Unsupported key in %s: %s", propType, propertyKey))
                .collect(toList());

        validationMessages.addAll(missingRequiredProperties);
        validationMessages.addAll(unsupportedProperties);

        return validationMessages;
    }

    private void validateByType(ValidationType validationType, String propToValidate, PropertyField resourcePropertyField) {
        boolean valid = true;

        switch (validationType) {
            case EMAIL:
                EmailValidator emailValidator = EmailValidator.getInstance();
                valid = emailValidator.isValid(propToValidate);
                break;
        }

        if (!valid) {
            throw new BadRequestException(format("Invalid format for property %s: %s. Property must match %s format",
                    resourcePropertyField.getName(),
                    propToValidate, validationType));
        }
    }


    private void validateEnum(PropertyField resourcePropertyField, String property) {
        List<String> validEnumValues = resourcePropertyField.getValues().stream().map(String::toLowerCase).collect(toList());
        if (!validEnumValues.contains(property.toLowerCase())) {
            throw new BadRequestException(format("Invalid enum value for %s. Use one of [%s]",
                    resourcePropertyField.getName().toLowerCase(), join(validEnumValues, ", ")));
        }
    }


    private void validateByRegExpPattern(Pattern regexpPattern, String propertyKey, String propToValidate) {
        String validationPattern = regexpPattern.pattern();
        RegexValidator regexValidator = new RegexValidator(validationPattern);

        if (!regexValidator.isValid(propToValidate)) {
            throw new BadRequestException(format("property %s: %s does not match pattern %s", propertyKey, propToValidate, validationPattern));
        }
    }

    private String join(List<String> messages, String delimiter) {
        return messages.stream().collect(joining(delimiter));
    }
}
