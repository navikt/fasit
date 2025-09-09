package no.nav.aura.envconfig;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class JPABooleanToNumberConverter implements AttributeConverter<Boolean, Integer>{
    @Override
    public Integer convertToDatabaseColumn(Boolean attribute) {
        return attribute == null ? null : (attribute ? 1 : 0);
    }

    @Override
    public Boolean convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : dbData != 0;
    }
}
