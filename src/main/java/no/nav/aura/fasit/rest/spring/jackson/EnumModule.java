package no.nav.aura.fasit.rest.spring.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class EnumModule extends SimpleModule {

    public EnumModule() {
        super("EnumModule");
        
        setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyEnumDeserializer(DeserializationConfig config, 
                                                           final JavaType type,
                                                           BeanDescription beanDesc, 
                                                           final JsonDeserializer<?> deserializer) {
                return new CaseInsensitiveEnumDeserializer(type.getRawClass());
            }
        });
        
        setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public JsonSerializer<?> modifyEnumSerializer(SerializationConfig config, 
                                                       JavaType valueType,
                                                       BeanDescription beanDesc, 
                                                       JsonSerializer<?> serializer) {
                return new LowercaseEnumSerializer();
            }
        });
    }

    private static class CaseInsensitiveEnumDeserializer extends JsonDeserializer<Enum<?>> {
        private final Class<?> enumClass;

        private CaseInsensitiveEnumDeserializer(Class<?> enumClass) {
            this.enumClass = enumClass;
        }

        @Override
        public Enum<?> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            String value = jp.getText();
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
                String.format("Value: %s is not valid for enum %s. Use %s",
                    value, enumClass.getSimpleName(), validValues));
        }
    }
    
    
    // To serialize enums as lowercase strings
    private static class LowercaseEnumSerializer extends JsonSerializer<Enum<?>> {
        @Override
        public void serialize(Enum<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value != null) {
                gen.writeString(value.name().toLowerCase());
            } else {
                gen.writeNull();
            }
        }
    }
}

