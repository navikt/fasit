package no.nav.aura.appconfig.security;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import no.nav.aura.appconfig.Namespaces;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class OpenIdConnect extends Login {

    @XmlAttribute
    private String moduleClass;

    @XmlAttribute
    private String interceptedPathFilter;

    @XmlAttribute
    private Boolean mapIdentityToRegistryUser;

    @XmlElement(name = "contextRoot", namespace = Namespaces.DEFAULT)
    private List<String> contextRoots;

    public String getModuleClass() {
        return moduleClass;
    }

    public String getInterceptedPathFilter() {
        return interceptedPathFilter;
    }

    public Boolean getMapIdentityToRegistryUser() {
        return mapIdentityToRegistryUser;
    }

    public List<String> getContextRoots() {
        return contextRoots;
    }
}
