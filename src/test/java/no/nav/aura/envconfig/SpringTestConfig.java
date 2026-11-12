package no.nav.aura.envconfig;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import no.nav.aura.envconfig.spring.SpringDomainConfig;
import no.nav.aura.envconfig.util.TestDatabaseHelper;

@Configuration
@EnableTransactionManagement
@Import({ SpringDomainConfig.class })
public class SpringTestConfig {

    @Bean(name = "dataSource")
    public DataSource dataSource() {
        return TestDatabaseHelper.createDatasourceFromPropertyfile();
    }
}
