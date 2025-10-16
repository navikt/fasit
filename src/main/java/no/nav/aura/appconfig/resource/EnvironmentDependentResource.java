package no.nav.aura.appconfig.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

/**
 * Resource depending on fasit properties
 */
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class EnvironmentDependentResource extends Resource {

    @XmlAttribute(required = true)
    private String alias;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

}
