package no.nav.aura.integration;

import com.google.protobuf.InvalidProtocolBufferException;
import no.nav.protos.deployment.DeploymentEvent;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class DeployementEventDeserializer  implements Deserializer {

    @Override
    public void configure(Map map, boolean b) {

    }

    @Override
    public DeploymentEvent.Event deserialize(String s, byte[] bytes) {
        try {
            return DeploymentEvent.Event.parseFrom(bytes);
        } catch (InvalidProtocolBufferException ipbe) {
            throw new RuntimeException("Error deserializoing", ipbe);
        }
    }

    @Override
    public void close() {

    }
}
