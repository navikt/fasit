package no.nav.aura.fasit.rest.spring.jackson;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

@Component
public class JacksonConfig {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    @Bean
    JsonMapper jsonMapper() {
        // java.time support is built into Jackson 3; only the LocalDateTime format is customized
        SimpleModule dateTimeModule = new SimpleModule("DateTimeModule");
        dateTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));

        return JsonMapper.builder()
                .addModule(new EnumModule())
                .addModule(dateTimeModule)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
                .build();
    }
}
