package no.nav.aura.envconfig.client;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import jakarta.xml.bind.annotation.*;

import no.nav.aura.appconfig.Application;
import no.nav.aura.envconfig.client.rest.ResourceElement;

@Deprecated
@XmlRootElement(name = "deployedApplication")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class DeployedApplicationDO {

    private String version;
    private Application appconfig;
    @XmlElementWrapper
    @XmlElementRef
    private Set<ResourceElement> usedResources = new HashSet<ResourceElement>();

    DeployedApplicationDO() {

    }

    public DeployedApplicationDO(Application appconfig, String version) {
        this.appconfig = appconfig;
        this.version = version;
    }

    public Application getAppconfig() {
        return appconfig;
    }

    public Set<ResourceElement> getUsedResources() {
        return usedResources;
    }

    public void addUsedResources(ResourceElement... resources) {
        for (ResourceElement resourceElement : resources) {
            this.usedResources.add(resourceElement);
        }

    }

    public void addUsedResources(Collection<ResourceElement> findResources) {
        addUsedResources(findResources.toArray(new ResourceElement[0]));

    }

    public void setAppconfig(Application appconfig) {
        this.appconfig = appconfig;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
