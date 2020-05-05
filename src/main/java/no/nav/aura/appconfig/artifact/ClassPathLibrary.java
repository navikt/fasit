package no.nav.aura.appconfig.artifact;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ClassPathLibrary extends Artifact {

    @XmlAttribute(required = false)
    private boolean requireClassifier;

    @XmlAttribute(required = false)
    private boolean unpack;

    @XmlAttribute(required = false)
    private boolean isolatedClassLoader;

    @XmlAttribute(required = false)
    private boolean setNativeLibraryPath;

    public ClassPathLibrary() {
    }

    public ClassPathLibrary(String groupId, String artifactId, String version, boolean requireClassifier, boolean unpack, boolean isolatedClassLoader, boolean setNativeLibraryPath) {
        setGroupId(groupId);
        setArtifactId(artifactId);
        setVersion(version);
        this.requireClassifier = requireClassifier;
        this.unpack = unpack;
        this.isolatedClassLoader = isolatedClassLoader;
        this.setNativeLibraryPath = setNativeLibraryPath;
    }

    public boolean requiresClassifier() {
        return requireClassifier;
    }

    public boolean unpack() {
        return unpack;
    }

    public boolean usesIsolatedClassLoader() {
        return isolatedClassLoader;
    }

    public boolean usesSetNativeLibraryPath() {
        return setNativeLibraryPath;
    }
}
