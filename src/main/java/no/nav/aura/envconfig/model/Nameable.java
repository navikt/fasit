package no.nav.aura.envconfig.model;

import java.util.HashMap;
import java.util.Map;

public interface Nameable {

    String getName();

    default String getInfo() {
        return "";
    }

    default Map<String, Object> getEnityProperties() {
        return new HashMap<>();
    }
}
