package no.nav.aura.appconfig.security;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
