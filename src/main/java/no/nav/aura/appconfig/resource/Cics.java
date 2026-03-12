package no.nav.aura.appconfig.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Cics extends AbstractPooledResource {

    public Cics() {
    }

    public Cics(String alias, String jndi) {
        setAlias(alias);
        setJndi(jndi);
    }
}
