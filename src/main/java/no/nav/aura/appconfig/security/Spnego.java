package no.nav.aura.appconfig.security;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Spnego extends Login {

    @XmlAttribute
    private String ldapResourceAlias = "ldap";
    @XmlAttribute
    private String fallbackLoginPagePath;

    public String getLdapResourceAlias() {
        return ldapResourceAlias;
    }

    public String getFallbackLoginPagePath() {
        return fallbackLoginPagePath;
    }
}
