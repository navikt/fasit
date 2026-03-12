package no.nav.aura.envconfig.client;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@XmlRootElement(name = "environment")
@XmlAccessorType(XmlAccessType.FIELD)
public class EnvironmentDO {
    @XmlAttribute
    private URI ref;
    private String name;
    private String envClass;
    private URI applicationsRef;

    public EnvironmentDO() {
    }

    public EnvironmentDO(String name, String envClass, URI baseUri) {
        this.name = name;
        this.envClass = envClass;
        ref = UriComponentsBuilder.fromUri(baseUri)
                .path("/conf/environments/{env}")
                .buildAndExpand(name)
                .toUri();
                
        applicationsRef = UriComponentsBuilder.fromUri(baseUri)
                .path("/conf/environments/{env}/applications")
                .buildAndExpand(name)
                .toUri();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnvClass() {
        return envClass;
    }

    public void setEnvClass(String envClass) {
        this.envClass = envClass;
    }

    public URI getRef() {
        return ref;
    }

    public URI getApplicationsRef() {
        return applicationsRef;
    }
}
