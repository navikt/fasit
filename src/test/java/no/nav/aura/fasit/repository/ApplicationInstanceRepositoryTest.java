package no.nav.aura.fasit.repository;

import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.spring.SpringUnitTestConfig;
import no.nav.aura.fasit.repository.specs.ApplicationInstanceSpecs;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import jakarta.inject.Inject;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringJUnitConfig(classes = {SpringUnitTestConfig.class})
@Transactional
public class ApplicationInstanceRepositoryTest {

    @Inject
    private ApplicationInstanceRepository applicationInstanceRepository;

    @Inject
    private EnvironmentRepository environmentRepository;

    @Inject
    private ApplicationRepository applicationRepository;
    @Inject
    private ResourceRepository resourceRepository;

    private Application tsys;
    private Environment t1;

    @BeforeEach
    public void setup() throws Exception {
        String appName = "tsys";

        tsys = applicationRepository.save(new Application(appName));
        Application gosys = applicationRepository.save(new Application("gosys"));
        Application fasit = applicationRepository.save(new Application("fasit"));

        Environment u1 = new Environment("u1", EnvironmentClass.u);
        Environment q1 = new Environment("q1", EnvironmentClass.q);
        t1 = new Environment("t1", EnvironmentClass.t);
        Environment t2 = new Environment("t2", EnvironmentClass.t);

        addCluster(t1, tsys);
        addCluster(u1, tsys, gosys, fasit);
        addCluster(q1, tsys, gosys, fasit);
        addCluster(t2, gosys, fasit);
    }

    private void addCluster(Environment environment, Application... apps) {
        Domain domain = Domain.getByEnvironmentClass(environment.getEnvClass()).get(0);
        Cluster clusterTestLocal = new Cluster(apps[0].getName() + "Cluster", domain);
        environment.addCluster(clusterTestLocal);
        for (Application application : apps) {
            clusterTestLocal.addApplication(application);
        }
        environment = environmentRepository.save(environment);
    }

    @Test
    public void returnsCorrectInstanceOfApplicationInEnvironment() {
        ApplicationInstance instance = applicationInstanceRepository.findInstanceOfApplicationInEnvironment("tSyS", "T1");
        assertEquals("tsys", instance.getApplication().getName());
    }

    @Test
    public void findByApplicationSpec() {
        assertThat(applicationInstanceRepository.findAll(ApplicationInstanceSpecs.findByApplicationName("tsys")), Matchers.hasSize(3));
        assertThat(applicationInstanceRepository.findAll(ApplicationInstanceSpecs.findByApplicationName("gosys")), Matchers.hasSize(3));
        assertThat(applicationInstanceRepository.findAll(ApplicationInstanceSpecs.findByApplicationName("fasit")), Matchers.hasSize(3));
        assertThat(applicationInstanceRepository.findAll(ApplicationInstanceSpecs.findByApplicationName("unknown")), Matchers.hasSize(0));
    }

    @Test
    public void findByEnvironmentSpec() {
        assertThat(applicationInstanceRepository.findAll(ApplicationInstanceSpecs.findByEnvironment("t2")), Matchers.hasSize(2));
        assertThat(applicationInstanceRepository.findAll(ApplicationInstanceSpecs.findByEnvironment("t1")), Matchers.hasSize(1));
        assertThat(applicationInstanceRepository.findAll(ApplicationInstanceSpecs.findByEnvironment("q1")), Matchers.hasSize(3));
        assertThat(applicationInstanceRepository.findAll(ApplicationInstanceSpecs.findByEnvironment("U1")), Matchers.hasSize(3));
        assertThat(applicationInstanceRepository.findAll(ApplicationInstanceSpecs.findByEnvironment("unknown")), Matchers.hasSize(0));
    }

    @Test
    public void findByEnvironmentClassSpec() {
        assertThat(applicationInstanceRepository.findAll(ApplicationInstanceSpecs.findByEnvironmentClass(EnvironmentClass.u)), Matchers.hasSize(3));
        assertThat(applicationInstanceRepository.findAll(ApplicationInstanceSpecs.findByEnvironmentClass(EnvironmentClass.t)), Matchers.hasSize(3));
        assertThat(applicationInstanceRepository.findAll(ApplicationInstanceSpecs.findByEnvironmentClass(EnvironmentClass.q)), Matchers.hasSize(3));
        assertThat(applicationInstanceRepository.findAll(ApplicationInstanceSpecs.findByEnvironmentClass(EnvironmentClass.p)), Matchers.hasSize(0));
    }

    @Test
    public void findEnvironmentForInstance() {
        ApplicationInstance fasitq1 = getInstance("q1", "fasit");
        assertThat(applicationInstanceRepository.findEnvironmentWith(fasitq1).getName(), equalTo("q1"));
        ApplicationInstance fasitu1 = getInstance("u1", "fasit");
        assertThat(applicationInstanceRepository.findEnvironmentWith(fasitu1).getName(), equalTo("u1"));
    }

    private ApplicationInstance getInstance(String environment, String app) {
        ApplicationInstance instance = applicationInstanceRepository.findOne(ApplicationInstanceSpecs.find(environment, null, app, null)).orElse(null);
        assertThat(instance, notNullValue());
        return instance;
    }

    @Test
    public void returnsNullWhenInstanceOfApplicationInEnvironmentIsNotFound() {
        assertNull(applicationInstanceRepository.findInstanceOfApplicationInEnvironment("foo", "pub"));
    }

    @Test
    public void findApplicationInstancesByResource() {
        Resource resource = resourceRepository.save(new Resource("db", ResourceType.DataSource, new Scope(EnvironmentClass.u)));
        assertEquals(0, applicationInstanceRepository.findApplicationInstancesUsing(resource).size());
        ApplicationInstance applicationInstance = applicationInstanceRepository.findInstanceOfApplicationInEnvironment("tsys", "t1");
        applicationInstance.getResourceReferences().add(new ResourceReference(resource, null));
        applicationInstance = applicationInstanceRepository.save(applicationInstance);
        List<ApplicationInstance> applicationInstances = applicationInstanceRepository.findApplicationInstancesUsing(resource);
        assertEquals(1, applicationInstances.size());
        assertEquals(applicationInstance.getID(), applicationInstances.iterator().next().getID());
    }

    @Test
    public void countApplicationInstancesByResource() {
        Resource resource = resourceRepository.save(new Resource("db", ResourceType.DataSource, new Scope(EnvironmentClass.u)));
        assertEquals(0, applicationInstanceRepository.countApplicationInstancesUsing(resource));
        ApplicationInstance applicationInstance = applicationInstanceRepository.findInstanceOfApplicationInEnvironment("tsys", "t1");
        applicationInstance.getResourceReferences().add(new ResourceReference(resource, null));
        applicationInstanceRepository.save(applicationInstance);
        assertEquals(1, applicationInstanceRepository.countApplicationInstancesUsing(resource));
    }

}
