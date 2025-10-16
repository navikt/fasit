package no.nav.aura.appconfig.resource;

import jakarta.xml.bind.annotation.*;

/**
 * @author v137023
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({Queue.class})
public abstract class JmsDestination extends AbstractJndiCapableResource {

    @XmlEnum
    public enum TargetClient {
        JMS, MQ;
    }
    
    public JmsDestination() {
    }
    
    @XmlAttribute(required = false)
    private TargetClient targetClient = TargetClient.JMS;
    
    
    

    public TargetClient getTargetClient() {
        return targetClient;
    }

    public void setTargetClient(TargetClient targetClient) {
        this.targetClient = targetClient;
    }
}

