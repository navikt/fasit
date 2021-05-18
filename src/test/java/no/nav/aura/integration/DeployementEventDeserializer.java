package no.nav.aura.integration;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class DeployementEventDeserializer implements Deserializer {

    @Override
    public void configure(Map map, boolean b) {

    }

    @Override
    public com.google.protobuf.Any deserialize(String s, byte[] bytes) {
        try {
            return com.google.protobuf.Any.parseFrom(bytes);
        } catch (InvalidProtocolBufferException ipbe) {
            throw new RuntimeException("Error deserializing", ipbe);
        }
    }

    @Override
    public void close() {

    }
}
