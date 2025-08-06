package no.nav.aura.envconfig.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.persistence.PersistenceException;

import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.Cluster;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.Node;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DataIntegrityRulesAspectTest extends SpringTest {

    private Application application;
    private Environment environment;
    private Cluster cluster;

    @BeforeEach
    public void createData() {
        environment = new Environment("bull", EnvironmentClass.u);
        cluster = new Cluster("cluster", Domain.Devillo);
        cluster.addNode(new Node("b02.devillo.no", "user", "password"));
        environment.addCluster(cluster);
        application = repository.store(new Application("hei"));
        cluster = environment.findClusterByName("cluster");
        cluster.addApplication(application);
        environment = repository.store(environment);
    }

    @Test
    public void clusterNameUnique() {
        Cluster newCluster = environment.addCluster(new Cluster("newCluster", Domain.Devillo));
        repository.store(environment);
    }

    @Test
    public void clusterNameNotUnique() {
        Cluster newCluster = environment.addCluster(new Cluster("cluster", Domain.Devillo));
        try {
            repository.store(environment);
            Assertions.fail("cluster " + newCluster.getID());
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("Cluster cluster already exist in environment bull"));
        }
    }

    @Test
    public void environmentNameDomainDuplicate() {
        Assertions.assertThrows(PersistenceException.class, () -> {
            repository.store(new Environment("bull", EnvironmentClass.u));
        });
    }

    @Test
    public void environmentNameDistinct() {
        repository.store(new Environment("heisann", EnvironmentClass.u));
    }

    @Test
    public void environmentNameDuplicateAcrossEnvironmentClass() {
        Assertions.assertThrows(PersistenceException.class, () -> {
            repository.store(new Environment("bull", EnvironmentClass.t));
        });
    }

    @Test
    public void newApplicationNotDistinct() {
        Assertions.assertThrows(PersistenceException.class, () -> {
            repository.store(new Application("hei"));
        });
    }

    @Test
    public void newApplicationDistinct() {
        repository.store(new Application("hopp"));
    }

    @Test
    public void oldApplicationDistinctSameName() {
        Application app = repository.store(new Application("hopp"));
        app.setArtifactId("nyid");
        repository.store(app);
    }

    @Test
    public void oldApplicationDistinctChangedName() {
        Application app = repository.store(new Application("hopp"));
        app.setName("hipp");
        repository.store(app);
    }

    @Test
    public void oldApplicationNonDistinctChangedName() {
        Assertions.assertThrows(PersistenceException.class, () -> {
            Application app = repository.store(new Application("hopp"));
            app.setName("hei");
            repository.store(app);
        });
    }

    @Test
    public void sameApplicationTwiceInEnvironmentNotAllowed() {
        Cluster newCluster = new Cluster("newCluster", Domain.Devillo);
        environment.addCluster(newCluster);
        repository.store(environment);

        newCluster.addApplication(application);
        try {
            environment = repository.store(environment);
            Assertions.fail("cluster " + newCluster.getID());
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("Application hei already exists in environment bull"));
        }
    }

    @Test
    public void applicationInTwoEnvironmentsIsAllowed() {
        Cluster newCluster = new Cluster("newCluster", Domain.Devillo);
        Environment newEnvironment = new Environment("newEnv", EnvironmentClass.u);
        newEnvironment.addCluster(newCluster);
        newCluster.addApplication(application);
        newEnvironment = repository.store(newEnvironment);
    }

    @Test
    public void applicationInstanceShouldBeUpdateable() {
        List<ApplicationInstance> applicationInstances = repository.findApplicationInstancesBy(application);
        assertEquals(1, applicationInstances.size());
        ApplicationInstance applicationInstance = applicationInstances.iterator().next();
        applicationInstance.setVersion("3.14");
        repository.store(applicationInstance);
        assertEquals(1, repository.findApplicationInstancesBy(application).size());
    }

}
