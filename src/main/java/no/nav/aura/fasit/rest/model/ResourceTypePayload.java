package no.nav.aura.fasit.rest.model;

import no.nav.aura.envconfig.model.resource.PropertyField;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.ResourceTypeDocumentation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;
import static no.nav.aura.envconfig.model.resource.PropertyField.Type.*;

public class ResourceTypePayload {
    String type;
    List<String> requiredProperties;
    List<String> optionalProperties;
    List<String> requiredSecrets;
    List<String> optionalSecrets;
    List<String> requiredFiles;
    List<String> optionalFiles;

    ResourceTypeDocumentation documentation;

    public ResourceTypePayload(ResourceType type) {
        this.type = type.toString();
        Set<PropertyField> properties = type.getProperties();
        Set<PropertyField> secrets = type.getFieldsBy(SECRET);
        Set<PropertyField> files = type.getFieldsBy(FILE);

        this.documentation = type.getResourceDocumentation();

        Predicate<PropertyField> requiredFieldsFilter = field -> !field.isOptional();
        Predicate<PropertyField> optionalFieldsFilter = PropertyField::isOptional;

        filterByPredicate(properties, requiredFieldsFilter).ifPresent(filterered -> this.requiredProperties = filterered);
        filterByPredicate(properties, optionalFieldsFilter).ifPresent(filterered -> this.optionalProperties = filterered);
        filterByPredicate(secrets, requiredFieldsFilter).ifPresent(filterered -> this.requiredSecrets = filterered);
        filterByPredicate(secrets, optionalFieldsFilter).ifPresent(filterered -> this.optionalSecrets = filterered);
        filterByPredicate(files, requiredFieldsFilter).ifPresent(filterered -> this.requiredFiles = filterered);
        filterByPredicate(files, optionalFieldsFilter).ifPresent(filterered -> this.optionalFiles = filterered);
    }

    private Optional<List<String>> filterByPredicate(Set<PropertyField> allSecrets, Predicate<PropertyField> predicate) {
        List<String> filtered = allSecrets.stream().
                filter(predicate).
                map(PropertyField::getName).
                collect(toList());

        if(filtered.size() > 0 ){
            return Optional.of(filtered);
        }
        return Optional.empty();
    }
}

