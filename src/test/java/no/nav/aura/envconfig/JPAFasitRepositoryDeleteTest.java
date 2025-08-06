package no.nav.aura.envconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collection;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import no.nav.aura.envconfig.model.ModelEntity;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.Cluster;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.ExposedServiceReference;
import no.nav.aura.envconfig.model.infrastructure.Node;
import no.nav.aura.envconfig.model.infrastructure.ResourceReference;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.model.secrets.Secret;
import no.nav.aura.envconfig.spring.SpringUnitTestConfig;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;

@SpringJUnitConfig(classes = {SpringUnitTestConfig.class})
@Transactional
@Rollback
public class JPAFasitRepositoryDeleteTest {

    @Inject
    private FasitRepository repository;

    @Inject
    private ApplicationInstanceRepository appInstanceRepository;
    
    private Environment environmentU;
    private Application applicationHei;
    private Resource resource;

    @BeforeEach
    public void createData() {

        Scope envClassUScope = new Scope(EnvironmentClass.u);
        Resource baseUrl = new Resource("baseUrl", ResourceType.BaseUrl, envClassUScope);
        resource = repository.store(baseUrl);
        environmentU = new Environment("Hei", EnvironmentClass.u);
        Cluster clusterDevillo = new Cluster("Cluster", Domain.Devillo);
        environmentU.addCluster(clusterDevillo);
        applicationHei = repository.store(new Application("Hei", "hei", "no.nav"));
        clusterDevillo.addApplication(applicationHei);
        ApplicationInstance applicationInstance = single(clusterDevillo.getApplicationInstances());
        applicationInstance.getResourceReferences().add(new ResourceReference(resource, null));
        Resource webservice = new Resource("myExposedResourceWS", ResourceType.WebserviceEndpoint, envClassUScope);
        applicationInstance.getExposedServices().add(new ExposedServiceReference(webservice, null));
        Node nodeDevillo = new Node("hosthost.devillo.no", "root", "password");
        environmentU.addNode(clusterDevillo, nodeDevillo);
        environmentU = repository.store(environmentU);
        Environment environmentAdeo = new Environment("prod", EnvironmentClass.p);
        repository.store(environmentAdeo);
    }

    @Test
    public void deleteResource() {
        assertNotNull(reget(resource));
        repository.delete(reget(resource));
        assertGetByIdReturnsNone(Resource.class, resource.getID());
    }

    @SuppressWarnings("unchecked")
    private <T extends ModelEntity> T reget(T t) {
        return (T) repository.getById(t.getClass(), t.getID());
    }


    @Test
    public void deletingClusterDoesNotCascadeToNodeDeletion() {
        Cluster cluster = single(environmentU.getClusters());
        ApplicationInstance applicationInstance = single(cluster.getApplicationInstances());
        Node node = single(cluster.getNodes());
        repository.delete(reget(cluster));
        assertGetByIdReturnsNone(Cluster.class, cluster.getID());
        assertGetByIdReturnsNone(ApplicationInstance.class, applicationInstance.getID());
        assertNotNull(reget(node));
        assertNotNull(reget(applicationHei));
    }

    private void assertGetByIdReturnsNone(Class<? extends ModelEntity> type, Long id) {
        try {
            Assertions.fail("It exists: " + ToStringBuilder.reflectionToString(repository.getById(type, id)));
        } catch (NoResultException e) {
            // OK
        }
    }

    @Test
    public void deletingResourceLeavesResourceReferenceDanglingButIntact() {
        Resource resource = single(repository.findResourcesByExactAlias(new Scope(EnvironmentClass.u), ResourceType.BaseUrl, "baseUrl"));
        ApplicationInstance applicationInstance = single(appInstanceRepository.findApplicationInstancesUsing(resource));
        ResourceReference resourceReference = single(applicationInstance.getResourceReferences());
        repository.delete(reget(resource));
        assertGetByIdReturnsNone(Resource.class, resource.getID());
        ResourceReference updateResourceReference = reget(resourceReference);
        assertNotNull(updateResourceReference);
        // Really liked to see that the resource was null, however hibernate does not seem to agree
        assertGetByIdReturnsNone(Resource.class, updateResourceReference.getResource().getID());
    }

    @Test
    public void deletingDataSourceResourceAnnihilatesEverything() {
        Resource resource = new Resource("res", ResourceType.DataSource, new Scope(EnvironmentClass.u));
        resource.putPropertyAndValidate("username", "sa");
        resource.putSecretAndValidate("password", "myLittleSecret");
        resource = repository.store(resource);
        assertNotNull(reget(resource.getSecrets().get("password")));
        repository.delete(resource);
        assertGetByIdReturnsNone(Secret.class, resource.getSecrets().get("password").getID());
    }

    @Test
    public void deletingExposedQueue() {
        Resource resource = new Resource("queueRes", ResourceType.Queue, new Scope(EnvironmentClass.u));
        resource.putPropertyAndValidate("queueName", "mqQueue");
        resource = repository.store(resource);
        Long queueId = resource.getID();
        ApplicationInstance applicationInstance = appInstanceRepository.findInstanceOfApplicationInEnvironment(applicationHei.getName(), environmentU.getName());
        applicationInstance.getExposedServices().add(new ExposedServiceReference(resource, 1L));
        repository.store(applicationInstance);

        assertEquals(applicationInstance.getID(), repository.findApplicationInstanceByExposedResourceId(queueId).getID(), "applicationinstance has queue as exposed resource");
        repository.delete(resource);
        assertGetByIdReturnsNone(Resource.class, queueId);
        Set<ExposedServiceReference> exposedServices = reget(applicationInstance).getExposedServices();
        assertThat("applicationinstance don't have queue as exposed resource",  exposedServices, Matchers.hasSize(1));
    }


    private <T> T single(Collection<T> ts) {
        assertEquals(1, ts.size());
        return ts.iterator().next();
    }

}
