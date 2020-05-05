package no.nav.aura.integration.spring;

import no.nav.aura.integration.VeraRestClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VeraSpringConfig {
    @Value("${deployLog_v1.url}")
    private String veraUrl;

    @Bean
    public VeraRestClient vera() {
        return new VeraRestClient(veraUrl);
    }

}
