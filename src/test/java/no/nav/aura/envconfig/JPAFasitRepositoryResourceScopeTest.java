package no.nav.aura.envconfig;

import static no.nav.aura.envconfig.model.infrastructure.Domain.Devillo;
import static no.nav.aura.envconfig.model.infrastructure.EnvironmentClass.u;
import static no.nav.aura.envconfig.model.resource.ResourceType.DataSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.spring.SpringUnitTestConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SpringUnitTestConfig.class })
@Transactional
@Rollback
public class JPAFasitRepositoryResourceScopeTest {

    @Autowired
    private FasitRepository repository;

    private Application app1;
    private Application app2;
    private Application otherapp;

    @BeforeEach
    public void setup() throws Exception {

        app1 = repository.store(new Application("app1"));
        app2 = repository.store(new Application("app2"));
        otherapp = repository.store(new Application("otherapp"));

        repository.store(new Resource("mydb", DataSource, new Scope(u).domain(Devillo).envName("u1").application(app1)));
        repository.store(new Resource("mydb_withaliasliketheotherone", DataSource, new Scope(u).domain(Devillo).envName("u1").application(app1)));
        repository.store(new Resource("mydb", DataSource, new Scope(u).domain(Devillo).envName("u2").application(app1)));
        repository.store(new Resource("mydb", DataSource, new Scope(u).domain(Devillo).envName("u2").application(app1)));
        repository.store(new Resource("mydb", DataSource, new Scope(u).domain(Devillo).envName("u2").application(app2)));
        repository.store(new Resource("mydb", DataSource, new Scope(u).domain(Devillo).envName("u3").application(app1)));
        repository.store(new Resource("mydb", DataSource, new Scope(u).domain(Devillo).envName("u3")));
    }

    @Test
    public void find_u1() {
        assertEquals(1, repository.findResourcesByExactAlias(new Scope(u).domain(Devillo).envName("u1").application(app1), DataSource, "mydb").size());
        assertEquals(0, repository.findResourcesByExactAlias(new Scope(u).domain(Devillo).envName("u1").application(otherapp), DataSource, "mydb").size());
    }

    @Test
    public void find_u2() {
        // assertEquals(1, repository.findResources(new Scope(u).domain(Devillo).envName("u2").application(app1), DataSource,
        // "mydb").size());
        assertEquals(1, repository.findResourcesByExactAlias(new Scope(u).domain(Devillo).envName("u2").application(app2), DataSource, "mydb").size());
        assertEquals(0, repository.findResourcesByExactAlias(new Scope(u).domain(Devillo).envName("u2").application(otherapp), DataSource, "mydb").size());
    }

    @Test
    public void find_u3() {
        assertEquals(2, repository.findResourcesByExactAlias(new Scope(u).domain(Devillo).envName("u3").application(app1), DataSource, "mydb").size());
        assertEquals(1, repository.findResourcesByExactAlias(new Scope(u).domain(Devillo).envName("u3").application(otherapp), DataSource, "mydb").size());
    }

}