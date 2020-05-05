package no.nav.aura.envconfig.spring;

import javax.sql.DataSource;

import no.nav.aura.envconfig.util.TestDatabaseHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@Import({ SpringUnitTestConfig.class })
public class SpringOracleUnitTestConfig {

    public static final String URL = "jdbc:oracle:thin:@d26dbfl004.test.local:1521:DBcfc01b";
    public static final String TEMPORARY_DATABASE_SCHEMA = "TEMPORARY_DATABASE_SCHEMA";
    public static final Logger logger = LoggerFactory.getLogger(SpringOracleUnitTestConfig.class);

    @Bean(name = "dataSource")
    public DataSource dataSource() {
        System.getProperties().remove("useH2");
        String schemaName = System.getProperty(TEMPORARY_DATABASE_SCHEMA);
        if (schemaName == null) {
            throw new RuntimeException("Missing temporary database schema");
        }
        DataSource dataSource = TestDatabaseHelper.createDataSource("oracle", URL, schemaName, "password");
        TestDatabaseHelper.annihilateAndRebuildDatabaseSchema(dataSource);
        return dataSource;
    }

}
