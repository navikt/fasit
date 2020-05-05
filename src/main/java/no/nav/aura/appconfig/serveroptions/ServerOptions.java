package no.nav.aura.appconfig.serveroptions;

import no.nav.aura.appconfig.JaxbPropertyHelper;
import no.nav.aura.appconfig.Namespaces;
import no.nav.aura.appconfig.jaxb.JaxbPropertySet;
import no.nav.aura.appconfig.resource.*;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
public class ServerOptions {

    @XmlElement(namespace = Namespaces.DEFAULT, name = "cron")
    private List<Cron> cron = new ArrayList<>();

    @XmlElement(namespace = Namespaces.DEFAULT, name = "memoryParameters")
    private MemoryParameters memoryParameters;

    @XmlElement(namespace = Namespaces.DEFAULT, name = "jvmArgs")
    private String jvmArgs;

    @XmlElement(namespace = Namespaces.DEFAULT, name = "deploymentScanner")
    private DeploymentScanner deploymentScanner;

    @XmlElement(namespace = Namespaces.DEFAULT, name = "timerManager")
    private Collection<TimerManager> timerManagers;

    @XmlElement(namespace = Namespaces.DEFAULT, name = "workManager")
    private Collection<WorkManager> workManagers;

    @XmlElement(namespace = Namespaces.DEFAULT, name = "schedulerConfiguration")
    private Collection<SchedulerConfiguration> schedulerConfigurations;

    @XmlElement(namespace = Namespaces.DEFAULT, name = "resourceEnvironmentProvider")
    private Collection<ResourceEnvironmentProviderConfiguration> resourceEnvironmentProvider;

    @XmlElement(namespace = Namespaces.DEFAULT, name = "webSockets")
    private WebSocket webSocketConfiguration;

    @XmlElement(namespace = Namespaces.DEFAULT, name = "replicationDomain")
    private Collection<ReplicationDomain> replicationDomains;

    @XmlElement(namespace = Namespaces.DEFAULT, name = "cacheInstance")
    private Collection<CacheInstance> cacheInstances;

    @XmlElement(namespace = Namespaces.DEFAULT, name = "webSettings")
    private WebSettings webSettings;

    @XmlElement(namespace = Namespaces.DEFAULT, name = "ejbSettings")
    private EjbSettings ejbSettings;

    @XmlElementRef
    private List<JaxbPropertySet> customProperties = new ArrayList<>();

    public List<Cron> getCronjobs() {
        return cron;
    }

    public MemoryParameters getMemoryParameters() {
        return memoryParameters;
    }

    public String getJvmArgs() {
        return jvmArgs;
    }

    public DeploymentScanner getDeploymentScanner() {
        return deploymentScanner;
    }

    public Collection<TimerManager> getTimerManagers() {
        return timerManagers;
    }

    public Collection<WorkManager> getWorkManagers() {
        return workManagers;
    }

    public Collection<SchedulerConfiguration> getSchedulerConfigurations() {
        return schedulerConfigurations;
    }

    public Collection<ResourceEnvironmentProviderConfiguration> getResourceEnvironmentProviderConfiguration() {
        return resourceEnvironmentProvider;
    }

    public WebSocket getWebSocketConfiguration() {
        return webSocketConfiguration;
    }

    public Collection<ReplicationDomain> getReplicationDomains() {
        return replicationDomains;
    }

    public Collection<CacheInstance> getCacheInstances() {
        return cacheInstances;
    }

    public WebSettings getWebSettings() {
        return webSettings;
    }

    public EjbSettings getEjbSettings() {
        return ejbSettings;
    }

    public List<JaxbPropertySet> getCustomProperties() {
        return customProperties;
    }

    public void setCron(List<Cron> cron) {
        this.cron = cron;
    }

    public void setMemoryParameters(MemoryParameters memoryParameters) {
        this.memoryParameters = memoryParameters;
    }

    public void setJvmArgs(String jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    public void setDeploymentScanner(DeploymentScanner deploymentScanner) {
        this.deploymentScanner = deploymentScanner;
    }

    public void setCustomProperties(List<JaxbPropertySet> customProperties) {
        this.customProperties = customProperties;
    }

    public Map<String, String> getCustomProperties(String parentObject) {
        return JaxbPropertyHelper.getCustomProperties(parentObject, customProperties);
    }
}
