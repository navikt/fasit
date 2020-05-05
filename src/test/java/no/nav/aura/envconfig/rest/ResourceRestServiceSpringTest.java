package no.nav.aura.envconfig.rest;

import no.nav.aura.envconfig.client.ApplicationInstanceDO;
import no.nav.aura.envconfig.client.ResourceTypeDO;
import no.nav.aura.envconfig.client.rest.ResourceElement;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.spring.SpringTest;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ResourceRestServiceSpringTest extends SpringTest {

    private final String loadBalancer = "https://myloadbalancer.adeo.no";
    private ResourcesRestService service;
    private Environment env;
    private ApplicationInstance applicationInstance;
    private Application application;
    private Resource datasourceResource;

    @Inject
    private ApplicationInstanceRepository appInstanceRepository;

    @Mock
    private UriInfo uriInfoMock;

    @BeforeEach
    public void setup() throws Exception {

        MockitoAnnotations.initMocks(this);
        initializeUriInfo();

        service = new ResourcesRestService(repository, appInstanceRepository);

        env = new Environment("test", EnvironmentClass.t);
        Cluster cluster = new Cluster("myCluster", Domain.TestLocal);
        cluster.setLoadBalancerUrl(loadBalancer);
        env.addCluster(cluster);
        application = new Application("MyApplication", "app", "no.nav.app");
        cluster.addApplication(application);
        cluster.addNode(new Node("hostname.test.local", "username", "password"));
        env.addCluster(cluster);
        env = repository.store(env);
        repository.store(new Resource("appUser", ResourceType.Credential, new Scope(EnvironmentClass.t)));

        datasourceResource = repository.store(new Resource("appDb2", ResourceType.DataSource, new Scope(env)));

        Set<ApplicationInstance> applicationInstances = repository.findEnvironmentBy("test").getApplicationInstances();
        assertEquals(1, applicationInstances.size());
        applicationInstance = applicationInstances.iterator().next();
        assertNull(applicationInstance.getVersion());
    }

    @Test
    public void resourcesMustShowDodgyResources() throws URISyntaxException {

        ResourceElement resourceElement = findResourceElement();
        assertEquals(false, resourceElement.isDodgy());

        initializeUriInfo();

        datasourceResource.markAsDodgy(false);
        resourceElement = findResourceElement();
        assertEquals(false, resourceElement.isDodgy());

        initializeUriInfo();

        datasourceResource.markAsDodgy(true);
        resourceElement = findResourceElement();
        assertEquals(true, resourceElement.isDodgy());

    }

    private ResourceElement findResourceElement() {
        ResourceElement resourceElement;
        resourceElement = service.findResources(
                datasourceResource.getScope().getEnvClass().name(),
                datasourceResource.getScope().getEnvironmentName(),
                Domain.TestLocal.getFqn(),
                application.getName(),
                ResourceTypeDO.DataSource,
                datasourceResource.getName(),
                true,
                false,
                uriInfoMock)[0];
        return resourceElement;
    }

    @Test
    public void resourceMustDisplayUsedInApplications() throws URISyntaxException {

        String resourceName1 = "aaregDataSource1";
        String resourceName2 = "aaregDataSource2";

        Scope scope = new Scope(EnvironmentClass.t).domain(Domain.TestLocal).envName(env.getName());

        Resource resource1 = repository.store(new Resource(resourceName1, ResourceType.DataSource, scope));
        Resource resource2 = repository.store(new Resource(resourceName2, ResourceType.DataSource, scope));

        ResourceReference resourceReference1 = new ResourceReference(resource1, 0L);
        ResourceReference resourceReference2 = new ResourceReference(resource2, 0L);

        Set<ResourceReference> resourceReferenceSet = applicationInstance.getResourceReferences();
        resourceReferenceSet.add(resourceReference1);
        resourceReferenceSet.add(resourceReference2);

        ResourceElement resourceElement1 = service.findResources(
                env.getEnvClass().name(),
                env.getName(),
                "test.local",
                application.getName(),
                ResourceTypeDO.DataSource,
                resourceName1,
                true,
                true,
                uriInfoMock)[0];

        initializeUriInfo();

        ResourceElement resourceElement2 = service.findResources(
                env.getEnvClass().name(),
                env.getName(),
                "test.local",
                application.getName(),
                ResourceTypeDO.DataSource,
                resourceName2,
                true,
                true,
                uriInfoMock)[0];

        ApplicationInstanceDO usedInApplicationInstance1 = resourceElement1.getUsedInApplication().get(0);
        ApplicationInstanceDO usedInApplicationInstance2 = resourceElement2.getUsedInApplication().get(0);

        String expectedApplicationName = application.getName();
        assertEquals(usedInApplicationInstance1.getName(), expectedApplicationName);
        assertEquals(usedInApplicationInstance2.getName(), expectedApplicationName);
    }

    @Test
    public void domainScopedResourceMustShowUsageInApplications() throws URISyntaxException {

        String resourceName = "aaregDataSource1";

        Scope scope = new Scope(EnvironmentClass.t).domain(Domain.TestLocal);

        Resource resource = repository.store(new Resource(resourceName, ResourceType.DataSource, scope));

        ResourceReference resourceReference = new ResourceReference(resource, 0L);

        Set<ResourceReference> resourceReferenceSet = applicationInstance.getResourceReferences();
        resourceReferenceSet.add(resourceReference);

        ResourceElement resourceElement = service.findResources(
                env.getEnvClass().name(),
                env.getName(),
                "test.local",
                application.getName(),
                ResourceTypeDO.DataSource,
                resourceName,
                true,
                true,
                uriInfoMock)[0];

        ApplicationInstanceDO usedInApplicationInstance = resourceElement.getUsedInApplication().get(0);

        String expectedApplicationName = application.getName();
        assertEquals(usedInApplicationInstance.getName(), expectedApplicationName);

    }

    private void initializeUriInfo() throws URISyntaxException {
        Mockito.when(uriInfoMock.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri("http://someserver"));
        Mockito.when(uriInfoMock.getBaseUri()).thenReturn(new URI("http://someserver"));
    }

}
