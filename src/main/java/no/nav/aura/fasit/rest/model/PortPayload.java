package no.nav.aura.fasit.rest.model;

import javax.validation.constraints.NotNull;

public class PortPayload {

    public enum PortType {
        https, http, bootstrap
    }

    @NotNull(message = "port number is required")
    public Integer port;
    @NotNull(message = "port type is required")
    public PortType type;

    public PortPayload(int port, PortType type) {
        this.port = port;
        this.type = type;
    }

    @Override
    public String toString() {
        return type + ":" + port;
    }
    
    

}
