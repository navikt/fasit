package no.nav.aura.appconfig.monitoring;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "selftest")
@XmlAccessorType(XmlAccessType.FIELD)
public class SelftestMonitoring {

    @XmlAttribute
    private Integer interval = 60;

    public Integer getInterval() {
        return interval;
    }
}
