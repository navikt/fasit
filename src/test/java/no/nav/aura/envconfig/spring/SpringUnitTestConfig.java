package no.nav.aura.envconfig.spring;

import javax.sql.DataSource;

import com.bettercloud.vault.Vault;
import no.nav.aura.envconfig.util.InsideJobService;

import no.nav.aura.envconfig.util.TestDatabaseHelper;
import no.nav.aura.integration.FasitKafkaProducer;
import no.nav.aura.sensu.SensuClient;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.mockito.Mockito.mock;

@Configuration
@EnableTransactionManagement
@Import({ SpringDomainConfig.class })
public class SpringUnitTestConfig {

    public SpringUnitTestConfig() {
        System.setProperty("fasit.encryptionkeys.username", "junit");
        System.setProperty("fasit.encryptionkeys.password", "password");
    }

    @Bean
    public SensuClient sensuClient() {
        return mock(SensuClient.class);
    }

    @Bean(name = "dataSource")
    public DataSource dataSource() {
        return TestDatabaseHelper.createDataSource("h2", "jdbc:h2:mem:", "sa", "");
    }

    @Bean
    public FasitKafkaProducer fasitKafkaProducer() {
        //return new FasitKafkaProducer();
        KafkaProducer kafkaProducerMock = mock(KafkaProducer.class);
        return new FasitKafkaProducer(kafkaProducerMock);
    }

    @Bean
    public InsideJobService getInsideJobService() {
        return new InsideJobService();
    }

}
