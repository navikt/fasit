package no.nav.aura.appconfig.security;

import no.nav.aura.appconfig.Namespaces;

import javax.xml.bind.annotation.*;
import java.util.List;

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
