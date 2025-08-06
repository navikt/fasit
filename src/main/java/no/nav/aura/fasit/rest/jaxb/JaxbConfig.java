package no.nav.aura.fasit.rest.jaxb;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;

@Configuration
public class JaxbConfig {

	@Bean
	Jaxb2RootElementHttpMessageConverter jaxb2RootElementHttpMessageConverter() {
	    Jaxb2RootElementHttpMessageConverter converter = new Jaxb2RootElementHttpMessageConverter();
	    converter.setSupportedMediaTypes(Arrays.asList(
	            MediaType.APPLICATION_XML,
	            MediaType.TEXT_XML));
	    return converter;
	}

}
