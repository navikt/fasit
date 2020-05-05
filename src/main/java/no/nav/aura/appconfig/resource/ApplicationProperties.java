package no.nav.aura.appconfig.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Properties internal to the application that changes between environments. The properties in this resource should have
 * "production" values as default and there should be minimal need for this resources in prod environment
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ApplicationProperties extends EnvironmentDependentResource {

}
