package no.nav.aura.fasit.rest.config;

import no.nav.aura.envconfig.rest.ResourcesRestService;
import no.nav.aura.envconfig.rest.SimpleExceptionMapper;
import no.nav.aura.envconfig.spring.SpringDomainConfig;
import no.nav.aura.envconfig.spring.SpringSecurityConfig;
import no.nav.aura.fasit.rest.ResourceRest;
import no.nav.aura.integration.spring.KafkaSpringConfig;
import no.nav.aura.integration.spring.VeraSpringConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
//@ComponentScan(basePackageClasses = ResourcesRestService.class)
//@EnableJpaRepositories(basePackageClasses = NodeRepository.class)
@ComponentScan(basePackageClasses = ResourceRest.class, excludeFilters = @ComponentScan.Filter(Configuration.class))
@Import({
        SpringSecurityConfig.class,
        SpringSecurityRestConfig.class,
        SpringDomainConfig.class,
//        SpringRepositoryConfig.class,
        KafkaSpringConfig.class,
        VeraSpringConfig.class,
})
public class SpringRestConfig {
    @Bean
    SimpleExceptionMapper getExceptionMapper() {
        return new SimpleExceptionMapper();
    }
}
