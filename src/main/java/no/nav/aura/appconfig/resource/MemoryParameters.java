package no.nav.aura.appconfig.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class MemoryParameters {

    @XmlAttribute
    private String resourceAlias;

    public String getResourceAlias() {
        return resourceAlias;
    }

    public void setResourceAlias(String resourceAlias) {
        this.resourceAlias = resourceAlias;
    }
}
