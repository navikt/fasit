package no.nav.aura.integration.spring;

import no.nav.aura.integration.FasitKafkaProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaSpringConfig {


        @Bean
        public FasitKafkaProducer fasitKafkaProducer() {
            System.out.println("Initializing fasit Kafka producer");
            return new FasitKafkaProducer();
        }
}
