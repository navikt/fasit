package no.nav.aura.fasit.rest.config;

import no.nav.aura.fasit.repository.ResourceRepository;
import no.nav.aura.fasit.repository.RevisionRepository;
import no.nav.aura.fasit.rest.search.SearchRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackageClasses = ResourceRepository.class)
public class SpringRepositoryConfig {
    
    @Bean(name="revisionRepository")
    public RevisionRepository revisionRepository() {
        return new RevisionRepository();
    }

    @Bean
    public SearchRepository searchRepository() {
        return new SearchRepository();
    }

}
