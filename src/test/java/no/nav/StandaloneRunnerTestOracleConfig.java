package no.nav;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.oracle.OracleContainer;

import no.nav.aura.envconfig.util.FlywayUtil;

@TestConfiguration
public class StandaloneRunnerTestOracleConfig  {
	private static final Logger logger = LoggerFactory.getLogger(StandaloneRunnerTestOracleConfig.class);
    static String image = "gvenzl/oracle-free:23.6-slim-faststart";

    @SuppressWarnings("resource") // connection is closed in AfterAll
	@Container
    static OracleContainer oracleContainer = new OracleContainer(image)
            .withStartupTimeout(Duration.ofMinutes(3))
            .withUsername("testuser")
            .withPassword("testpwd");
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("JDBC_URL", oracleContainer::getJdbcUrl);
        registry.add("USERNAME", oracleContainer::getUsername);
        registry.add("PASSWORD", oracleContainer::getPassword);
    }
    
    @Bean
    @Primary
    DataSource getOracleDataSource() {
//        System.setProperty("envconfDB.dbtype", "oracle");
    	System.getProperties().remove("useH2");
        oracleContainer.start();
        BasicDataSource ds = new BasicDataSource();
        ds.setUrl(oracleContainer.getJdbcUrl());
        ds.setUsername(oracleContainer.getUsername());
        ds.setPassword(oracleContainer.getPassword());
        ds.setMaxWait(Duration.ofMillis(20000));
        FlywayUtil.migrateFlyway(ds);
        /*try (Connection conn = ds.getConnection()) {
            dumpSchemaDDL(conn, oracleContainer.getUsername());
        } catch (SQLException e) {
			e.printStackTrace();
		}*/
        return ds;
	}
    
    private void dumpSchemaDDL(Connection conn, String schema) throws SQLException {
        String sql = "SELECT DBMS_METADATA.GET_DDL('TABLE', table_name, owner) " +
                     "FROM all_tables WHERE owner = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String ddl = rs.getString(1);
                    System.out.println(ddl); // Or write to a file
                }
            }
        }
    }

}
