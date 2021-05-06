package no.nav.aura.integration;

import com.google.protobuf.Timestamp;
import no.nav.aura.envconfig.auditing.EntityCommenter;
import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.Cluster;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.protos.deployment.DeploymentEvent;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;


public class FasitKafkaProducer {

    private KafkaProducer<String, DeploymentEvent.Event> kafkaProducer;
    private static final Logger log = LoggerFactory.getLogger(FasitKafkaProducer.class);
    private String kafkaDeploymentEventTopic;


    public FasitKafkaProducer() {
        final String kafkaServers = getProperty("kafka.servers");
        final String kafkaCredstorePassword = getProperty("kafka.credstore.password");
        final String kafkaKeystorePath = getProperty("kafka.keystore.path");
        final String kafkaTruststorePath = getProperty("kafka.truststore.path");
        kafkaDeploymentEventTopic = getProperty("kafka.deployment.event.topic");

        Properties prop = new Properties();

        prop.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
        prop.put(ProducerConfig.CLIENT_ID_CONFIG, "fasit");
        prop.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "20000");
        prop.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "https");
        prop.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, "500");

        prop.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, kafkaKeystorePath);
        prop.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, kafkaTruststorePath);
        prop.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, kafkaCredstorePassword);
        prop.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, kafkaCredstorePassword);
        prop.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

        try {
            kafkaProducer = new KafkaProducer<>(prop, new StringSerializer(),
                    new DeploymentEventSerializer());
            log.info("Kafka client connected to " + kafkaServers);
        } catch (KafkaException ke) {
            throw new RuntimeException("Unable to connect to kafka " + kafkaServers, ke);
        }

    }

    public FasitKafkaProducer(KafkaProducer producer) {
        kafkaProducer = producer;
    }

    public void publishDeploymentEvent(ApplicationInstance appInstance, Environment environment) {
        DeploymentEvent.Event deploymentEvent = createDeploymentEvent(appInstance, environment);
        log.info("Ready to publish deployment-event to topic: %s:%s  %s. CorrelationId: %s",
                deploymentEvent.getApplication(),
                deploymentEvent.getVersion(),
                deploymentEvent.getSkyaEnvironment(),
                deploymentEvent.getCorrelationID());
        kafkaProducer.send(new ProducerRecord<>(kafkaDeploymentEventTopic, deploymentEvent), new KafkaCallback(deploymentEvent));
    }

    protected DeploymentEvent.Event createDeploymentEvent(ApplicationInstance appInstance, Environment environment) {
        final Cluster cluster = appInstance.getCluster();

        Instant timestamp = Instant.now();
        DeploymentEvent.Event.Builder deploymentEventBuilder = DeploymentEvent.Event.newBuilder()
                .setCorrelationID(generateUUID())
                .setApplication(appInstance.getApplication().getName())
                .setSkyaEnvironment(environment.getName())
                .setVersion(appInstance.getVersion())
                .setEnvironment(environment.getEnvClass() != EnvironmentClass.p ? DeploymentEvent.Environment.development : DeploymentEvent.Environment.production)
                .setSource(DeploymentEvent.System.aura)
                .setPlatform(buildPlatformType(cluster))
                .setRolloutStatus(DeploymentEvent.RolloutStatus.complete)
                .setTimestamp(Timestamp.newBuilder().setSeconds(timestamp.getEpochSecond()).setNanos(timestamp.getNano()).build());

        String updatedBy = EntityCommenter.getOnBehalfUserOrRealUser(appInstance);

        Optional.ofNullable(updatedBy).ifPresent(deployedBy ->
                deploymentEventBuilder.setDeployer(DeploymentEvent.Actor.newBuilder().setName(deployedBy).build()));

        return deploymentEventBuilder.build();
    }

    private String generateUUID() {
        return UUID.randomUUID().toString();
    }

    private DeploymentEvent.Platform buildPlatformType(Cluster cluster) {
        DeploymentEvent.Platform.Builder platformTypeBuilder = DeploymentEvent.Platform.newBuilder();

        if (cluster.getName().equalsIgnoreCase("nais")) {
            platformTypeBuilder.setType(DeploymentEvent.PlatformType.nais);
        } else {
            cluster.getNodes().stream().findFirst().ifPresent(node -> {

                switch (node.getPlatformType()) {
                    case JBOSS:
                        platformTypeBuilder.setType(DeploymentEvent.PlatformType.jboss);
                        platformTypeBuilder.setVariant("eap");
                        break;
                    case WILDFLY:
                        platformTypeBuilder.setType(DeploymentEvent.PlatformType.jboss);
                        platformTypeBuilder.setVariant("wildfly");
                        break;
                    case WAS:
                        platformTypeBuilder.setType(DeploymentEvent.PlatformType.was);
                        platformTypeBuilder.setVariant("was");
                        break;
                    case WAS9:
                        platformTypeBuilder.setType(DeploymentEvent.PlatformType.was);
                        platformTypeBuilder.setVariant("was9");
                        break;
                    case LIBERTY:
                        platformTypeBuilder.setType(DeploymentEvent.PlatformType.was);
                        platformTypeBuilder.setVariant("liberty");
                        break;
                    case BPM:
                        platformTypeBuilder.setType(DeploymentEvent.PlatformType.bpm);
                        platformTypeBuilder.setVariant("bpm");
                        break;
                    case BPM86:
                        platformTypeBuilder.setType(DeploymentEvent.PlatformType.bpm);
                        platformTypeBuilder.setVariant("bpm86");
                        break;
                }
            });
        }
        return platformTypeBuilder.build();
    }

    private String getProperty(String key) {
        return Optional
                .ofNullable(System.getProperty(key))
                .orElseThrow(() ->
                        new RuntimeException("Unable to connect Kafka client. Missing required system property: " + key));
    }

    class KafkaCallback implements Callback {
        private final DeploymentEvent.Event deploymentEvent;

        public KafkaCallback(DeploymentEvent.Event deploymentEvent) {
            this.deploymentEvent = deploymentEvent;
        }

        @Override
        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
            if (e != null) {
                log.error("Unable to publish deployment-event to topic " + recordMetadata != null ? recordMetadata.topic() : "", e);
            } else {
                log.info(String.format(
                        "Published deployment-event to topic: %s:%s  %s. CorrelationId: %s",
                        deploymentEvent.getApplication(),
                        deploymentEvent.getVersion(),
                        deploymentEvent.getSkyaEnvironment(),
                        deploymentEvent.getCorrelationID()));
            }
        }
    }
}