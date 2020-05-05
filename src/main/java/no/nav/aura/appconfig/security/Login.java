package no.nav.aura.appconfig.security;

import javax.xml.bind.annotation.XmlSeeAlso;

@XmlSeeAlso({ OpenAm.class, Spnego.class, Saml.class, LdapAuth.class, OpenIdConnect.class, DatabaseLogin.class })
public abstract class Login {

}
