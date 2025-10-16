package no.nav.aura.appconfig.monitoring;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "selftest")
@XmlAccessorType(XmlAccessType.FIELD)
public class SelftestMonitoring {

    @XmlAttribute
    private Integer interval = 60;

    public Integer getInterval() {
        return interval;
    }
}
