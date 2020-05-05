package no.nav.aura.appconfig.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class WebSocket {

    @XmlAttribute(name = "enable", required = false)
    private boolean enabled = false;

    public boolean getWebSocketEnabled() {
        return enabled;
    }

    public void setWebSocketEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
