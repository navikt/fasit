package no.nav.aura.appconfig.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Endpoint of soap service. This type is different from the Webservice resource in that it is not deployed to service gateway.
 * Applications using this resource will give the url directly to the service. And not to service gateway as happens with the Webservice resource
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Soap extends AbstractPropertyResource {
}
