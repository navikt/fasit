package no.nav.aura.appconfig.security;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class SecurityMapping {

    @XmlAttribute
    private String resourceAlias;

    @XmlAttribute
    private String toRole;

    public String getResourceAlias() {
        return resourceAlias;
    }

    public void setResourceAlias(String resourceAlias) {
        this.resourceAlias = resourceAlias;
    }

    public String getToRole() {
        return toRole;
    }

    public void setToRole(String toRole) {
        this.toRole = toRole;
    }

}
