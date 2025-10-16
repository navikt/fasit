package no.nav.aura.appconfig.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Setting up application certificate on the target server. Result is a keystorefile and system properties with keystore.path,
 * keystore password and keystore alias
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ApplicationCertificate extends EnvironmentDependentResource {

}
