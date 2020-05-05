package no.nav.aura.integration;


import no.nav.protos.deployment.DeploymentEvent;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class DeploymentEventSerializer implements Serializer<DeploymentEvent.Event> {

    @Override
    public void configure(Map<String, ?> map, boolean b) {
        
    }

    @Override
    public byte[] serialize(String s, DeploymentEvent.Event event) {
        return event.toByteArray();
    }



    @Override
    public void close() {

    }
}
