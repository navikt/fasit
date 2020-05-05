package no.nav.aura.appconfig.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Endpoint of web service including location of the corresponding WSDL artifact
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Webservice extends EnvironmentDependentResource {
    /** Name of the property this resource is mapped to */
	@XmlAttribute(required = true)
	private String mapToProperty;

    @XmlAttribute
    private boolean skipServiceGateway = false;


	/** Name of the property this resource is mapped to at the target system. Supports multiple properties separated with comma */
    public String getMapToProperty() {
        return mapToProperty;
    }

    /** Name of the property this resource is mapped to at the target system. Supports multiple properties separated with comma */
    public void setMapToProperty(String mapToProperty) {
        this.mapToProperty = mapToProperty;
    }

    public boolean skipServicegGateway() {
        return skipServiceGateway;
    }

    public void setSkipServiceGateway(boolean skipServiceGateway) {
        this.skipServiceGateway = skipServiceGateway;
    }
}
