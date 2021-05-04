package no.nav.aura.integration;

import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.Cluster;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.Node;
import no.nav.aura.envconfig.model.infrastructure.PlatformType;
import no.nav.protos.deployment.DeploymentEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import static no.nav.protos.deployment.DeploymentEvent.Event;
import static no.nav.protos.deployment.DeploymentEvent.RolloutStatus;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class FasitKafkaProducerTest {

    // Need to set these in order to create nodes with password
    static {
        System.setProperty("fasit.encryptionkeys.username", "junit");
        System.setProperty("fasit.encryptionkeys.password", "password");
    }

    // This test is run on-demand to check Kafka connectivity using TLS auth.
    // Extract credentials from Kafkarator secrets, then change the test parameters to get it working.
    @Ignore
    @Test
    public void tlsConnectionIntegrationTest() {
        System.setProperty("kafka.servers", "10.6.3.18:26484");
        System.setProperty("kafka.credstore.password", "changeme");
        System.setProperty("kafka.keystore.path", "/tmp/client.keystore.p12");
        System.setProperty("kafka.truststore.path", "/tmp/client.truststore.jks");
        System.setProperty("kafka.deployment.event.topic", "aura.dev-rapid");

        Cluster cluster = new Cluster("myCluster", Domain.TestLocal);
        ApplicationInstance appInstance = new ApplicationInstance(new Application("myApp"), cluster);
        appInstance.setVersion("1.1");
        appInstance.setUpdatedBy("mr. deployer");

        Environment environment = new Environment("test", EnvironmentClass.t);
        FasitKafkaProducer fasitKafkaProducer = new FasitKafkaProducer();
        fasitKafkaProducer.publishDeploymentEvent(appInstance, environment);
    }

    @Test
    public void basicGenerationOfDeploymentEventWithEmptyClusterWorks() {

        Cluster cluster = new Cluster("myCluster", Domain.TestLocal);
        ApplicationInstance appInstance = new ApplicationInstance(new Application("myApp"), cluster);
        appInstance.setVersion("1.1");
        appInstance.setUpdatedBy("mr. deployer");
        Event deploymentEvent = createDeploymentEvent(EnvironmentClass.t, appInstance);

        assertThat("myApp", is(deploymentEvent.getApplication()));
        assertThat("1.1", is(deploymentEvent.getVersion()));
        assertThat("mr. deployer", is(deploymentEvent.getDeployer().getName()));
        assertThat("myenv", is(deploymentEvent.getSkyaEnvironment()));
        assertThat(DeploymentEvent.Environment.development, is(deploymentEvent.getEnvironment()));
        assertThat(DeploymentEvent.System.aura, is(deploymentEvent.getSource()));
        assertThat(RolloutStatus.complete, is(deploymentEvent.getRolloutStatus()));
    }

    @Test
    public void deploymentEventWithJbossClusterSetsCorrectPlatformType() {
        ApplicationInstance appInstance = createAppInstanceWithCluster("myCluster", Domain.Devillo, EnvironmentClass.u, PlatformType.JBOSS);
        Event deploymentEvent = createDeploymentEvent(EnvironmentClass.u, appInstance);

        assertThat(deploymentEvent.getPlatform().getType(), is(DeploymentEvent.PlatformType.jboss));
        assertThat(deploymentEvent.getPlatform().getVariant(), is("eap"));
    }

    @Test
    public void deploymentEventWithWas9ClusterSetsCorrectPlatformType() {
        ApplicationInstance appInstance = createAppInstanceWithCluster("myCluster", Domain.Adeo, EnvironmentClass.p, PlatformType.WAS9);
        Event deploymentEvent = createDeploymentEvent(EnvironmentClass.u, appInstance);

        assertThat(deploymentEvent.getPlatform().getType(), is(DeploymentEvent.PlatformType.was));
        assertThat(deploymentEvent.getPlatform().getVariant(), is("was9"));
    }

    @Test
    public void deploymentEventWithBpm86ClusterSetsCorrectPlatformType() {
        ApplicationInstance appInstance = createAppInstanceWithCluster("myCluster", Domain.TestLocal, EnvironmentClass.t, PlatformType.BPM86);
        Event deploymentEvent = createDeploymentEvent(EnvironmentClass.t, appInstance);

        assertThat(deploymentEvent.getPlatform().getType(), is(DeploymentEvent.PlatformType.bpm));
        assertThat(deploymentEvent.getEnvironment(), is(DeploymentEvent.Environment.development));
        assertThat(deploymentEvent.getPlatform().getVariant(), is("bpm86"));
    }

    @Test
    public void deploymentEventWithLibertyClusterSetsCorrectPlatformType() {
        ApplicationInstance appInstance = createAppInstanceWithCluster("myCluster", Domain.TestLocal, EnvironmentClass.t, PlatformType.LIBERTY);
        Event deploymentEvent = createDeploymentEvent(EnvironmentClass.t, appInstance);

        assertThat(deploymentEvent.getPlatform().getType(), is(DeploymentEvent.PlatformType.was));
        assertThat(deploymentEvent.getPlatform().getVariant(), is("liberty"));
    }

    @Test
    public void deploymentEventWithWildflyClusterSetsCorrectPlatformType() {
        ApplicationInstance appInstance = createAppInstanceWithCluster("myCluster", Domain.TestLocal, EnvironmentClass.t, PlatformType.WILDFLY);
        Event deploymentEvent = createDeploymentEvent(EnvironmentClass.t, appInstance);

        assertThat(deploymentEvent.getPlatform().getType(), is(DeploymentEvent.PlatformType.jboss));
        assertThat(deploymentEvent.getEnvironment(), is(DeploymentEvent.Environment.development));
        assertThat(deploymentEvent.getPlatform().getVariant(), is("wildfly"));
    }

    private Event createNaisDeployment(Domain domain, EnvironmentClass envClass) {
        Cluster cluster = new Cluster("nais", domain);
        ApplicationInstance appInstance = new ApplicationInstance(new Application("myApp"), cluster);
        appInstance.setVersion("1.1");

        return createDeploymentEvent(envClass, appInstance);
    }


    private Event createDeploymentEvent(EnvironmentClass envClass, ApplicationInstance appInstance) {
        KafkaProducer kafkaProducer = mock(KafkaProducer.class);
        FasitKafkaProducer fasitKafkaProducer = new FasitKafkaProducer(kafkaProducer);
        Environment environment = new Environment("myenv", envClass);
        return fasitKafkaProducer.createDeploymentEvent(
                appInstance,
                environment
        );
    }


    private ApplicationInstance createAppInstanceWithCluster(String clusterName, Domain domain, EnvironmentClass environmentClass, PlatformType platformType) {
        Cluster cluster = new Cluster(clusterName, domain);

        Node node = new Node("hostname.com", "user", "password", environmentClass, platformType);
        cluster.addNode(node);

        ApplicationInstance appInstance = new ApplicationInstance(new Application("myApp"), cluster);
        appInstance.setVersion("1.1");

        return appInstance;
    }

    @Test
    public void serializationAndDeserializationToAndFromProtobufWorks() {
        ApplicationInstance appInstance = createAppInstanceWithCluster("myCluster", Domain.TestLocal, EnvironmentClass.t, PlatformType.WILDFLY);
        Event deploymentEvent = createDeploymentEvent(EnvironmentClass.t, appInstance);

        DeploymentEventSerializer serializer = new DeploymentEventSerializer();
        DeployementEventDeserializer deserializer = new DeployementEventDeserializer();

        byte[] serialized = serializer.serialize("topic", deploymentEvent);
        Event deserialized = deserializer.deserialize("topic", serialized);

        assertEquals(deserialized, deploymentEvent);
    }
}