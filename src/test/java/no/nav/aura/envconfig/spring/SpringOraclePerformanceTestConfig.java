package no.nav.aura.envconfig.spring;

import no.nav.aura.envconfig.util.TestDatabaseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@Import({ SpringUnitTestConfig.class })
public class SpringOraclePerformanceTestConfig {

    public static final Logger logger = LoggerFactory.getLogger(SpringOraclePerformanceTestConfig.class);

    @Bean(name = "dataSource")
    DataSource dataSource() {
        DataSource dataSource = TestDatabaseHelper.createDatasourceFromPropertyfile();
        return dataSource;
    }

}
