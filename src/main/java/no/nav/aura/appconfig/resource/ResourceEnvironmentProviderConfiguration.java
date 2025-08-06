package no.nav.aura.appconfig.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class ResourceEnvironmentProviderConfiguration {

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "isolatedClassLoader", required = false)
    private boolean isolatedClassLoader;

    @XmlAttribute(name = "refFactoryClassName")
    private String refFactoryClassName;

    @XmlAttribute(name = "refClassName")
    private String refClassName;

    @XmlAttribute(name = "resEnvEntryJndi")
    private String resEnvEntryJndi;

    @XmlAttribute(name = "resEnvEntryName")
    private String resEnvEntryName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean usesIsolatedClassLoader() {
        return isolatedClassLoader;
    }

    public String getRefFactoryClassName() {
        return refFactoryClassName;
    }

    public void setRefFactoryClassName(String refFactoryClassName) {
        this.refFactoryClassName = refFactoryClassName;
    }

    public String getRefClassName() {
        return refClassName;
    }

    public void setRefClassName(String refClassName) {
        this.refClassName = refClassName;
    }

    public String getResEnvEntryJndi() {
        return resEnvEntryJndi;
    }

    public void setResEnvEntryJndi(String resEnvEntryJndi) {
        this.resEnvEntryJndi = resEnvEntryJndi;
    }

    public String getResEnvEntryName() {
        return resEnvEntryName;
    }

    public void setResEnvEntryName(String resEnvEntryName) {
        this.resEnvEntryName = resEnvEntryName;
    }

}
