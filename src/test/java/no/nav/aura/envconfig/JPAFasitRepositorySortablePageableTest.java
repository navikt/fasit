package no.nav.aura.envconfig;

import static java.util.Arrays.asList;
import static no.nav.aura.envconfig.model.infrastructure.Domain.Devillo;
import static no.nav.aura.envconfig.model.infrastructure.Domain.Oera;
import static no.nav.aura.envconfig.model.infrastructure.EnvironmentClass.t;
import static no.nav.aura.envconfig.model.infrastructure.EnvironmentClass.u;
import static no.nav.aura.envconfig.model.resource.ResourceType.BaseUrl;
import static no.nav.aura.envconfig.model.resource.ResourceType.DataSource;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.spring.SpringUnitTestConfig;


@SpringJUnitConfig(classes = {SpringUnitTestConfig.class})
@Transactional
@Rollback
public class JPAFasitRepositorySortablePageableTest {

    @Autowired
    private FasitRepository repository;

    @BeforeEach
    public void setup() throws Exception {

        repository.store(new Resource("dba", DataSource, new Scope(u).domain(Devillo).envName("u1")));
        repository.store(new Resource("dbb", DataSource, new Scope(u).domain(Oera).envName("u1")));
        repository.store(new Resource("url1", BaseUrl, new Scope(u).domain(Devillo).envName("u1")));

        repository.store(new Resource("dba", DataSource, new Scope(t).domain(Devillo).envName("t8")));
        repository.store(new Resource("dbb", DataSource, new Scope(t).domain(Oera).envName("t8")));
        repository.store(new Resource("url1", BaseUrl, new Scope(t).domain(Devillo).envName("t8")));
    }
    
    @AfterEach
    public void tearDown() {
    	List<Resource> resources = repository.findResourcesByLikeAlias(new Scope().envName("t8"), null, null, 0, 10, "alias", true);
    	for (Resource resource : resources) {
			repository.delete(resource);
		}
    	List<Resource> resources2 = repository.findResourcesByLikeAlias(new Scope().envName("u1"), null, null, 0, 10, "alias", true);
    	for (Resource resource : resources2) {
			repository.delete(resource);
    	}
    }

    @Test
    public void findsNumberOfResources() {
        assertThat(repository.findNrOfResources(new Scope(), null, null), is(6L));
        assertThat(repository.findNrOfResources(new Scope().envName("u1"), null, null), is(3L));
    }

    @Test
    public void findsByEnvName() {
        assertThat(repository.findNrOfResources(new Scope().envName("t8"), null, null), is(3L));
        List<Resource> resources = repository.findResourcesByLikeAlias(new Scope().envName("t8"), null, null, 0, 10, "alias", true);
//        assertThat(Lists.transform(resources, AS_ALIAS), equalTo(asList("dba", "dbb", "url1")));
        assertThat(resources.stream().map(input -> input.getAlias()).collect(Collectors.toList()), equalTo(asList("dba", "dbb", "url1")));
//        assertThat(Lists.transform(resources, AS_ENV_NAME), equalTo(asList("t8", "t8", "t8")));
        assertThat(resources.stream().map(input -> input.getScope().getEnvironmentName()).collect(Collectors.toList()), equalTo(asList("t8", "t8", "t8")));
    }

    @Test
    public void findsByPartialAlias() {
        assertThat(repository.findNrOfResources(new Scope(), null, "db"), is(4L));
        List<Resource> resources = repository.findResourcesByLikeAlias(new Scope(), null, "db", 0, 10, "alias", true);
        assertThat(resources.stream().map(input -> input.getAlias()).collect(Collectors.toList()), equalTo(asList("dba", "dba", "dbb", "dbb")));

    }

    @Test
    public void sortsByAlias() {
        // Sorts ascending
        List<Resource> resources = repository.findResourcesByLikeAlias(new Scope(), null, null, 0, 10, "alias", true);
        List<String> aliases = resources.stream().map(input -> input.getAlias()).collect(Collectors.toList());
        
        assertThat(aliases, equalTo(asList("dba", "dba", "dbb", "dbb", "url1", "url1")));

        // Sorts descending
        resources = repository.findResourcesByLikeAlias(new Scope(), null, null, 0, 10, "alias", false);
        aliases = resources.stream().map(input -> input.getAlias()).collect(Collectors.toList());;
        assertThat(aliases, equalTo(asList("url1", "url1", "dbb", "dbb", "dba", "dba")));

        // Returns the first three
        resources = repository.findResourcesByLikeAlias(new Scope(), null, null, 0, 3, "alias", true);
        aliases = resources.stream().map(input -> input.getAlias()).collect(Collectors.toList());;
        assertThat(aliases, equalTo(asList("dba", "dba", "dbb")));

        // Returns the last three
        resources = repository.findResourcesByLikeAlias(new Scope(), null, null, 0, 3, "alias", false);
        aliases = resources.stream().map(input -> input.getAlias()).collect(Collectors.toList());;
        assertThat(aliases, equalTo(asList("url1", "url1", "dbb")));
    }

    @Test
    public void sortsByResourceType() {
        // Sorts ascending
        List<Resource> resources = repository.findResourcesByLikeAlias(new Scope(), null, null, 0, 10, "type", true);
        List<ResourceType> aliases = resources.stream().map(input -> input.getType()).collect(Collectors.toList());
        assertThat(aliases, equalTo(asList(BaseUrl, BaseUrl, DataSource, DataSource, DataSource, DataSource)));

        // Sorts descending
        resources = repository.findResourcesByLikeAlias(new Scope(), null, null, 0, 10, "type", false);
        aliases = resources.stream().map(input -> input.getType()).collect(Collectors.toList());
        assertThat(aliases, equalTo(asList(DataSource, DataSource, DataSource, DataSource, BaseUrl, BaseUrl)));

        // Returns the first three
        resources = repository.findResourcesByLikeAlias(new Scope(), null, null, 0, 3, "type", true);
        aliases = resources.stream().map(input -> input.getType()).collect(Collectors.toList());
        assertThat(aliases, equalTo(asList(BaseUrl, BaseUrl, DataSource)));

        // Returns the last three
        resources = repository.findResourcesByLikeAlias(new Scope(), null, null, 0, 3, "type", false);
        aliases = resources.stream().map(input -> input.getType()).collect(Collectors.toList());
        assertThat(aliases, equalTo(asList(DataSource, DataSource, DataSource)));
    }

    @Test
    public void sortsByResourceEnvironmentClass() {
        // Sorts ascending
        List<Resource> resources = repository.findResourcesByLikeAlias(new Scope(), null, null, 0, 10, "scope.envClass", true);
        List<EnvironmentClass> aliases = resources.stream().map(input -> input.getScope().getEnvClass()).collect(Collectors.toList());
        assertThat(aliases, equalTo(asList(t, t, t, u, u, u)));

        // Sorts descending
        resources = repository.findResourcesByLikeAlias(new Scope(), null, null, 0, 10, "scope.envClass", false);
        aliases = resources.stream().map(input -> input.getScope().getEnvClass()).collect(Collectors.toList());
        assertThat(aliases, equalTo(asList(u, u, u, t, t, t)));

        // Returns the first three
        resources = repository.findResourcesByLikeAlias(new Scope(), null, null, 0, 3, "scope.envClass", true);
        aliases = resources.stream().map(input -> input.getScope().getEnvClass()).collect(Collectors.toList());
        assertThat(aliases, equalTo(asList(t, t, t)));

        // Returns the last three
        resources = repository.findResourcesByLikeAlias(new Scope(), null, null, 0, 3, "scope.envClass", false);
        aliases = resources.stream().map(input -> input.getScope().getEnvClass()).collect(Collectors.toList());
        assertThat(aliases, equalTo(asList(u, u, u)));
    }

    @Test
    public void sortsByResourceEnvironmentName() {
        // Sorts ascending
        List<Resource> resources = repository.findResourcesByLikeAlias(new Scope(), null, null, 0, 10, "scope.environmentName", true);
        List<String> aliases = resources.stream().map(input -> input.getScope().getEnvironmentName()).collect(Collectors.toList());
        assertThat(aliases, equalTo(asList("t8", "t8", "t8", "u1", "u1", "u1")));

        // Sorts descending
        resources = repository.findResourcesByLikeAlias(new Scope(), null, null, 0, 10, "scope.environmentName", false);
        aliases = resources.stream().map(input -> input.getScope().getEnvironmentName()).collect(Collectors.toList());
        assertThat(aliases, equalTo(asList("u1", "u1", "u1", "t8", "t8", "t8")));

        // Returns the first three
        resources = repository.findResourcesByLikeAlias(new Scope(), null, null, 0, 3, "scope.environmentName", true);
        aliases = resources.stream().map(input -> input.getScope().getEnvironmentName()).collect(Collectors.toList());
        assertThat(aliases, equalTo(asList("t8", "t8", "t8")));

        // Returns the last three
        resources = repository.findResourcesByLikeAlias(new Scope(), null, null, 0, 3, "scope.environmentName", false);
        aliases = resources.stream().map(input -> input.getScope().getEnvironmentName()).collect(Collectors.toList());
        assertThat(aliases, equalTo(asList("u1", "u1", "u1")));
    }

    @Test
    public void sortsByResourceDomain() {
        // Sorts ascending
        List<Resource> resources = repository.findResourcesByLikeAlias(new Scope(), null, null, 0, 10, "scope.domain", true);
        List<Domain> aliases = resources.stream().map(input -> input.getScope().getDomain()).collect(Collectors.toList());
        assertThat(aliases, equalTo(asList(Devillo, Devillo, Devillo, Devillo, Oera, Oera)));

        // Sorts descending
        resources = repository.findResourcesByLikeAlias(new Scope(), null, null, 0, 10, "scope.domain", false);
        aliases = resources.stream().map(input -> input.getScope().getDomain()).collect(Collectors.toList());
        assertThat(aliases, equalTo(asList(Oera, Oera, Devillo, Devillo, Devillo, Devillo)));

        // Returns the first three
        resources = repository.findResourcesByLikeAlias(new Scope(), null, null, 0, 3, "scope.domain", true);
        aliases = resources.stream().map(input -> input.getScope().getDomain()).collect(Collectors.toList());
        assertThat(aliases, equalTo(asList(Devillo, Devillo, Devillo)));

        // Returns the last three
        resources = repository.findResourcesByLikeAlias(new Scope(), null, null, 0, 3, "scope.domain", false);
        aliases = resources.stream().map(input -> input.getScope().getDomain()).collect(Collectors.toList());
        assertThat(aliases, equalTo(asList(Oera, Oera, Devillo)));
    }
}