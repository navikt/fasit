package no.nav.aura.integration;

import com.google.protobuf.Timestamp;
import no.nav.aura.envconfig.auditing.EntityCommenter;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.protos.deployment.DeploymentEvent;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.config.SaslConfigs;
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

    // Feature toggle for kafka integration
    private final boolean kafkaEnabled = System.getProperty("publish.deployment.events.to.kafka") != null &&
            System.getProperty("publish.deployment.events.to.kafka").equalsIgnoreCase("true");


    public FasitKafkaProducer(){
        if(kafkaEnabled) {
            final String kafkaServers = getProperty("kafka.servers");
            final String kafkaUsername = getProperty("kafka.username");
            final String kafkaPassword = getProperty("kafka.password");
            final boolean saslEnabled = getProperty("kafka.sasl.enabled").equalsIgnoreCase("true");
            kafkaDeploymentEventTopic =  getProperty("kafka.deployment.event.topic");

            Properties prop = new Properties();

            prop.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
            prop.put(ProducerConfig.CLIENT_ID_CONFIG, "fasit");
            prop.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "20000");
            prop.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "https");
            prop.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, "500");


            if(saslEnabled) {
                prop.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
                prop.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
                prop.put(
                        SaslConfigs.SASL_JAAS_CONFIG,
                        String.format("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                                kafkaUsername, kafkaPassword));
            }

            try {
                kafkaProducer = new KafkaProducer<>(prop, new StringSerializer(),
                        new DeploymentEventSerializer());
                log.info("Kafka client connected to " + kafkaServers);
            } catch (KafkaException ke) {
                throw new RuntimeException("Unable to connect to kafka " + kafkaServers, ke);
            }

        }
        else {
            log.info("Kafka integration is disabled. Will skip configuration of Kafka producer");
        }
    }

    public FasitKafkaProducer(KafkaProducer producer) {
        kafkaProducer = producer;
    }

    public void publishDeploymentEvent(ApplicationInstance appInstance, Environment environment) {
        // Feature toggle for kafka integration
        // We do not want to send deployment events to kafka if the app was deployed with naisd. That is because naisd posts its own deployment events to kafka
        if (kafkaEnabled && !isNaisDeployment(appInstance)) {
            DeploymentEvent.Event deploymentEvent = createDeploymentEvent(appInstance, environment);
            log.info("Ready to publish deployment-event to topic: %s:%s  %s. CorrelationId: %s",
                    deploymentEvent.getApplication(),
                    deploymentEvent.getVersion(),
                    deploymentEvent.getSkyaEnvironment(),
                    deploymentEvent.getCorrelationID());
            kafkaProducer.send(new ProducerRecord<>(kafkaDeploymentEventTopic, deploymentEvent), new KafkaCallback(deploymentEvent));
        }
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

    private boolean isNaisDeployment(ApplicationInstance applicationInstance) {
        Cluster cluster = applicationInstance.getCluster();
        return cluster.getName().equalsIgnoreCase("nais");
    }

    private String getProperty(String key ){
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
            }
            else {
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