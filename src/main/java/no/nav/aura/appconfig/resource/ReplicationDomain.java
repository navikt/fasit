package no.nav.aura.appconfig.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "replicationDomain")
@XmlAccessorType(XmlAccessType.FIELD)
public class ReplicationDomain {

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "requestTimeout", required = false)
    private int requestTimeout = 5;

    @XmlAttribute(name = "numberOfReplicas", required = false)
    private int numberOfReplicas = -1;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public int getNumberOfReplicas() {
        return numberOfReplicas;
    }

    public void setNumberOfReplicas(int numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }
}
