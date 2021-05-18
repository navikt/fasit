package no.nav.aura.integration;


import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class DeploymentEventSerializer implements Serializer<com.google.protobuf.Any> {

    @Override
    public void configure(Map<String, ?> map, boolean b) {

    }

    @Override
    public byte[] serialize(String s, com.google.protobuf.Any event) {
        return event.toByteArray();
    }


    @Override
    public void close() {

    }
}
