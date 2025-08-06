package no.nav.aura.envconfig.rest;

import com.google.common.collect.ImmutableMap;
import no.nav.aura.envconfig.client.ResourceTypeDO;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.spring.SpringTest;
import no.nav.aura.fasit.client.model.ExposedResource;
import no.nav.aura.fasit.client.model.RegisterApplicationInstancePayload;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;
import no.nav.aura.integration.FasitKafkaProducer;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class ApplicationInstanceResourceSpringTest extends SpringTest {

    private ApplicationInstanceResource service;

    @Inject
    private ApplicationInstanceRepository instanceRepository;

    private Environment env;

    private Application app;

    private Cluster cluster;

    private Node node;

    @BeforeEach
    public void setUp() {
        service = new ApplicationInstanceResource(repository, instanceRepository, mock(FasitKafkaProducer.class));
        
        env = repository.store(new Environment("env", EnvironmentClass.p));
        node = repository.store(new Node("hostname.adeo.no", "bleep", "bloop"));

        cluster = new Cluster("cluster", Domain.Adeo);
        cluster.addNode(node);
        app = repository.store(new Application("newApp"));

        cluster.addApplication(app);
        env.addCluster(cluster);

        repository.store(env);
    }
    
    @AfterEach
    public void tearDown() {
    	repository.delete(node);
		repository.delete(env);
		repository.delete(app);
	}

    private ApplicationInstance getAppInstance() {
        return instanceRepository.findInstanceOfApplicationInEnvironment(app.getName(), env.getName());
    }

    @Test
    public void registeringNewInstanceUpdatesFutureReferences() {
        
        ApplicationInstance appInstance = getAppInstance();
        appInstance.getResourceReferences().add(ResourceReference.future("myfutureresource", ResourceType.WebserviceEndpoint));
        repository.store(appInstance);

        // cluster for instance having a exposed resource resolving the future reference
        Cluster otherCluster = new Cluster("otherCluster", Domain.Adeo);
        otherCluster.addNode(node);
        Application otherApp = repository.store(new Application("otherApp"));
        otherCluster.addApplication(otherApp);
        env.addCluster(otherCluster);

        repository.store(env);

        final ResourceReference referenceThatShouldBeFuture = getAppInstance().getResourceReferences().iterator().next();
        assertTrue(referenceThatShouldBeFuture.isFuture());

        service.registerApplicationInstance(no.nav.aura.envconfig.rest.ClasspathResourceHelper.getStringFromFileOnClassPath("/payloads/updatingfutures.json"));
        final ResourceReference referenceThatShouldNotBeFuture = getAppInstance().getResourceReferences().iterator().next();
        assertFalse(referenceThatShouldNotBeFuture.isFuture());
    }

    @Test
    public void registeringNewInstanceExposedResource() {

        RegisterApplicationInstancePayload payload1 = new RegisterApplicationInstancePayload(app.getName(), "1.0", env.getName());
        payload1.setNodes(Arrays.asList(node.getName()));
        Map<String, String> properties = ImmutableMap.of("url", "http://someservice/");
        String alias = "restService";
        ExposedResource exposedResource = new ExposedResource(ResourceTypeDO.RestService, alias, properties);
        exposedResource.setDomain("adeo.no");
        exposedResource.setAccessAdGroups("somegroup");
        payload1.getExposedResources().add(exposedResource);
        RegisterApplicationInstancePayload payload = payload1;

        service.registerApplicationInstance(payload.toJson());
        ApplicationInstance appinst = getAppInstance();

        assertEquals(1, appinst.getExposedServices().size());
        Resource resource = appinst.getExposedServices().iterator().next().getResource();
        assertEquals(ResourceType.RestService, resource.getType());
        assertEquals("restService", resource.getAlias());
        assertEquals("somegroup", resource.getAccessControl().getAdGroups());
        assertEquals("http://someservice/", resource.getProperties().get("url"));
        assertEquals(new Scope(env).domain(Domain.Adeo),resource.getScope());

    }
    
    @Test
    public void changeScopeUpdatesExposedResource() {

        RegisterApplicationInstancePayload payload = new RegisterApplicationInstancePayload(app.getName(), "1.0", env.getName());
        payload.setNodes(Arrays.asList(node.getName()));
        Map<String, String> properties = ImmutableMap.of("url", "http://someservice/");
        String alias = "restService";
        ExposedResource exposedResource = new ExposedResource(ResourceTypeDO.RestService, alias, properties);
        exposedResource.setAccessAdGroups("somegroup");
        payload.getExposedResources().add(exposedResource);

        service.registerApplicationInstance(payload.toJson());
        ApplicationInstance appinst = getAppInstance();

        assertEquals(1, appinst.getExposedServices().size());
        Resource exposed = appinst.getExposedServices().iterator().next().getResource();
        assertEquals(new Scope(env), exposed.getScope());
        payload.setVersion("2.0");
        payload.getExposedResources().get(0).setDomain("adeo.no");;
        service.registerApplicationInstance(payload.toJson());
        
        ApplicationInstance appinst2 = getAppInstance();

        assertEquals("2.0", appinst2.getVersion());
        assertEquals(1, appinst2.getExposedServices().size());
        Resource exposed2 = appinst2.getExposedServices().iterator().next().getResource();
        assertEquals(Domain.Adeo, exposed2.getScope().getDomain());
//        assertEquals(new Scope(env), exposed2.getScope());
     
        
      

    }

    @Test
    public void removedExposedResourcesWillBeDeleted() {

        RegisterApplicationInstancePayload payload = new RegisterApplicationInstancePayload(app.getName(), "1.0", env.getName());
        payload.setNodes(Arrays.asList(node.getName()));
        Map<String, String> properties = ImmutableMap.of("url", "http://someservice/");
        String alias = "restService";
        payload.getExposedResources().add(new ExposedResource(ResourceTypeDO.RestService, alias, properties));

        service.registerApplicationInstance(payload.toJson());
        ApplicationInstance appinst = getAppInstance();

        assertEquals(1, appinst.getExposedServices().size());
//        Resource resource = appinst.getExposedServices().iterator().next().getResource();
        assertThat(repository.findResourcesByExactAlias(new Scope(env), ResourceType.RestService, alias), Matchers.hasSize(1));
        
        // remove exposed from payload
        payload.getExposedResources().clear();
        service.registerApplicationInstance(payload.toJson());
        
        ApplicationInstance appinst2 = getAppInstance();
        assertEquals(0, appinst2.getExposedServices().size());
        assertThat(repository.findResourcesByExactAlias(new Scope(env), ResourceType.RestService, alias), Matchers.hasSize(0));
    }
    
    @Test
    public void removedQueueExposedResourcesWillNotBeDeleted() {
        
        String alias = "myQueue";
        Resource resource = repository.store(new Resource(alias, ResourceType.Queue, new Scope(env)));

        RegisterApplicationInstancePayload payload = new RegisterApplicationInstancePayload(app.getName(), "1.0", env.getName());
        payload.setNodes(Arrays.asList(node.getName()));
        Map<String, String> properties = ImmutableMap.of();
        payload.getExposedResources().add(new ExposedResource(ResourceType.Queue.name(), alias,resource.getID(),  properties));

        service.registerApplicationInstance(payload.toJson());
        ApplicationInstance appinst = getAppInstance();

        assertEquals(1, appinst.getExposedServices().size());
        assertThat(repository.findResourcesByExactAlias(new Scope(env), ResourceType.Queue, alias), Matchers.hasSize(1));
        
        // remove exposed from payload
        payload.getExposedResources().clear();
        service.registerApplicationInstance(payload.toJson());
        
        ApplicationInstance appinst2 = getAppInstance();
        assertEquals(0, appinst2.getExposedServices().size());
        assertThat(repository.findResourcesByExactAlias(new Scope(env), ResourceType.Queue, alias), Matchers.hasSize(1));
    }

}
