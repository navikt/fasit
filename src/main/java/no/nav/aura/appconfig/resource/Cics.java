package no.nav.aura.appconfig.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

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
