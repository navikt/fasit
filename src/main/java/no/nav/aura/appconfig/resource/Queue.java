package no.nav.aura.appconfig.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import no.nav.aura.appconfig.Namespaces;

@XmlRootElement(name = "queue")
@XmlAccessorType(XmlAccessType.FIELD)
public class Queue extends JmsDestination {

    @XmlElement(namespace = Namespaces.DEFAULT)
    private ListenerPort listenerPort;

    @XmlElement(namespace = Namespaces.DEFAULT)
    private ActivationSpec activationSpec;

    public Queue() {
    }

    public Queue(String alias) {
        setAlias(alias);
    }

    public ListenerPort getListenerPort() {
        return listenerPort;
    }

    public ActivationSpec getActivationSpec() {
        return activationSpec;
    }

}
