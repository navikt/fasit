package no.nav.aura.fasit.rest.spring.jackson;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.ValueSerializerModifier;

public class EnumModule extends SimpleModule {

    public EnumModule() {
        super("EnumModule");

        setDeserializerModifier(new ValueDeserializerModifier() {
            @Override
            public ValueDeserializer<?> modifyEnumDeserializer(DeserializationConfig config,
                                                               JavaType type,
                                                               BeanDescription.Supplier beanDescRef,
                                                               ValueDeserializer<?> deserializer) {
                return new CaseInsensitiveEnumDeserializer(type.getRawClass());
            }
        });

        setSerializerModifier(new ValueSerializerModifier() {
            @Override
            public ValueSerializer<?> modifyEnumSerializer(SerializationConfig config,
                                                           JavaType valueType,
                                                           BeanDescription.Supplier beanDescRef,
                                                           ValueSerializer<?> serializer) {
                return new LowercaseEnumSerializer();
            }
        });
    }

    private static class CaseInsensitiveEnumDeserializer extends ValueDeserializer<Enum<?>> {
        private final Class<?> enumClass;

        private CaseInsensitiveEnumDeserializer(Class<?> enumClass) {
            this.enumClass = enumClass;
        }

        @Override
        public Enum<?> deserialize(JsonParser jp, DeserializationContext ctxt) {
            String value = jp.getString();
            if (value == null || value.isEmpty()) {
                return null;
            }

            Enum<?>[] enumConstants = (Enum<?>[]) enumClass.getEnumConstants();
            for (Enum<?> constant : enumConstants) {
                if (constant.name().equalsIgnoreCase(value.trim())) {
                    return constant;
                }
            }

            String validValues = Arrays.stream(enumConstants)
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Value: %s is not valid for enum %s. Use %s".formatted(
                            value, enumClass.getSimpleName(), validValues));
        }
    }

    // To serialize enums as lowercase strings
    private static class LowercaseEnumSerializer extends ValueSerializer<Enum<?>> {
        @Override
        public void serialize(Enum<?> value, JsonGenerator gen, SerializationContext ctxt) {
            if (value != null) {
                gen.writeString(value.name().toLowerCase());
            } else {
                gen.writeNull();
            }
        }
    }
}
