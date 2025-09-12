package no.nav.aura.envconfig.util;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

public class FlywayUtil {
    private static Logger log = LoggerFactory.getLogger(FlywayUtil.class);

    public static final String DB_MIGRATION_ENVCONF_DB = "/db/migration/envconfDB";

    public static void migrateFlyway(DataSource dataSource) {
        Flyway flyway = new Flyway();

        flyway.setDataSource(dataSource);
        flyway.setLocations(DB_MIGRATION_ENVCONF_DB);

        int migrationsApplied = flyway.migrate();
        log.info(migrationsApplied + " flyway migration scripts ran");
    }
}
