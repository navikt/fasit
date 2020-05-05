package no.nav.aura.appconfig.monitoring;

import javax.xml.bind.annotation.XmlAttribute;

public class Metric {

    @XmlAttribute
    private String path;

    @XmlAttribute
    private Integer interval = 60;

    public String getPath() {
        return path;
    }

    public Integer getInterval() {
        return interval;
    }
}
