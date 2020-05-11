package no.nav.aura.envconfig.spring;

import no.nav.aura.envconfig.DataIntegrityRulesEvaluator;
import no.nav.aura.envconfig.FasitRepository;

import no.nav.aura.envconfig.filter.DummyUserLookup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@Configuration
@EnableJdbcHttpSession
@ImportResource({ "classpath:spring-test-security.xml" })
@Import(SpringSecurityHandlersConfig.class)
public class SpringSecurityTestConfig {

    @Bean
    public DummyUserLookup ldapUserLookup() {
        return new DummyUserLookup();
    }

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
