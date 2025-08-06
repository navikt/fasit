package no.nav.aura.appconfig.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Credential resource mapping to .username .password for service users and others.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Credential extends AbstractPropertyResource {

}
