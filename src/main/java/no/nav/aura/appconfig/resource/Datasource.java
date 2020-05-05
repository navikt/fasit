package no.nav.aura.appconfig.resource;

import no.nav.aura.appconfig.Namespaces;

import javax.xml.bind.annotation.*;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Datasource extends AbstractPooledResource implements DBTypeAware, Cloneable {

    @XmlAttribute(required = false)
    private DBType type = DBType.ORACLE;

    @XmlAttribute(required = false)
    private boolean cmpEnabled = true;

    @XmlAttribute(required = false)
    private boolean jtaEnabled = true;

    @XmlElement(namespace = Namespaces.DEFAULT)
    private FlywaySettings flywaySettings = new FlywaySettings();

    public Datasource() {
    }

    public Datasource(String alias, DBType type, String jndi) {
        this(alias, type, jndi, true);
    }

    public Datasource(String alias, DBType type, String jndi, boolean cmpEnabled) {
        setAlias(alias);
        setType(type);
        setJndi(jndi);
        setCmpEnabled(cmpEnabled);
    }

    @Override
    public Datasource clone() throws CloneNotSupportedException {
        return (Datasource) super.clone();
    }

    public DBType getType() {
        return type;
    }

    public void setType(DBType type) {
        this.type = type;
    }

    public boolean isCmpEnabled() {
        return cmpEnabled;
    }

    public void setCmpEnabled(boolean cmpEnabled) {
        this.cmpEnabled = cmpEnabled;
    }

    public boolean isJtaEnabled() {
        return jtaEnabled;
    }

    public void setJtaEnabled(boolean jtaEnabled) {
        this.jtaEnabled = jtaEnabled;
    }

    public FlywaySettings getFlywaySettings() {
        return flywaySettings;
    }

    public void setFlywaySettings(FlywaySettings flywaySettings) {
        this.flywaySettings = flywaySettings;
    }
}
