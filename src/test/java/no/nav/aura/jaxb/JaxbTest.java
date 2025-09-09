package no.nav.aura.jaxb;

import no.nav.aura.appconfig.*;
import no.nav.aura.appconfig.artifact.Artifact;
import no.nav.aura.appconfig.artifact.Batch;
import no.nav.aura.appconfig.artifact.ClassPathLibrary;
import no.nav.aura.appconfig.artifact.Ear;
import no.nav.aura.appconfig.exposed.*;
import no.nav.aura.appconfig.monitoring.Metric;
import no.nav.aura.appconfig.monitoring.SelftestMonitoring;
import no.nav.aura.appconfig.resource.*;
import no.nav.aura.appconfig.resource.DBTypeAware.DBType;
import no.nav.aura.appconfig.resource.ListenerPort.InitialStates;
import no.nav.aura.appconfig.security.*;
import no.nav.aura.appconfig.serveroptions.Cron;
import no.nav.aura.appconfig.serveroptions.ServerOptions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

public class JaxbTest {

    private final Application app = Application.instance(getClass().getResourceAsStream("/app-config-max.xml"));

    @Test
    public void basicParameters() {
        assertThat(app.getName(), is("tpr-testapp"));
        assertThat("all resources", app.getResources().size(), is(26));
        assertThat("services", app.getExposedServices().size(), is(10));
    }

    @Test
    public void jndiAttributesMustBeOptional() {
        Collection<Datasource> resourceCollection = app.getResources(Datasource.class);

        Datasource applicationManagedDatasource = findResourceByAlias("myUnmanagedDB2DataSource", Datasource.class);
        assertNull(applicationManagedDatasource.getJndi());

        Datasource containerManagedDatasource = findResourceByAlias("myXmlDataSource", Datasource.class);
        assertEquals("java:/ds/myXmlDatasource", containerManagedDatasource.getJndi());
    }

    @Test
    public void selftest() throws Exception {
        Selftest selftest = app.getSelftest();
        assertEquals("/virgo/internal/selftest", selftest.getPath());
        assertEquals("/virgo/internal/selftest", selftest.getHumanReadablePath());
    }

    @Test
    public void testLoadBalancing() throws Exception {
        LoadBalancer loadBalancer = app.getLoadBalancer();

        LBHealthMonitor lbHealthMonitor = loadBalancer.getHealthMonitor();
        assertThat("alias is what we set", loadBalancer.getAlias(), is("banan.loadbalancer"));
        assertThat("isAlive is what we set", loadBalancer.getIsAlive(), is("/app/internal/isAlive"));
        assertThat("connection limit is what we set", loadBalancer.getConnectionLimit(), is(69));
    }

    @Test
    public void testSuspend() {
        Suspend suspend = app.getSuspend();

        assertThat("suspen url is what we set", suspend.getUrl(), is("mgmt/suspend"));
        assertThat("suspend timeout is what we set", suspend.getTimeoutSeconds(), is(96));
        assertThat("suspend credntial is what we set", suspend.getCredential(), is("suspendCredentialAlias"));
    }

    @Test
    public void artifacts() {
        List<Artifact> artifacts = app.getArtifacts();
        assertThat("artifacts", artifacts.size(), is(3));

        Ear someartifact = (Ear) findByArtifactId(artifacts, "someartifactId");
        assertEquals("no.nav.somegroup", someartifact.getGroupId());
        assertThat(someartifact.getVersion(), is(nullValue()));
        assertThat(someartifact.getName(), equalTo("someDisplayName"));

        Ear versionArtifact = (Ear) findByArtifactId(artifacts, "versionedArtifact");
        assertEquals("no.nav.somegroup", versionArtifact.getGroupId());
        assertThat(versionArtifact.getVersion(), equalTo("1.0"));

        ClassPathLibrary classPathLibraryArtifact = (ClassPathLibrary) findByArtifactId(artifacts, "regler");
        assertThat(classPathLibraryArtifact.requiresClassifier(), is(true));
        assertThat(classPathLibraryArtifact.unpack(), is(true));
        assertThat(classPathLibraryArtifact.usesSetNativeLibraryPath(), is(true));

        Batch bathArtifact = (Batch) findByArtifactId(artifacts, "batchArtifact");
        assertThat(bathArtifact.getGroupId(), is("no.nav.somebatchgroup"));
        assertThat(bathArtifact.getVersion(), is("2.0"));
        assertThat(bathArtifact.getSymlink(), is("/was_app/batch/pen"));

    }

    @Test
    public void filter() {
        assertThat("filter resources", app.getResources(Datasource.class).size(), is(5));
    }

    @Test
    public void filterAbstractPropertyResources() {
        assertThat("filter property resources", app.getResources(AbstractPropertyResource.class).size(), is(19));
    }

    @Test
    public void dataSourceProperties() {
        Datasource ds = findResourceByAlias("myDataSource", Datasource.class);
        assertEquals("java:/ds/myDatasource", ds.getJndi());
        assertEquals(DBTypeAware.DBType.ORACLE, ds.getType());
        assertThat(ds.getPool().getMaxPoolSize(), is(50));
        assertThat(ds.getPool().getConnectionTimeout(), is(180));
        assertThat(ds.getJ2EEResourceProperties(), hasKey("mykey"));
        assertThat(ds.getJ2EEResourceProperties(), hasValue("myValue"));
    }

    @Test
    public void applicationProperties() {
        ApplicationProperties ds = findResourceByAlias("myCustomProps", ApplicationProperties.class);
        assertNotNull(ds);
    }

    @Test
    public void noCustomPropertiesSetOnResource_shouldReturnEmptyMap() {
        Datasource ds = findResourceByAlias("myXaDataSource", Datasource.class);
        assertTrue(ds.getJ2EEResourceProperties().isEmpty());
    }

    @Test
    public void whenNoPurgePolicyIsSet_shouldReturnNull() {
        Datasource ds = findResourceByAlias("myXaDataSource", Datasource.class);
        assertNull(ds.getPool().getPurgePolicy());
    }

    @Test
    public void cmpFlagShouldDefaultToTrueOnDataSources() {
        Datasource ds = findResourceByAlias("myDataSource", Datasource.class);
        assertTrue(ds.isCmpEnabled());
    }

    @Test
    public void cmpFlagIsDisabled() {
        Datasource ds = findResourceByAlias("myXaDataSource", Datasource.class);
        assertThat(ds.isCmpEnabled(), is(false));
    }

    @Test
    public void xaEnabledFlagShouldDefaultToFalseOnAnyAbstractJndiResource() {
        Datasource ds = findResourceByAlias("myDataSource", Datasource.class);
        Cics cics = findResourceByAlias("myXaCics", Cics.class);
        assertThat(ds.isXaEnabled(), is(false));
        assertThat(cics.isXaEnabled(), is(false));
    }

    @Test
    public void whenCustomPropertiesElementHasNoParentObjectAttribute_shouldDefaultToJ2EEResourceProperty() {
        Cics cics = findResourceByAlias("myCics", Cics.class);
        assertThat(cics.getJ2EEResourceProperties(), hasKey("mykey"));
    }

    @Test
    public void xaDataSourceProperties() {
        Datasource ds = findResourceByAlias("myXaDataSource", Datasource.class);
        assertEquals(DBType.DB2, ds.getType());
        assertTrue(ds.isXaEnabled());
        assertEquals("java:/ds/myXaDatasource", ds.getJndi());
        assertThat(ds.getPool().getMaxPoolSize(), is(50));
    }

    @Test
    public void xmlDataSourceProperties() {
        Datasource xds = findResourceByAlias("myXmlDataSource", XmlDatasource.class);
        assertEquals("java:/ds/myXmlDatasource", xds.getJndi());
        assertThat(xds.getPool().getMaxPoolSize(), is(50));
    }

    @Test
    public void unmanagedDataSourceProperties() {
        Datasource ds = findResourceByAlias("someUnmanagedDataSource", Datasource.class);
        assertEquals("someUnmanagedDataSource", ds.getAlias());
        assertEquals("someUnmanagedDataSource", ds.getMapToProperty());
    }

    @Test
    public void ldapProperties() {
        Ldap ldap = app.getResources(Ldap.class).iterator().next();
        assertEquals("ldap", ldap.getAlias());
        assertEquals("ldapProperty", ldap.getMapToProperty());
    }

    @Test
    public void appCert() {
        ApplicationCertificate cert = app.getResources(ApplicationCertificate.class).iterator().next();
        assertEquals("myCert", cert.getAlias());
    }

    @Test
    public void emptyDirectory() {
        Directory directory = findbyName(app.getResources(Directory.class), "kodeverkcache");
        assertEquals("kodeverkcache", directory.getName());
        assertEquals("folder.kodeverk", directory.getMapToProperty());
        assertTrue(directory.isTemporary());
    }

    @Test
    public void directoryWithFiles() {
        Directory directory = findbyName(app.getResources(Directory.class), "folderWithFiles");
        assertEquals("folderWithFiles", directory.getName());
        assertEquals("folder.withfiles", directory.getMapToProperty());
        assertEquals("/was_app/whazzupp", directory.getSymlink());
        assertEquals(1, directory.getFiles().size());
        FileElement fileElement = directory.getFiles().get(0);
        assertEquals("foo/bar/importantFile.xml", fileElement.getSource());
        assertEquals("file.important", fileElement.getMapToProperty());

    }

    @Test
    public void directoryWithAlias() {
        FileLibrary directory = findResourceByAlias("autotest_ledeteksterv2", FileLibrary.class);
        assertEquals("autotest_ledeteksterv2", directory.getAlias());
        assertEquals("folder.ledetekst", directory.getMapToProperty());
    }

    @Test
    public void jms() {
        QueueManager qmgr = findResourceByAlias("someQueueManager", QueueManager.class);
        Collection<Queue> queues = qmgr.getQueues();

        assertEquals("someQueueManager", qmgr.getAlias());
        assertEquals("java:/jboss/mqConnectionFactory", qmgr.getJndi());
        assertThat(qmgr.isSslEnabled(), is(true));
        assertThat(qmgr.isBindToMdbEnabled(), is(true));
        assertThat(qmgr.isUnifiedConnectionFactory(), is(true));

        assertThat(qmgr.getPool().getMaxPoolSize(), is(50));
        assertThat(qmgr.getSessionPool().getAgedTimeout(), is(444));
        assertThat(qmgr.getSessionPool().getMaxPoolSize(), is(3));
        assertThat(qmgr.getSessionPool().getMinPoolSize(), is(2));
        assertThat(qmgr.getSessionPool().getPurgePolicy(), is("EntirePool"));

        assertThat(qmgr.getCredential().getAlias(), is("myMqUser"));
        assertThat(qmgr.getChannel().getAlias(), is("myChannel"));

        assertThat("queue size", queues.size(), is(1));

        Queue queue = queues.iterator().next();
        assertEquals("java:/jboss/MY_QUEUE", queue.getJndi());
        assertEquals("someQueue", queue.getAlias());
        assertThat("queue size", queues.size(), is(1));
        assertThat(queue.getListenerPort(), notNullValue());
        assertThat(queue.getListenerPort().getName(), is("myListenerPort"));
        assertThat(queue.getListenerPort().getInitialState(), is(InitialStates.START));
        assertThat(queue.getListenerPort().getMaxSessions(), is(1));
        assertThat(queue.getListenerPort().getStartOnlyOnOneNode(), is(true));
        assertThat(queue.getTargetClient(), is(Queue.TargetClient.MQ));

        assertEquals("ALWAYS", queue.getCustomProperties().get("replyWithRFH2"));
        assertEquals("819", queue.getCustomProperties().get("CCSID"));
    }

    @Test
    public void jms_MultipleQueueManagers() {
        Collection<QueueManager> queueManagers = app.getResources(QueueManager.class);
        assertThat(queueManagers.size(), is(2));
    }

    @Test
    public void jms_QueueManagerSupportBothQueues() {
        QueueManager qmgr = findResourceByAlias("somOtherQueueManager", QueueManager.class);
        Collection<Queue> queues = qmgr.getQueues();

        assertThat(queues.size(), is(1));
        assertThat(queues.iterator().next().getAlias(), is("someQueueInOtherQM"));
    }

    @Test
    public void cronJobs() {
        ServerOptions serverOptions = app.getServerOptions();
        List<Cron> cron = serverOptions.getCronjobs();
        Cron job = cron.get(0);
        assertEquals("beskrivelsesomblirmedicrontabjobbsomkommentar", job.getDescription());
        assertEquals("* * * * *", job.getSchedule());
        assertEquals("kommando", job.getCommand());

    }

    @Test
    public void cicsProperties() {
        Cics cics = findResourceByAlias("myCics", Cics.class);
        assertEquals("jca/cics/mycics", cics.getJndi());
        assertThat(cics.getPool().getMaxPoolSize(), is(50));
        assertThat(cics.getPool().getConnectionTimeout(), is(180));
        assertThat(cics.getPool().getReapTime(), is(3600));
        assertEquals(1, cics.getJ2EEResourceProperties().size());
        assertEquals("myValue", cics.getJ2EEResourceProperties().get("mykey"));
    }

    @Test
    public void customPropertiesOnConnectionPool() {
        Datasource dataSource = findResourceByAlias("myDataSource", Datasource.class);
        assertThat(dataSource.getPool().getCustomProperties(), hasEntry("myPoolKey", "myPoolValue"));
    }

    @Test
    public void testSecurity() {
        assertEquals("srv_myapp", app.getSecurity().getServiceUserResourceAlias());
    }

    @Test
    public void testMonitoringMetrics() {
        List<Metric> metrics = app.getMonitoring().getMetrics();
        assertThat("monitoring object has the right metric count", metrics.size(), is(2));
        assertThat("the metric path is as specified", metrics.get(0).getPath(), is("/metrics1"));
        assertThat("the metric interval is as specified", metrics.get(0).getInterval(), is(69));
        assertThat("the metric interval is 60 by default (nothing specified)", metrics.get(1).getInterval(), is(60));
    }

    @Test
    public void testMonitoringSelftest() {
        SelftestMonitoring selftest = app.getMonitoring().getSelftest();
        assertNotNull(selftest);
        assertThat("the selftest interval is as specified", selftest.getInterval(), is(42));
    }

    @Test
    public void testLogins() {
        assertThat(app.getSecurity().getRoleMappings().size(), is(2));
        assertThat(app.getSecurity().getRoleMappings().get(0).getResourceAlias(), is("myFirstRoleAlias"));
        assertThat(app.getSecurity().getRoleMappings().get(0).getToRole(), is("USER"));
        assertThat(app.getSecurity().getLogins().size(), is(6));

        OpenAm openAmLogin = app.getSecurity().getLogin(OpenAm.class);
        assertThat(openAmLogin.getContextRoots().get(0), is("myFirstContextRoot"));

        Spnego spnegoLogin = app.getSecurity().getLogin(Spnego.class);
        assertThat(spnegoLogin.getLdapResourceAlias(), is("myLdap"));
        assertThat(spnegoLogin.getFallbackLoginPagePath(), is("/myApp/login.html"));

        Saml samlLogin = app.getSecurity().getLogin(Saml.class);
        assertNotNull(samlLogin);

        LdapAuth ldapAuth = app.getSecurity().getLogin(LdapAuth.class);
        assertThat(ldapAuth.getLdapResourceAlias(), is("someLdap"));
        assertThat(ldapAuth.getAdditionalBaseContext(), is("OU=ApplAccounts,OU=ServiceAccounts"));
        assertThat(ldapAuth.getLockToUser(), is("e137012"));
        assertThat(ldapAuth.getAuthenticatedRole(), is("jeg_er_autentisert_ihvertfall"));

        OpenIdConnect openIdLogin = app.getSecurity().getLogin(OpenIdConnect.class);
        assertThat(openIdLogin.getModuleClass(), is("no.nav.aura.loginmodule.JwtLoginModule"));
        assertThat(openIdLogin.getContextRoots().size(), is(2));
        assertThat(openIdLogin.getContextRoots().get(0), is("/myFirstContextRoot"));

    }

    @Test
    public void testRunAsMapping() {
        assertThat(app.getSecurity().getRunAsMappings().size(), is(2));
        assertThat(app.getSecurity().getRunAsMappings().get(0).getResourceAlias(), is("someCredential"));
        assertThat(app.getSecurity().getRunAsMappings().get(0).getToRole(), is("aRole"));
        assertThat(app.getSecurity().getRunAsMappings().get(1).getResourceAlias(), is("someOtherCredential"));
        assertThat(app.getSecurity().getRunAsMappings().get(1).getToRole(), is("anOtherRole"));
    }

    @Test
    public void webservice() {
        Webservice webservice = app.getResources(Webservice.class).iterator().next();
        assertEquals("ws1", webservice.getAlias());
        assertEquals("myWs1", webservice.getMapToProperty());
    }

    @Test
    public void rest() {
        Rest rest = app.getResources(Rest.class).iterator().next();
        assertEquals("aRestService", rest.getAlias());
        assertEquals("aRestService", rest.getMapToProperty());
    }

    @Test
    public void letterTemplate() {
        LetterTemplate letterTemplate = app.getResources(LetterTemplate.class).iterator().next();
        assertEquals("arenabrev_v1", letterTemplate.getAlias());
    }

    @Test
    public void exposedLetterTemplate() {
        Collection<ExposedLetterTemplate> exposedLetterTemplates = app.getExposedServices(ExposedLetterTemplate.class);
        assertEquals(1, exposedLetterTemplates.size());
        ExposedLetterTemplate template = exposedLetterTemplates.iterator().next();
        assertEquals("arenabrev_v1", template.getName());
        assertEquals("no.nav.brevmal", template.getGroupId());
        assertEquals("arenabrev", template.getArtifactId());
        assertEquals("1.1", template.getVersion());
        assertEquals("HL3 brevene til arena", template.getDescription());
    }

    @Test
    public void exposedWebservice() {
        Collection<ExposedSoap> exposedServices = app.getExposedServices(ExposedSoap.class);
        ExposedSoap service = findByName(exposedServices, "exposeThis");

        assertEquals("exposeThis", service.getName());
        assertEquals("/test/brukerprofil", service.getPath());
        assertEquals("myWsdl", service.getWsdlArtifactId());
        assertEquals("no.nav.tjenester.test", service.getWsdlGroupId());
        assertEquals("1.0", service.getWsdlVersion());
        assertEquals(SecurityToken.NONE, service.getSecurityToken());
        assertEquals(1, service.getExportToZones().size());
        assertTrue(service.exportTo(NetworkZone.SBS), "expose to SBS ");
    }

    @Test
    public void exposedSoapService() {
        Collection<ExposedSoap> exposedServices = app.getExposedServices(ExposedSoap.class);
        assertEquals(4, exposedServices.size());
        ExposedSoap sgwService = findByName(exposedServices, "exposeThisSoapService");

        assertEquals("exposeThisSoapService", sgwService.getName());
        assertEquals("/test/brukerprofil", sgwService.getPath());
        assertEquals("myWsdl", sgwService.getWsdlArtifactId());
        assertEquals("no.nav.tjenester.test", sgwService.getWsdlGroupId());
        assertEquals("1.0", sgwService.getWsdlVersion());
        assertEquals(SecurityToken.NONE, sgwService.getSecurityToken());
        assertEquals(1, sgwService.getExportToZones().size());
        assertTrue(sgwService.exportTo(NetworkZone.SBS), "expose to SBS ");
        assertEquals("Dette er en viktig tjeneste", sgwService.getDescription());
    }

    @Test
    public void exposedSoapServiceOnlyRequiredValues() {
        Collection<ExposedSoap> exposedServices = app.getExposedServices(ExposedSoap.class);
        assertEquals(4, exposedServices.size());
        ExposedSoap service = findByName(exposedServices, "myLittleWS");

        assertEquals("myLittleWS", service.getName());
        assertEquals("/services/AKindOfService", service.getPath());
    }

    @Test
    public void exposedRestService() {
        Collection<ExposedRest> exposedServices = app.getExposedServices(ExposedRest.class);
        assertEquals(1, exposedServices.size());
        ExposedRest service = findByName(exposedServices, "aRestService");

        assertEquals("aRestService", service.getName());
        assertEquals("/nav.no/AKindOfRestService", service.getPath());
    }

    @Test
    public void exposedSoapNotDeployedToServiceGateway() {
        long servicesNotDeployedToServiceGateway = app.getExposedServices(ExposedSoap.class).stream().filter(exposedSoap -> exposedSoap.isDeployToServiceGateway() == false).count();
        assertEquals(1, servicesNotDeployedToServiceGateway);
    }

    @Test
    public void exposedFileLibrary() {
        Collection<ExposedFileLibrary> exposedServices = app.getExposedServices(ExposedFileLibrary.class);
        assertEquals(1, exposedServices.size());
        ExposedFileLibrary service = findByName(exposedServices, "autodeploy-kodeverk");

        assertEquals("autodeploy-kodeverk", service.getName());
        assertEquals("kodeverkcache", service.getDirectory());
    }

    @Test
    public void exposedUrl() {
        Collection<ExposedUrl> exposedUrl = app.getExposedServices(ExposedUrl.class);
        assertEquals(1, exposedUrl.size());
        ExposedUrl service = findByName(exposedUrl, "aWebLink");

        assertEquals("aWebLink", service.getName());
        assertEquals("linkToAnotherApp", service.getPath());
        assertTrue(service.exportTo(NetworkZone.ALL), "expose to ALL domains ");
    }

    @Test
    public void nfs() throws Exception {
        Directory directory = findbyName(app.getResources(Directory.class), "nfsFolder");
        assertThat(directory.getName(), is("nfsFolder"));
        NfsMount mountOnNfs = directory.getMountOnNfs();
        assertThat(mountOnNfs, is(notNullValue()));
        assertThat(mountOnNfs.getCustomNFSResourceAlias(), is("sharedNFS"));
    }

    @Test
    public void exposedEjb() {
        Collection<ExposedEjb> exposedEjbs = app.getExposedServices(ExposedEjb.class);
        assertEquals(1, exposedEjbs.size());
        ExposedEjb exposed = exposedEjbs.iterator().next();

        assertThat(exposed.getName(), is("testEJB"));
        assertThat(exposed.getJndi(), is("java:/ejb/no/nav/TralalaHome"));
        assertThat(exposed.getBeanHomeInterface(), is("no.nav.gsak.ejb.TralalaHome"));
        assertThat(exposed.getBeanComponentInterface(), is("no.nav.gsak.ejb.Tralala"));
    }

    @Test
    public void exposedJMS() {
        Collection<ExposedQueue> exposedServices = app.getExposedServices(ExposedQueue.class);
        assertEquals(1, exposedServices.size());
        ExposedQueue incoming = findByName(exposedServices, "someQueue");
        assertEquals("someQueue", incoming.getName());
    }

    @Test
    public void serverJvmArgs() {
        ServerOptions serverOptions = app.getServerOptions();
        assertThat(serverOptions.getMemoryParameters().getResourceAlias(), equalTo("myMemoryParameters"));
        assertThat(serverOptions.getJvmArgs(), equalTo("-Dhei=hopp -esa"));
        assertThat(serverOptions.getCustomProperties(), hasSize(9));
        Map<String, String> transactionServicePropertySet = serverOptions.getCustomProperties("TransactionService");
        assertTrue(transactionServicePropertySet.containsKey("totalTranLifetimeTimeout"));
        assertTrue(transactionServicePropertySet.containsValue("7200"));
        Map<String, String> serverClusterePropertySet = serverOptions.getCustomProperties("ServerCluster");
        assertTrue(serverClusterePropertySet.containsKey("jsfProvider"));
        assertTrue(serverClusterePropertySet.containsValue("SunRi1.2"));
        Map<String, String> sessionTuning = serverOptions.getCustomProperties("TuningParams");
        assertThat(sessionTuning, hasEntry("maxInMemorySessionCount", "1337"));
        Map<String, String> processPriority = serverOptions.getCustomProperties("ProcessExecution");
        assertThat(processPriority, hasEntry("processPriority", "10"));
    }

    private Artifact findByArtifactId(List<? extends Artifact> artifacts, String artifactId) {
        for (Artifact artifact : artifacts) {
            if (artifactId.equals(artifact.getArtifactId())) {
                return artifact;
            } else {
                if (artifact instanceof Ear ear) {
                    if (ear.getClassPathLibraries() != null) {
                        List<ClassPathLibrary> classPathLibraries = ear.getClassPathLibraries();
                        for (ClassPathLibrary library : classPathLibraries) {
                            if (library.getArtifactId().equals(artifactId)) {
                                return library;
                            }
                        }
                    }
                }
            }
        }
        throw new IllegalArgumentException("artifact with id " + artifactId + " not found");
    }

    private Directory findbyName(Collection<Directory> files, String name) {
        for (Directory directory : files) {
            if (name.equals(directory.getName())) {
                return directory;
            }
        }
        throw new IllegalArgumentException("no directory found with name " + name);
    }

    private <T extends EnvironmentDependentResource> T findResourceByAlias(String alias, Class<T> resourceType) {
        Collection<T> resouces = app.getResources(resourceType);
        T foundResource = null;
        for (T resource : resouces)
            if (alias.equals(resource.getAlias()))
                foundResource = resource;
        assertNotNull(foundResource);
        return foundResource;
    }

    private <T extends ExposedService> T findByName(Collection<T> exposedServices, String name) {
        for (T exposedService : exposedServices) {
            if (name.equals(exposedService.getName())) {
                return exposedService;
            }
        }
        fail("Exposed service with name " + name + " not found");
        return null;
    }

}
