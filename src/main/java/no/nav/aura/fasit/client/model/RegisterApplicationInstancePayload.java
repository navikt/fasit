package no.nav.aura.fasit.client.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import no.nav.aura.envconfig.client.rest.ResourceElement;

import com.google.gson.Gson;

public class RegisterApplicationInstancePayload {
    private String application;
    private String version;
    private String environment;
    private String selftest;
    private List<String> nodes = new ArrayList<>();
    private Set<UsedResource> usedResources = new HashSet<>();
    private List<ExposedResource> exposedResources = new ArrayList<>();
    private List<MissingResource> missingResources = new ArrayList<>();
    private AppConfig appConfig;

    public RegisterApplicationInstancePayload() {
    }

    public RegisterApplicationInstancePayload(String application, String version, String environment) {
        this.application = application;
        this.version = version;
        this.environment = environment;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getSelftest() {
        return selftest;
    }

    public void setSelftest(String selftest) {
        this.selftest = selftest;
    }

    public List<String> getNodes() {
        return nodes;
    }

    public void setNodes(List<String> nodes) {
        this.nodes = nodes;
    }

    public void setNodes(String... nodes) {
        this.nodes = Arrays.asList(nodes);
    }

    public List<ExposedResource> getExposedResources() {
        return exposedResources;
    }

    public void setExposedResources(List<ExposedResource> exposedResources) {
        this.exposedResources = exposedResources;
    }


    public String toJson() {
        return new Gson().toJson(this);
    }

    public Set<UsedResource> getUsedResources() {
        return usedResources;
    }

    public void setUsedResources(Set<UsedResource> usedResources) {
        this.usedResources = usedResources;
    }

    public List<MissingResource> getMissingResources() {
        return missingResources;
    }

    public void setMissingResources(List<MissingResource> missingResources) {
        this.missingResources = missingResources;
    }

    public void addUsedResources(UsedResource... usedResources) {
        for (UsedResource usedResource : usedResources) {
            this.usedResources.add(usedResource);
        }
    }

    public void addUsedResources(Set<ResourceElement> usedResources) {
        for (ResourceElement resourceElement : usedResources) {
            this.usedResources.add(new UsedResource(resourceElement.getId(), resourceElement.getRevision()));
        }
    }

    public AppConfig getAppConfig() {
        return appConfig;
    }

    public void setAppConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }
}
