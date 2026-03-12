package no.nav.aura.envconfig.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

import javax.sql.DataSource;

import no.nav.aura.envconfig.spring.SpringOracleUnitTestConfig;

import org.apache.commons.dbcp2.BasicDataSource;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.flywaydb.core.Flyway;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

public abstract class TestDatabaseHelper {

    private static Logger log = LoggerFactory.getLogger(TestDatabaseHelper.class);

    /** Uninstantiateable */
    private TestDatabaseHelper() {
    }

    public static DataSource createDatasourceFromPropertyfile() {
        Properties dbProperties = new Properties();
        try {
            File propertyFile = new File(System.getProperty("user.home"), "database.properties");
            if (!propertyFile.exists()) {
                throw new IllegalArgumentException("Propertyfile does not exist " + propertyFile.getAbsolutePath());
            }
            dbProperties.load(new FileInputStream(propertyFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return createDataSource(dbProperties.getProperty("db.type"), dbProperties.getProperty("db.url"), dbProperties.getProperty("db.username"), dbProperties.getProperty("db.password"));
    }

    public static DataSource createDataSource(String type, String url, String username, String password) {
        if ("h2".equalsIgnoreCase(type)) {
            System.setProperty("useH2", "true");
            System.setProperty("envconfDB.dbtype", "h2");
            BasicDataSource ds = new BasicDataSource();
            ds.setUrl(url);
            ds.setUsername(username);
            ds.setPassword(password);
            ds.setMaxWait(Duration.ofMillis(20000));
            ds.setMinIdle(0);
            ds.setMinEvictableIdle(Duration.ofMillis(10000));
            System.out.println("using database " + ds.getUserName() + "@" + ds.getUrl());

            Resource script = new DefaultResourceLoader().getResource("org/springframework/session/jdbc/schema-h2.sql");
            System.out.println("Using spring session script " + script.getFilename());
            ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator(script);
            resourceDatabasePopulator.setContinueOnError(true);
            resourceDatabasePopulator.execute(ds);

            return ds;
        } else {
            System.setProperty("hibernate.default_schema", username);

            BasicDataSource ds = new BasicDataSource();
            ds.setUrl(url);
            ds.setUsername(username);
            ds.setPassword(password);
            ds.setMaxWait(Duration.ofMillis(20000));
            ds.setMinIdle(0);
            ds.setMinEvictableIdle(Duration.ofMillis(10000));
            System.out.println("using database " + ds.getUserName() + "@" + ds.getUrl());
            return ds;
        }
    }

    public static void annihilateAndRebuildDatabaseSchema(DataSource dataSource) {
        TestDatabaseHelper.updateDatabaseSchema(dataSource, true, FlywayUtil.DB_MIGRATION_ENVCONF_DB);
    }

    public static void updateDatabaseSchema(DataSource dataSource, String... locations) {
        updateDatabaseSchema(dataSource, false, locations);
    }

    public static void updateDatabaseSchema(DataSource dataSource) {
        updateDatabaseSchema(dataSource, false, FlywayUtil.DB_MIGRATION_ENVCONF_DB);
    }

    private static void updateDatabaseSchema(DataSource dataSource, boolean annihilate, String... locations) {
        if (annihilate) {
        	Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations(locations)
                    .validateOnMigrate(false)
                    .load();
        	flyway.clean();
            MigrateResult result = flyway.migrate();
        }

        // Skip migrations if in-memory/H2, our scripts are not compatible
        if (!Boolean.valueOf(System.getProperty("useH2"))) {
            FlywayUtil.migrateFlyway(dataSource);
        }
    }

    public static void createTemporaryDatabase() {
        String schemaName = TestDatabaseHelper.createTemporarySchema();
        System.getProperties().setProperty(SpringOracleUnitTestConfig.TEMPORARY_DATABASE_SCHEMA, schemaName);
    }

    public static DataSource getAdminDataSource() {
        return createDataSource("oracle", SpringOracleUnitTestConfig.URL, "admin", "Start1");
    }

    public static String createTemporarySchema() {
        try (Connection connection = getAdminDataSource().getConnection()) {
            Statement statement = connection.createStatement();
            String schemaName = ("JUNIT" + UUID.randomUUID().toString().replaceAll("-", "")).substring(0, 16);
            statement.execute("CREATE USER " + schemaName + " PROFILE DEFAULT IDENTIFIED BY password DEFAULT TABLESPACE DEV TEMPORARY TABLESPACE TEMP ACCOUNT UNLOCK");
            statement.execute("grant connect to " + schemaName);
            statement.execute("grant resource to " + schemaName);
            statement.execute("grant create table to " + schemaName);
            // statement.execute("grant create index to " + schemaName);
            return schemaName;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void dropTemporaryDatabase() {
        String schemaName = System.getProperty(SpringOracleUnitTestConfig.TEMPORARY_DATABASE_SCHEMA);
        if (schemaName != null) {
            try (Connection connection = getAdminDataSource().getConnection()) {
                Statement statement = connection.createStatement();
                statement.execute("drop user " + schemaName + " cascade");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

   /* public static DataSource createH2() {
        return createDataSource("h2", "jdbc:h2:mem:", "sa", "");
    }*/
}
