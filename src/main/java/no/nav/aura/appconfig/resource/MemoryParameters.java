package no.nav.aura.appconfig.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

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
