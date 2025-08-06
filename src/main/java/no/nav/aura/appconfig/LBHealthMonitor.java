package no.nav.aura.appconfig;

import jakarta.xml.bind.annotation.XmlAttribute;

public class LBHealthMonitor {
    @XmlAttribute(required = true)
    private HealthMonitorType type = HealthMonitorType.HTTPS;
    @XmlAttribute
    private String isAliveUrl;
    @XmlAttribute
    private int interval = 5;
    @XmlAttribute
    private int timeout = 16;

    public HealthMonitorType getType() {
        return type;
    }

    public String getIsAliveUrl() {
        return isAliveUrl;
    }

    public int getInterval() {
        return interval;
    }

    public int getTimeout() {
        return timeout;
    }

    public enum HealthMonitorType {
        HTTPS, HTTP, TCP_HALF_OPEN
    }
}
