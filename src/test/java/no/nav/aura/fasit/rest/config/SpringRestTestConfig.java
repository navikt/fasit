package no.nav.aura.fasit.rest.config;

import com.bettercloud.vault.Vault;
import no.nav.aura.envconfig.spring.SpringSecurityTestConfig;
import no.nav.aura.envconfig.util.InsideJobService;
import no.nav.aura.integration.VeraRestClient;
import no.nav.aura.sensu.SensuClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.mockito.Mockito.mock;

@Configuration
@Import({ SpringRestConfig.class, SpringSecurityTestConfig.class})
public class SpringRestTestConfig {
    
    private Logger log = LoggerFactory.getLogger(SpringRestTestConfig.class); 
    
    public SpringRestTestConfig() {
        log.warn("Running in testmode");
    }


    @Bean
    public InsideJobService getInsideJobService() {
        return new InsideJobService();
    }

    @Bean
    public VeraRestClient vera() {
        return mock(VeraRestClient.class);
    }

    @Bean
    public SensuClient sensuClient() {
        return mock(SensuClient.class);
    }

    @Bean
    public FasitKafkaProducer fasitKafkaProducer() {
        return mock(FasitKafkaProducer.class);
    }

    @Bean
    public Vault vaultClient() {
        return mock(Vault.class);
    }

}
