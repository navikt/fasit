package no.nav.aura.appconfig.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class WebSettings {

    @XmlAttribute(name = "maxPostSize", required = false)
    private long maxPostSize = 10000000;

    public long getMaxPostSize() {
        return maxPostSize;
    }

    public void setWebSocketEnabled(long maxPostSize) {
        this.maxPostSize = maxPostSize;
    }
}
