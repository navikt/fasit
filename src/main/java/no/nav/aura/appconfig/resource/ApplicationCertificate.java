package no.nav.aura.appconfig.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Setting up application certificate on the target server. Result is a keystorefile and system properties with keystore.path,
 * keystore password and keystore alias
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ApplicationCertificate extends EnvironmentDependentResource {

}
