package no.nav.aura.envconfig.client;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.*;

/**
 * Simplifed resource representation for resource, preferring maps over classes like the more complex representation ResourceElement
 */
@XmlRootElement(name = "resource2")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class ResourceDO {

    private Map<String, String> properties;
    private String type;
    private String scope;
    private String alias;


    public ResourceDO(){
    };


    public ResourceDO(String type, String scope, String alias, Map<String, String> properties) {
        this.type = type;
        this.scope = scope;
        this.alias = alias;
        this.properties = properties;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
    public String getAlias() {
        return alias;
    }

    public String getScope() {
        return scope;
    }

    public String getType() {
        return type;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
