package no.nav;

import static org.mockito.Mockito.mock;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import no.nav.aura.envconfig.spring.SpringDomainConfig;
import no.nav.aura.envconfig.spring.SpringSecurityTestConfig;
import no.nav.aura.envconfig.util.InsideJobService;
import no.nav.aura.integration.FasitKafkaProducer;
import no.nav.aura.integration.VeraRestClient;

@Configuration
@EnableAutoConfiguration(exclude = { FlywayAutoConfiguration.class })
@Import({ SpringDomainConfig.class, SpringSecurityTestConfig.class})
public class StandaloneRunnerTestConfig {
	private static final Logger logger = LoggerFactory.getLogger(StandaloneRunnerTestConfig.class);
	
    @Bean
    DataSource getDataSource() {
        System.setProperty("envconfDB.dbtype", "h2");
        System.setProperty("useH2", "true");
		return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
	}

    @Bean
    static BeanFactoryPostProcessor init() {
    	System.setProperty("ldap.url", "ldap://ldapgw.adeo.no");
        System.setProperty("ldap.domain", "adeo.no");
        System.setProperty("ldap.user.basedn", "ou=NAV,ou=BusinessUnits,dc=adeo,dc=no");

        System.setProperty("ldap.username", "binduser");
        System.setProperty("ldap.password", "binduserpasswd");

        System.setProperty("ROLE_OPERATIONS.groups", "0000-GA-env-config-TestAdmin");
        System.setProperty("ROLE_PROD_OPERATIONS.groups", "0000-GA-env-config-ProdAdmin");
        // select encryption keys
        System.setProperty("fasit.encryptionkeys.username", "junit");
        System.setProperty("fasit.encryptionkeys.password", "password");

        // Use for testing Vera integration locally
        System.setProperty("deployLog_v1.url", "http://vera.com/api/v1/deployLog");
        System.setProperty("environment.name", "dev");
        System.setProperty("environment.class", "u");

        // Used for Kafka integration
        System.setProperty("publish.deployment.events.to.kafka", "false");
        System.setProperty("kafka.servers", "localhost:9092");
        System.setProperty("kafka.sasl.enabled", "false");
        System.setProperty("kafka.username", "tull");
        System.setProperty("kafka.password", "fjas");
        System.setProperty("kafka.deployment.event.topic", "deployment-event");

        System.setProperty("vault.url", "https://avaulturl.com");
        System.setProperty("systemuser.srvfasit.username", "srvfasit");
        System.setProperty("systemuser.srvfasit.password", "secret");

//        // Creating som testdata
//        Environment environment = new Environment("q1", EnvironmentClass.q);
        


		logger.info("init StandaloneRunnerTestConfig");
//		PropertyPlaceholderConfigurer propertyConfigurer = new PropertyPlaceholderConfigurer();
//		propertyConfigurer.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
        PropertySourcesPlaceholderConfigurer propertyConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertyConfigurer.setPropertySources(new MutablePropertySources());
        
        
		return propertyConfigurer;
	}

    @Bean
    FasitKafkaProducer fasitKafkaProducer() {
        return mock(FasitKafkaProducer.class);
    }

    @Bean
    InsideJobService getInsideJobService() {
        return new InsideJobService();
    }
	
    @Bean
    VeraRestClient vera() {
        return mock(VeraRestClient.class);
    }
}
