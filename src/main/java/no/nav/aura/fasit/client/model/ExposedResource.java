package no.nav.aura.fasit.client.model;

import java.util.HashMap;
import java.util.Map;

import no.nav.aura.envconfig.client.ResourceTypeDO;

public class ExposedResource {
    private Long id;
    private String type;
    private String alias;
    private String domain;
    private String accessAdGroups;
    private Map<String, String> properties;

    public ExposedResource() {
    }
    
    public ExposedResource(ResourceTypeDO type, String alias, Map<String, String> properties) {
        this(type.name(), alias, properties);
    }

    public ExposedResource(String type, String alias, Map<String, String> properties) {
        this(type,alias,null, properties);
    }
    
    public ExposedResource(String type, String alias, Long id, Map<String, String> properties ) {
        this.type = type;
        this.alias = alias;
        this.properties = properties;
        this.id=id;
    }

    public String getType() {
        return type;
    }

    public String getAlias() {
        return alias;
    }

    public Map<String, String> getProperties() {
        if(properties!= null){
        return properties;
        }
        return new HashMap<>();
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }

    @Override
    public String toString() {
        StringBuffer text= new StringBuffer("alias=" + this.alias + ", type=" + this.type + ", properties=" + getProperties());
        if(id != null){
            text.append(", id=" + id);
        }
        return text.toString();
    }

    public String getAccessAdGroups() {
        return accessAdGroups;
    }

    public void setAccessAdGroups(String accessAdGroups) {
        this.accessAdGroups = accessAdGroups;
    }

    public Long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

}