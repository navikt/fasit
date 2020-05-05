package no.nav.aura.envconfig.model.resource;

import static no.nav.aura.envconfig.model.infrastructure.Domain.Devillo;
import static no.nav.aura.envconfig.model.infrastructure.Domain.OeraT;
import static no.nav.aura.envconfig.model.infrastructure.EnvironmentClass.t;
import static no.nav.aura.envconfig.model.infrastructure.EnvironmentClass.u;
import static org.junit.jupiter.api.Assertions.assertEquals;
import no.nav.aura.envconfig.model.application.Application;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

public class ScopeableBestMatchTest {

    private static final String ENVNAME = "u1";

    private static final Application APPLICATION = new Application("myapp");

    private Resource resource_envclass = resource("class", new Scope(u));
    private Resource resource_envclass_domain = resource("class,domain", new Scope(u).domain(Devillo));
    private Resource resource_envclass_name = resource("class,name", new Scope(u).envName(ENVNAME));
    private Resource resource_envclass_app = resource("class_app", new Scope(u).application(APPLICATION));
    private Resource resource_envclass_domain_envname = resource("class,domain,name", new Scope(u).domain(Devillo).envName(ENVNAME));
    private Resource resource_envclass_domain_app = resource("class,domain,app", new Scope(u).domain(Devillo).application(APPLICATION));
    private Resource resource_envclass_name_multiapp = resource("u,name, app", new Scope(u).envName(ENVNAME).application(APPLICATION));
    private Resource resource_fullscope = resource("all", new Scope(u).domain(Devillo).envName(ENVNAME).application(APPLICATION));
    private Resource resource_illegal = resource("illegal", new Scope(t).domain(OeraT).envName("t8").application(APPLICATION));

    private Scope completeScope = new Scope(u).domain(Devillo).envName(ENVNAME).application(APPLICATION);

    private Resource resource(String alias, Scope scope) {
        Resource resource = new Resource(alias, ResourceType.DataSource, scope);
        resource.putPropertyAndValidate("url", "http:url");
        resource.putPropertyAndValidate("username", "user");
        resource.putSecretAndValidate("password", "secret");
        return resource;
    }

    @BeforeAll
    public static void setup() {
        System.out.println("setup");
        System.setProperty("fasit.encryptionkeys.username", "junit");
        System.setProperty("fasit.encryptionkeys.password", "password");
    }

    @Test
    public void verifyBestMatch() {
        Resource bestMatch = completeScope.singleBestMatch(Sets.newHashSet(resource_envclass, resource_envclass_domain, resource_envclass_name, resource_envclass_app, resource_envclass_domain_envname,
                resource_envclass_domain_app
                , resource_envclass_name_multiapp, resource_fullscope));
        assertEquals(resource_fullscope, bestMatch);
    }

    @Test
    public void verifyBestMatchOtherOrder() {
        Resource bestMatch = completeScope.singleBestMatch(Sets.newHashSet(resource_fullscope, resource_envclass_domain, resource_envclass_name, resource_envclass_app,
                resource_envclass_domain_envname, resource_envclass_domain_app, resource_envclass, resource_envclass_name_multiapp));
        assertEquals(resource_fullscope, bestMatch);
    }

    @Test
    public void verifyNoFullMatch() {
        Scope completeScope = new Scope(u).domain(Devillo).envName(ENVNAME).application(APPLICATION);
        Resource bestMatch = completeScope.singleBestMatch(Sets.newHashSet(resource_envclass, resource_envclass_domain, resource_envclass_name, resource_envclass_app, resource_envclass_domain_envname,
                resource_envclass_domain_app, resource_envclass_name_multiapp));
        assertEquals(resource_envclass_name_multiapp, bestMatch);
    }

    @Test
    public void singleElementCollection() {
        Scope searchScope = new Scope(u).domain(Devillo).envName(ENVNAME).application(APPLICATION);
        Resource bestMatch = searchScope.singleBestMatch(Sets.newHashSet(resource_envclass));
        assertEquals(resource_envclass, bestMatch);
    }

    @Test
    public void verifyDomainOverClass() {
        Scope completeScope = new Scope(u).domain(Devillo).envName(ENVNAME).application(APPLICATION);
        Resource bestMatch = completeScope.singleBestMatch(Sets.newHashSet(resource_envclass, resource_envclass_domain));
        assertEquals(resource_envclass_domain, bestMatch);
    }

    @Test
    public void verifyEnvNameOverClass() {
        Scope completeScope = new Scope(u).domain(Devillo).envName(ENVNAME).application(APPLICATION);
        Resource bestMatch = completeScope.singleBestMatch(Sets.newHashSet(resource_envclass_name, resource_envclass));
        assertEquals(resource_envclass_name, bestMatch);
    }

    @Test
    public void illegalScopeInInput() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Scope completeScope = new Scope(u).domain(Devillo).envName(ENVNAME).application(APPLICATION);
            completeScope.singleBestMatch(Sets.newHashSet(resource_envclass_name, resource_illegal));
        });
    }

    // @Test(expected = ResourceNotFoundException.class)
    // public void bestMatchingResource_foundNon() {
    // repository.findBestMatchingResourceForScope(new Scope(u).domain( Devillo).envName(ENVNAME).application(appsomething),
    // DataSource, "mydb");
    // }
    //
    // @Test(expected = ResourceConflictException.class)
    // public void bestMatchingResource_foundMultiple() {
    // repository.findBestMatchingResourceForScope(new Scope(u).domain( Devillo).envName("u2").application(app1), DataSource,
    // "mydb");
    // }

}