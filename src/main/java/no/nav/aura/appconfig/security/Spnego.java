package no.nav.aura.appconfig.security;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

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
