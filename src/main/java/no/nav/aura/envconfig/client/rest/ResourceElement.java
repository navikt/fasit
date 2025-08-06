package no.nav.aura.envconfig.client.rest;

import no.nav.aura.envconfig.client.ApplicationInstanceDO;
import no.nav.aura.envconfig.client.DomainDO;
import no.nav.aura.envconfig.client.LifeCycleStatusDO;
import no.nav.aura.envconfig.client.ResourceTypeDO;
import no.nav.aura.envconfig.client.rest.PropertyElement.Type;

import jakarta.xml.bind.annotation.*;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@XmlRootElement(name = "resource")
@XmlAccessorType(XmlAccessType.FIELD)
public class ResourceElement {

    private URI ref;
    private Long id;
    private ResourceTypeDO type;
    private String alias;
    private String environmentClass;
    private DomainDO domain;
    private String environmentName;
    @XmlElement(name = "scope.application")
    private String application;
    @XmlElement(name = "property")
    private Set<PropertyElement> properties = new HashSet<>();
    private LifeCycleStatusDO lifeCycleStatus;
    @XmlElementWrapper(name = "usedInApplications")
    private List<ApplicationInstanceDO> usedInApplication;
    private boolean dodgy;
    private Long revision;
    private String accessAdGroup;

    public ResourceElement() {
    }

    public ResourceElement(ResourceTypeDO type, String alias) {
        this.type = type;
        this.alias = alias;
    }

    public ResourceElement(String type, String alias) {
        this.type = ResourceTypeDO.valueOf(type);
        this.alias = alias;
    }

    public void addProperty(PropertyElement property) {
        properties.add(property);
    }

    public String getAlias() {
        return alias;
    }

    public ResourceTypeDO getType() {
        return type;
    }

    public void setType(ResourceTypeDO type) {
        this.type = type;
    }

    public String getEnvironmentClass() {
        return environmentClass;
    }

    public void setEnvironmentClass(String environmentClass) {
        this.environmentClass = environmentClass;
    }

    public DomainDO getDomain() {
        return domain;
    }

    public void setDomain(DomainDO domain) {
        this.domain = domain;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getApplication() {
        return application;
    }

    public Set<PropertyElement> getProperties() {
        return properties;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setRef(URI ref) {
        this.ref = ref;
    }

    public URI getRef() {
        return ref;
    }

    public void setLifeCycleStatus(LifeCycleStatusDO lifeCycleStatusDO) {
        this.lifeCycleStatus = lifeCycleStatusDO;
    }

    public void setUsedInApplication(List<ApplicationInstanceDO> usedInApplications) {
        this.usedInApplication = usedInApplications;
    }

    public boolean isDodgy() { return dodgy; }

    public void setDodgy(boolean dodgy) { this.dodgy = dodgy; }

    public List<ApplicationInstanceDO> getUsedInApplication() {
        return usedInApplication;
    }

    public LifeCycleStatusDO getLifeCycleStatus() {
        return lifeCycleStatus;
    }

    public String getPropertyString(String key) {
        PropertyElement property = getPropertyElement(key);
        if (property.getType().equals(Type.STRING)) {
            return property.getValue();
        }
        return null;
    }

    public URI getPropertyUri(String key) {
        PropertyElement property = getPropertyElement(key);
        if (!property.getType().equals(Type.STRING)) {
            return property.getRef();
        }
        return null;
    }

    private PropertyElement getPropertyElement(String key) {
        for (PropertyElement property : properties) {
            if (property.getName().equals(key)) {
                return property;
            }
        }
        throw new IllegalArgumentException("unknown property " + key);
    }

    public void setRevision(Long revision) {
        this.revision = revision;
    }

    public Long getRevision() {
        return revision;
    }

    public String getAccessAdGroup() {
        return accessAdGroup;
    }

    public void setAccessAdGroup(String accessAdGroup) {
        this.accessAdGroup = accessAdGroup;
    }
}
