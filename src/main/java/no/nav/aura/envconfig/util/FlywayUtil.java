package no.nav.aura.envconfig.util;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

public class FlywayUtil {
    private static Logger log = LoggerFactory.getLogger(FlywayUtil.class);

    public static final String DB_MIGRATION_ENVCONF_DB = "/db/migration/envconfDB";

    public static void migrateFlyway(DataSource dataSource) {
    	log.info("Starting Flyway migration for envconfDB");
    	
    	Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(DB_MIGRATION_ENVCONF_DB)
                .load();
        MigrateResult result = flyway.migrate();
        log.info("{} flyway migration scripts ran", result.migrationsExecuted);
    }
}
