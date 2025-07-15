package no.nav.aura;

import no.nav.aura.envconfig.util.FlywayUtil;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@SpringBootApplication(exclude = {ErrorMvcAutoConfiguration.class})
@ComponentScan(basePackages = "no.nav.aura")
@EnableCaching
public class FasitApplication {
	private static final Logger log = LoggerFactory.getLogger(FasitApplication.class);
    public static void main(String[] args) {
        System.out.println("Starting fasit with Spring Boot...");
        setSystemProperties();
    	SpringApplication springApp = new SpringApplication(FasitApplication.class);
		springApp.setBannerMode(Banner.Mode.OFF);
		springApp.run(args);
    }

    @Bean
    DataSource dataSource() {
        String url = System.getProperty("envconfDB.url");
        String username = System.getProperty("envconfDB.username");
        String password = System.getProperty("envconfDB.password");

        System.setProperty("hibernate.default_schema", username);
        BasicDataSource ds = new BasicDataSource();
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setMaxWait(20000);
        System.out.println("using database " + ds.getUsername() + "@" + ds.getUrl());
        log.info("using database " + ds.getUsername() + "@" + ds.getUrl());
        FlywayUtil.migrateFlyway(ds);
        return ds;
    }

    private static void setSystemProperties() {
        File configFile = new File(System.getProperty("fasit.configDir") + "/" + "config.properties");
        if (configFile.isFile()) {
            try {
                Properties props = new Properties();
                props.load(new FileInputStream(configFile));
                props.entrySet().forEach(entry -> {
                	log.info("Setting system property: {}={}", entry.getKey(), entry.getValue());
                    System.setProperty(entry.getKey().toString(), entry.getValue().toString());
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
