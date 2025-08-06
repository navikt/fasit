package no.nav.aura.appconfig.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class AbstractPropertyResource extends EnvironmentDependentResource {

    /** Name of the property this resource is mapped to */
    @XmlAttribute(required = false)
    private String mapToProperty;

    public AbstractPropertyResource() {
        super();
    }

    /** Name of the property this resource is mapped to at the target system. Supports multiple properties separated with comma */
    public String getMapToProperty() {
        return mapToProperty == null ? getAlias() : mapToProperty;
    }

    /** Name of the property this resource is mapped to at the target system. Supports multiple properties separated with comma */
    public void setMapToProperty(String mapToProperty) {
        this.mapToProperty = mapToProperty;
    }

}
