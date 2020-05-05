package no.nav.aura.appconfig.security;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.*;

import no.nav.aura.appconfig.Namespaces;

@XmlAccessorType(XmlAccessType.FIELD)
public class Security {

    @XmlElementWrapper(namespace = Namespaces.DEFAULT)
    @XmlElementRef
    private List<Login> logins = new ArrayList<>();

    @XmlElement(namespace = Namespaces.DEFAULT, name = "roleMapping")
    private List<RoleMapping> roleMappings = new ArrayList<>();

    @XmlElement(namespace = Namespaces.DEFAULT, name = "runAs")
    private List<RunAsMapping> runAsMappings = new ArrayList<>();

    /** reference to the credentials and applicationcertificate in envconfig for the service user from AD for this application */
    @XmlAttribute
    private String serviceUserResourceAlias;

    public List<Login> getLogins() {
        return logins;
    }

    public void setLogins(List<Login> logins) {
        this.logins = logins;
    }

    public List<RoleMapping> getRoleMappings() {
        return roleMappings;
    }

    public void setRoleMappings(List<RoleMapping> roleMappings) {
        this.roleMappings = roleMappings;
    }

    @SuppressWarnings("unchecked")
    public <T extends Login> T getLogin(Class<T> type) {
        for (Login login : logins) {
            if (type.isInstance(login)) {
                return (T) login;
            }
        }

        return null;
    }

    public String getServiceUserResourceAlias() {
        return serviceUserResourceAlias;
    }

    public void setServiceUserResourceAlias(String serviceUser) {
        this.serviceUserResourceAlias = serviceUser;
    }

    public List<RunAsMapping> getRunAsMappings() {
        return runAsMappings;
    }
}
