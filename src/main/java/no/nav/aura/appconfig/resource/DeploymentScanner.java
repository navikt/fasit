package no.nav.aura.appconfig.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class DeploymentScanner {

    @XmlAttribute(name = "scanEnabled", required = false)
    private boolean scanEnabled = true;

    @XmlAttribute(name = "scanInterval", required = false)
    private int scanInterval = 5000;

    public boolean getScanEnabled() {
        return scanEnabled;
    }

    public void setScanEnabled(boolean scanEnabled) {
        this.scanEnabled = scanEnabled;
    }

    public int getScanInterval() {
        return scanInterval;
    }

    public void setScanInterval(int scanInterval) {
        this.scanInterval = scanInterval;
    }
}
