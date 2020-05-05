package no.nav.aura.appconfig.resource;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import no.nav.aura.appconfig.Namespaces;

public class AbstractPooledResource extends AbstractJndiCapableResource {

    @XmlAttribute(required = false)
    private boolean xaEnabled = false;

    @XmlElement(namespace = Namespaces.DEFAULT)
    private ConnectionPool pool = new ConnectionPool();

    public boolean isXaEnabled() {
        return xaEnabled;
    }

    public void setXaEnabled(boolean xaEnabled) {
        this.xaEnabled = xaEnabled;
    }

    public ConnectionPool getPool() {
        return pool;
    }

    public void setPool(ConnectionPool pool) {
        this.pool = pool;
    }
}
