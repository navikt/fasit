package no.nav.aura.envconfig;

import no.nav.aura.envconfig.spring.SpringDomainConfig;
import no.nav.aura.envconfig.util.TestDatabaseHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@Import({ SpringDomainConfig.class })
public class SpringTestConfig {

    @Bean(name = "dataSource")
    public DataSource dataSource() {
        return TestDatabaseHelper.createDatasourceFromPropertyfile();
    }
}
