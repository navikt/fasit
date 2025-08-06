package no.nav.aura.appconfig.resource;

import no.nav.aura.appconfig.Namespaces;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static no.nav.aura.appconfig.Filter.filter;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class QueueManager extends AbstractPooledResource {

    @XmlAttribute(name = "enableSSL", required = false)
    private boolean sslEnabled = false;

    @XmlAttribute(name = "cipherSuite", required = false)
    private String cipherSuite = "TLS_RSA_WITH_AES_128_CBC_SHA";

    @XmlAttribute(name = "unifiedConnectionFactory", required = false)
    private boolean unifiedConnectionFactory = false;

    @XmlAttribute(name = "bindToMdb", required = false)
    private boolean bindToMdb = false;

    @XmlElement(namespace = Namespaces.DEFAULT)
    private ConnectionPool sessionPool = new ConnectionPool();

    @XmlElement(namespace = Namespaces.DEFAULT)
    private Credential credential;

    @XmlElement(namespace = Namespaces.DEFAULT)
    private Channel channel;

    @XmlElementRefs({ @XmlElementRef(name = "queue")})
    private List<JmsDestination> jmsDestinations = new ArrayList<>();

    public ConnectionPool getSessionPool() {
        return sessionPool;
    }

    public void setSessionPool(ConnectionPool sessionPool) {
        this.sessionPool = sessionPool;
    }

    public Collection<Queue> getQueues() {
        return filter(jmsDestinations, Queue.class);
    }

    public void add(JmsDestination jmsDestination) {
        jmsDestinations.add(jmsDestination);
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    public String getCipherSuite() {
        return cipherSuite;
    }

    public void setCipherSuite(String cipherSuite) {
        this.cipherSuite = cipherSuite;
    }

    public boolean isUnifiedConnectionFactory() {
        return unifiedConnectionFactory;
    }

    public void setUnifiedConnectionFactory(boolean unifiedConnectionFactory) {
        this.unifiedConnectionFactory = unifiedConnectionFactory;
    }

    public boolean isBindToMdbEnabled() {
        return bindToMdb;
    }

    public void setBindToMdbEnabled(boolean bindToMdb) {
        this.bindToMdb = bindToMdb;
    }

    public List<JmsDestination> getJmsDestinations() {
        return jmsDestinations;
    }

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
