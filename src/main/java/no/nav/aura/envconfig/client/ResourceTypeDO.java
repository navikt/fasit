package no.nav.aura.envconfig.client;

import no.nav.aura.appconfig.LoadBalancer;
import no.nav.aura.appconfig.resource.ApplicationCertificate;
import no.nav.aura.appconfig.resource.ApplicationProperties;
import no.nav.aura.appconfig.resource.BaseUrl;
import no.nav.aura.appconfig.resource.Channel;
import no.nav.aura.appconfig.resource.Cics;
import no.nav.aura.appconfig.resource.Credential;
import no.nav.aura.appconfig.resource.DBTypeAware;
import no.nav.aura.appconfig.resource.Ejb;
import no.nav.aura.appconfig.resource.EmailAddress;
import no.nav.aura.appconfig.resource.FileLibrary;
import no.nav.aura.appconfig.resource.Ldap;
import no.nav.aura.appconfig.resource.MemoryParameters;
import no.nav.aura.appconfig.resource.QueueManager;
import no.nav.aura.appconfig.resource.Resource;
import no.nav.aura.appconfig.resource.Rest;
import no.nav.aura.appconfig.resource.SmtpServer;
import no.nav.aura.appconfig.resource.Soap;
import no.nav.aura.appconfig.resource.Webservice;
import no.nav.aura.appconfig.security.RoleMapping;

public enum ResourceTypeDO {
    DataSource,
    MSSQLDataSource,
    DB2DataSource,
    LDAP(Ldap.class),
    BaseUrl(BaseUrl.class),
    Channel(Channel.class),
    Cics(Cics.class),
    Credential(Credential.class),
    Certificate(ApplicationCertificate.class),
    DeploymentManager,
    RoleMapping(RoleMapping.class),
    QueueManager(QueueManager.class),
    WebserviceEndpoint(Webservice.class),
    RestService(Rest.class),
    EJB(Ejb.class),
    OpenAm,
    OpenIdConnect,
    SMTPServer(SmtpServer.class),
    EmailAddress(EmailAddress.class),
    WebserviceGateway,
    Queue(no.nav.aura.appconfig.resource.Queue.class),
    ApplicationProperties(ApplicationProperties.class),
    LoadBalancer(LoadBalancer.class),
    LoadBalancerConfig,
    SoapService(Soap.class),
    MemoryParameters(MemoryParameters.class),
    FileLibrary(FileLibrary.class), 
    Datapower,
    AzureOIDC;

    private Class<?> mapFromResourceClass;

    private ResourceTypeDO() {
    }

    private ResourceTypeDO(Class<?> mapFromResourceClass) {
        this.mapFromResourceClass = mapFromResourceClass;
    }

    public boolean isTypeFor(Resource resource) {
        return mapFromResourceClass != null && mapFromResourceClass.isAssignableFrom(resource.getClass());
    }

    public static ResourceTypeDO findTypeFor(Resource resource) {
        ResourceTypeDO resourceTypeDO = find(resource);
        if (resourceTypeDO != null) {
            return resourceTypeDO;
        }
        throw new IllegalArgumentException("Resource of type " + resource.getClass() + " has no mapped resourceType");
    }

    private static ResourceTypeDO find(Resource resource) {
        if (resource instanceof DBTypeAware ds) {
            if (ds.getType() == DBTypeAware.DBType.DB2) {
                return DB2DataSource;
            }
            return DataSource;
        }

        ResourceTypeDO[] values = ResourceTypeDO.values();
        for (ResourceTypeDO resourceType : values) {
            if (resourceType.isTypeFor(resource)) {
                return resourceType;
            }
        }
        return null;
    }

    public static boolean isDefinedFor(Resource resource) {
        return find(resource) != null;
    }

}
