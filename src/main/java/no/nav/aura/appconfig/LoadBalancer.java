package no.nav.aura.appconfig;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class LoadBalancer {

    @XmlElement(name = "lbHealthMonitor", namespace = Namespaces.DEFAULT)
    private LBHealthMonitor healthMonitor;

    @XmlAttribute
    private String alias;

    @XmlAttribute
    private String isAlive;

    @XmlAttribute
    private Integer connectionLimit;

    @Deprecated
    @XmlElement(name = "contextRoot", namespace = Namespaces.DEFAULT)
    private List<String> contextRoots;

    public LBHealthMonitor getHealthMonitor() {
        return healthMonitor;
    }

    public String getAlias() {
        return alias;
    }

    public String getIsAlive() {
        return isAlive;
    }

    public void setHealthMonitor(LBHealthMonitor healthMonitor) {
        this.healthMonitor = healthMonitor;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public List<String> getContextRoots() {
        return contextRoots;
    }

    public Integer getConnectionLimit() {
        return connectionLimit;
    }

    public void setConnectionLimit(Integer connectionLimit) {
        this.connectionLimit = connectionLimit;
    }


}
