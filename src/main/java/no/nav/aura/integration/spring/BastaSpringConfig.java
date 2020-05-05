package no.nav.aura.integration.spring;

import no.nav.aura.integration.BastaRestClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BastaSpringConfig {
    @Value("${systemuser.srvfasit.username:srvfasit}")
    private String srvFasitUsername;
    @Value("${systemuser.srvfasit.password}")
    private String srvFasitPassword;
    @Value("${basta.url}")
    private String bastaUrl;
    @Value("${oracle_v1.url}")
    private String bastaOracleApiUrl;

    @Bean
    public BastaRestClient basta() {
        return new BastaRestClient(bastaUrl, bastaOracleApiUrl, srvFasitUsername, srvFasitPassword);
    }

}
