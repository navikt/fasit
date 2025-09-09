package no.nav.aura.fasit.rest.spring.jackson;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

@Component
public class JacksonConfig extends WebMvcAutoConfiguration {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    @Bean
    ObjectMapper objectMapper() {
		// Create a custom ObjectMapper with JavaTimeModule and EnumModule
		// to handle Java 8 time types and case-insensitive enum deserialization
    	// JAvaTimeModule is used to handle Java 8 date/time types
    	
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new EnumModule());

        // Configure JavaTimeModule with custom formatter for LocalDateTime
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, 
                new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
        
        objectMapper.registerModule(javaTimeModule);
//        objectMapper.setPropertyNamingStrategy(new LowerCaseStrategy());

        // Disable writing dates as timestamps
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Ignore unknown properties (fields not mapped in Java classes)
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

//    	ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
//                .modules(new JavaTimeModule(), new EnumModule())
//                .build();
        return objectMapper;
    }

    @Bean
    MappingJackson2HttpMessageConverter customJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper());
        return converter;
    }

//    @Override
//    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
//        converters.add(0, customJackson2HttpMessageConverter());
//    }
}