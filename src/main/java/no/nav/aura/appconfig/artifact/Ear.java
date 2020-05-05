package no.nav.aura.appconfig.artifact;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Ear extends Artifact {
    @XmlAttribute(required = false)
    private int startUpOrder = 10;
    @XmlAttribute(required = false)
    private String name;

    @XmlElementRef(name = "classLoader", required = false)
    private ClassLoader classLoader;

    @XmlElementRef(name = "classPathLibrary", required = false)
    private List<ClassPathLibrary> classPathLibraries = new ArrayList<>();

    public Ear() {
    }

    public Ear(String groupId, String artifactId, String version, String name, int startUpOrder) {
        setGroupId(groupId);
        setArtifactId(artifactId);
        setVersion(version);
        this.name = name;
        this.startUpOrder = startUpOrder;
    }

    public Ear(String groupId, String artifactId, String version, List<ClassPathLibrary> classPathLibraries, String name, int startUpOrder) {
        this(groupId, artifactId, version, name, startUpOrder);
        this.classPathLibraries = classPathLibraries;
    }

    public int getStartUpOrder() {
        return startUpOrder;
    }

    public void setStartUpOrder(int startUpOrder) {
        this.startUpOrder = startUpOrder;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public List<ClassPathLibrary> getClassPathLibraries() {
        return classPathLibraries;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
