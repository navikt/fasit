package no.nav.aura.appconfig.security;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import no.nav.aura.appconfig.Namespaces;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class OpenAm extends Login {

    @XmlElement(name = "contextRoot", namespace = Namespaces.DEFAULT)
    private List<String> contextRoots = new ArrayList<>();

    public List<String> getContextRoots() {
        return contextRoots;
    }

    public void setContextRoots(List<String> contextRoots) {
        this.contextRoots = contextRoots;
    }

}
