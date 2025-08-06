package no.nav.aura.appconfig.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Properties internal to the application that changes between environments. The properties in this resource should have
 * "production" values as default and there should be minimal need for this resources in prod environment
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ApplicationProperties extends EnvironmentDependentResource {

}
