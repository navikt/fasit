package no.nav.aura.appconfig.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "cacheInstance")
@XmlAccessorType(XmlAccessType.FIELD)
public class CacheInstance extends AbstractJndiCapableResource {

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "defaultPriority", required = false)
    private int defaultPriority = 1;

    @XmlAttribute(name = "cacheSize", required = false)
    private int cacheSize = 2000;

    @XmlAttribute(name = "replicationDomain", required = false)
    private String replicationDomain;

    @XmlAttribute(name = "replicationType", required = false)
    private ReplicationType replicationType;

    @XmlAttribute(name = "pushFrequency", required = false)
    private int pushFrequency = 1;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDefaultPriority() {
        return defaultPriority;
    }

    public void setDefaultPriority(int defaultPriority) {
        this.defaultPriority = defaultPriority;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public String getReplicationDomain() {
        return replicationDomain;
    }

    public void setReplicationDomain(String replicationDomain) {
        this.replicationDomain = replicationDomain;
    }

    public ReplicationType getReplicationType() {
        return replicationType;
    }

    public void setReplicationType(ReplicationType replicationType) {
        this.replicationType = replicationType;
    }

    public int getPushFrequency() {
        return pushFrequency;
    }

    public void setPushFrequency(int pushFrequency) {
        this.pushFrequency = pushFrequency;
    }

    @XmlEnum
    public enum ReplicationType {
        NONE, PUSH_PULL, PUSH, PULL
    }
}
