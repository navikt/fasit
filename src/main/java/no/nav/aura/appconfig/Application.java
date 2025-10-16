package no.nav.aura.appconfig;

import static no.nav.aura.appconfig.Filter.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import no.nav.aura.appconfig.artifact.Artifact;
import no.nav.aura.appconfig.deprecated.Deprecations;
import no.nav.aura.appconfig.exposed.ExposedService;
import no.nav.aura.appconfig.logging.Logging;
import no.nav.aura.appconfig.monitoring.Monitoring;
import no.nav.aura.appconfig.resource.Resource;
import no.nav.aura.appconfig.security.AbacSecurity;
import no.nav.aura.appconfig.security.Security;
import no.nav.aura.appconfig.serveroptions.ServerOptions;

/**
 * Main object for application configuration. This is the start point of all operations in app-config domain
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "application")
public class Application {

    @XmlElement(required = true)
    private String name;

    @XmlElement
    private String type;


    /**
     * relative path to selftestpage
     */
    @XmlElement
    private Selftest selftest;

    @XmlElement
    private LoadBalancer loadBalancer;

    @XmlElement
    private Suspend suspend;

    @XmlElementWrapper
    @XmlElementRef
    private List<Resource> resources = new ArrayList<>();

    @XmlElementWrapper(name = "exposed-services")
    @XmlElementRef
    private List<ExposedService> services = new ArrayList<>();

    @XmlElementWrapper
    @XmlElementRef(required = true)
    private List<Artifact> artifacts = new ArrayList<>();

    @XmlElement
    private ServerOptions serverOptions;

    @XmlElement
    private Security security;

    @XmlElement
    private AbacSecurity abacSecurity;

    @XmlElement
    private Logging logging;

    @XmlElement
    private Monitoring monitoring;

    public Application() {
    }

    public Application(String name) {
        this();
        this.name = name;
    }

    public Collection<Resource> getResources() {
        return resources;
    }

    /**
     * Return resource of a given type
     */
    public <T extends Resource> Collection<T> getResources(Class<T> type) {
        return filter(resources, type);
    }

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    /**
     * Return artifact of a given type
     */
    public <T extends Artifact> Collection<T> getArtifacts(Class<T> type) {
        return filter(artifacts, type);
    }

    public Selftest getSelftest() {
        return selftest;
    }

    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public Collection<ExposedService> getExposedServices() {
        return services;
    }

    /**
     * Return service of a given type
     */
    public <T extends ExposedService> Collection<T> getExposedServices(Class<T> type) {
        return filter(services, type);
    }

    public ServerOptions getServerOptions() {
        return serverOptions;
    }

    public void setServerOptions(ServerOptions serverOptions) {
        this.serverOptions = serverOptions;
    }

    public Security getSecurity() {
        return security;
    }

    public AbacSecurity getAbacSecurity() {
        return abacSecurity;
    }

    public Deprecations deprecations() {
        return new Deprecations(this);
    }

    public static Application instance(File xmlfile) {
        try {
            FileInputStream xml = new FileInputStream(xmlfile);
            return instance(xml);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File not found", e);
        }
    }

    public static Application instance(InputStream xml) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Application.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            unmarshaller.setSchema(getAppconfigXsd());
            // Mirakuløs for debug
            // unmarshaller.setEventHandler(new jakarta.xml.bind.helpers.DefaultValidationEventHandler());
            Application application = (Application) unmarshaller.unmarshal(xml);
            return (Application) application;
        } catch (JAXBException e) {
            if (e.getLinkedException() instanceof SAXParseException) {
                SAXParseException saxParseException = (SAXParseException) e.getLinkedException();
                throw new IllegalArgumentException("Xsd validation of app-config.xml failed: Reason " + saxParseException.getMessage());
            }

            throw new RuntimeException("Error unmarshalling application xml", e);
        }
    }

    public static Application instanceNotValidating(InputStream xml) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Application.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (Application) unmarshaller.unmarshal(xml);
        } catch (JAXBException e) {
            throw new RuntimeException("Error unmarshalling application xml", e);
        }
    }

    private static Schema getAppconfigXsd() {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            // System.out.println(Application.class.getResource("/appconfig.xsd"));
            return schemaFactory.newSchema(new StreamSource(Application.class.getResourceAsStream("/appconfig.xsd")));
        } catch (SAXException e) {
            throw new IllegalArgumentException("Error getting appconfig xsd ", e);
        }
    }

    public String asXml() {
        try {
            StringWriter sw = new StringWriter();
            JAXBContext jaxbContext = JAXBContext.newInstance(Application.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(this, sw);
            return sw.toString();

        } catch (JAXBException e) {
            throw new RuntimeException("error unmarshall", e);
        }
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Monitoring getMonitoring() {
        return monitoring;
    }

    public Logging getLogging() {
        return logging;
    }

    public void setAbacSecurity(AbacSecurity abacSecurity) {
        this.abacSecurity = abacSecurity;
    }

    public Suspend getSuspend() {
        return suspend;
    }
}
