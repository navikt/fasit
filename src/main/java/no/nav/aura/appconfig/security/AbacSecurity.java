package no.nav.aura.appconfig.security;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class AbacSecurity {

    /** reference to the credentials in fasit for the service user from AD for this application */
    @XmlAttribute
    private String serviceUserResourceAlias;

    public String getServiceUserResourceAlias() {
        return serviceUserResourceAlias;
    }

    public void setServiceUserResourceAlias(String serviceUser) {
        this.serviceUserResourceAlias = serviceUser;
    }
}
