package no.nav.aura.envconfig.spring;

import no.nav.aura.envconfig.DataIntegrityRulesEvaluator;
import no.nav.aura.envconfig.FasitRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@Configuration
@EnableJdbcHttpSession
@ImportResource({ "classpath:spring-security.xml" })
public class SpringSecurityConfig {

    @Bean
    public DataIntegrityRulesEvaluator getDataIntegrityRulesEvaluator(FasitRepository repository) {
        return new DataIntegrityRulesEvaluator(repository);
    }

    @Bean
    public SecurityAccessCheckAspect getSecurityAccessCheck() {
        return new SecurityAccessCheckAspect();
    }

    @Bean
    public DataIntegrityRulesAspect getDataIntegrityRulesAspect() {
        return new DataIntegrityRulesAspect();
    }

    @Bean
    public PerformanceMeasureAspect getPerformanceMeasureAspect() {
        return new PerformanceMeasureAspect();
    }

}
