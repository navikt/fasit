package no.nav.aura.appconfig.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

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
