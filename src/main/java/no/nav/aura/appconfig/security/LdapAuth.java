package no.nav.aura.appconfig.security;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import no.nav.aura.appconfig.Namespaces;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class LdapAuth extends Login {

    @XmlAttribute
    private String ldapResourceAlias = "ldap";
    @XmlAttribute
    private String authenticatedRole;
    @XmlAttribute
    private String lockToUser;
    @XmlAttribute()
    private String additionalBaseContext;
    @XmlElement(name = "additionalLdapContext", namespace = Namespaces.DEFAULT)
    private List<String> additionalLdapContexts = new ArrayList<>();

    public String getLdapResourceAlias() {
        return ldapResourceAlias;
    }

    public String getAdditionalBaseContext() { return additionalBaseContext; }

    public List<String> getAdditionalLdapContexts() { return additionalLdapContexts; }

    public String getLockToUser() {
        return lockToUser;
    }

    public String getAuthenticatedRole() {
        return authenticatedRole;
    }

}
