package no.nav.aura.envconfig.spring;

import javax.sql.DataSource;

import no.nav.aura.envconfig.util.InsideJobService;

import no.nav.aura.envconfig.util.TestDatabaseHelper;
import no.nav.aura.fasit.rest.search.SearchRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@Import({ SpringDomainConfig.class, SearchRepository.class })
public class SpringUnitTestConfig {

    public SpringUnitTestConfig() {
        System.setProperty("fasit.encryptionkeys.username", "junit");
        System.setProperty("fasit.encryptionkeys.password", "password");
    }

    @Bean(name = "dataSource")
    public DataSource dataSource() {
        return TestDatabaseHelper.createDataSource("h2", "jdbc:h2:mem:spring", "sa", "");
    }

    @Bean
    public InsideJobService getInsideJobService() {
        return new InsideJobService();
    }

}
