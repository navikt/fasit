package no.nav.aura.sensu;

import com.google.common.collect.ImmutableMap;

import no.nav.aura.sensu.SensuClient;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SensuClientTest {

    @Test
    public void createsValidSensuEvent() {
        final String sensuEvent = SensuClient.createSensuEvent("myevent", "blablabla");
        assertTrue(sensuEvent.contains("\"handlers\":[\"events\"]"), "contains correct handler");
        assertTrue(sensuEvent.contains("\"type\":\"metric\""), "type is set to metric");
    }

    @Test
    public void datapointWithoutTagsIsValid() {
        String line = SensuClient.toLineProtocol("measurement", null, ImmutableMap.<String, Object>of("value", 69));
        assertTrue(line.startsWith("measurement value=69"), "has no comma after measurement name");
    }

    @Test
    public void supportsMultipleFieldsAndTags() {
        ImmutableMap<String, Object> tags = ImmutableMap.<String, Object>of("tag1", "x", "tag2", "b");
        ImmutableMap<String, Object> fields = ImmutableMap.<String, Object>of("value", 69, "othervalue", "something", "banan", true);
        String line = SensuClient.toLineProtocol("measurement", tags, fields);
        assertTrue(line.startsWith("measurement,tag1=x,tag2=b value=69,othervalue=\\\"something\\\",banan=true"), "adheres to protocol with multiple fields and tags");
    }

    @Test
    public void stringFieldsAreEscapedProperly() {
        ImmutableMap<String, Object> fields = ImmutableMap.<String, Object>of("value", 69, "othervalue", "6.9.0", "banan", true);
        final String event = SensuClient.createSensuEvent("measurement", SensuClient.toLineProtocol("measurement", null, fields));
        assertTrue(event.contains("measurement value=69,othervalue=\\\"6.9.0\\\",banan=true"), "fields with type String must be escaped to survive transport");
    }

    @Test
    public void noFieldsInDatapointYieldsRuntimeException() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            SensuClient.toLineProtocol("measurement", ImmutableMap.<String, Object>of("x", "y"), null);
        });
    }
}