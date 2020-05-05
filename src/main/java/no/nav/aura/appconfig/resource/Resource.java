package no.nav.aura.appconfig.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({Datasource.class, XmlDatasource.class, BaseUrl.class, Channel.class, Credential.class, ApplicationCertificate.class, QueueManager.class,
        Webservice.class, Soap.class, Ejb.class, Rest.class, EmailAddress.class, SmtpServer.class, Ldap.class, Directory.class, Cics.class, ApplicationProperties.class, LetterTemplate.class, SchedulerConfiguration.class,
        TimerManager.class, WorkManager.class, ResourceEnvironmentProviderConfiguration.class, FileLibrary.class,
        ActivationSpec.class})
public abstract class Resource {

}
