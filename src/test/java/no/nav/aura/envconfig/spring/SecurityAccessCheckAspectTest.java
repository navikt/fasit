package no.nav.aura.envconfig.spring;

import javax.inject.Inject;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.util.SerializableFunction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
@SuppressWarnings("serial")
public class SecurityAccessCheckAspectTest extends SpringTest {

    @Inject
    private FasitRepository fasitRepository;

    @Test
    public void testNotLoggedInAccessAllowed() {
        fasitRepository.findEnvironmentsBy(EnvironmentClass.u).size();
        // fasitRepository.store(new Environment(Domain.Devillo, new EnvironmentOld("TullBall", EnvironmentClass.u)));
    }

    @Test
    public void testAccessAllowed() {
        runAsUser("user", "user", new SerializableFunction<Void, Void>() {
            public Void process(Void input) {
                fasitRepository.findEnvironmentsBy(EnvironmentClass.u).size();
                return null;
            }
        });
    }

    @Test
    public void testUpdateAllowedAsUser() {
        createEnvironmentWithUser("user", EnvironmentClass.u);
    }

    @Test
    public void testUpdateDisallowedAsUser() {
        Assertions.assertThrows(AccessException.class, () -> {
            createEnvironmentWithUser("user", EnvironmentClass.t);
        });
    }

    @Test
    public void testUpdateAllowedAsAdmin() {
        createEnvironmentWithUser("admin", EnvironmentClass.u);
    }

    @Test
    public void testUpdateDisallowedOnProd() {
        Assertions.assertThrows(AccessException.class, () -> {
            createEnvironmentWithUser("admin", EnvironmentClass.p);
        });
    }

    @Test
    public void testUpdateAllowedOnProd() {
        createEnvironmentWithUser("prodadmin", EnvironmentClass.p);
    }

    private void createEnvironmentWithUser(String user, final EnvironmentClass environmentClass) {
        runAsUser(user, user, new SerializableFunction<Void, Void>() {
            public Void process(Void input) {
                fasitRepository.store(new Environment("Tull", environmentClass));
                return null;
            }
        });
    }

    @Test
    public void testCreateApplication() {
        runAsUser("operation", "operation", new SerializableFunction<Void, Void>() {
            public Void process(Void input) {
                fasitRepository.store(new Application("dill", "no", "tull"));
                return null;
            }
        });
    };

    @Test
    public void testUpdateApplication() {
        Assertions.assertThrows(AccessException.class, () -> {
            updateApplicationWithUser("user");
        });
    }

    @Test
    public void testUpdateApplicationWithAdmin() {
        Assertions.assertThrows(AccessException.class, () -> {
            updateApplicationWithUser("admin");
        });
    }

    @Test
    public void testUpdateApplicationWithProdAdmin() {
        updateApplicationWithUser("prodadmin");
    }

    private void updateApplicationWithUser(String user) {
        runAsUser(user, user, new SerializableFunction<Void, Void>() {
            public Void process(Void input) {
                Application application = (Application) fasitRepository.store(new Application("dill", "no", "tull"));
                application.setName("dall");
                fasitRepository.store(application);
                return null;
            }
        });
    }

    @Test
    public void updateResourceWithUserInU() {
        createAndDeleteResourceAs("user", EnvironmentClass.u);
    }

    @Test
    public void updateResourceInTestWithUserInTShouldFail() {
        Assertions.assertThrows(AccessException.class, () -> {
            createAndDeleteResourceAs("user", EnvironmentClass.t);
        });
    }

    @Test
    public void updateResourceWithOperationInT() {
        createAndDeleteResourceAs("operation", EnvironmentClass.t);
    }

    @Test
    public void updateResourceWithOperationInQ() {
        createAndDeleteResourceAs("operation", EnvironmentClass.q);
    }
    
    @Test
    public void updateResourceWithOperationInP() {
        Assertions.assertThrows(AccessException.class, () -> {
            createAndDeleteResourceAs("operation", EnvironmentClass.p);
        });
    }

    @Test
    public void updateResourceInTestWithProdadminInP() {
        createAndDeleteResourceAs("prodAdmin", EnvironmentClass.p);
    }

    private void createAndDeleteResourceAs(final String user, final EnvironmentClass envClass) {
        runAsUser(user, user, new SerializableFunction<Void, Void>() {
            public Void process(Void input) {
                Resource resource = new Resource("testResource", ResourceType.BaseUrl, new Scope(envClass));
                resource.putPropertyAndValidate("url", "http://someserver.no");
                Resource stored = fasitRepository.store(resource);
                fasitRepository.delete(stored);
                return null;
            }
        });
    }
    
    @Test
    public void updateSecurityOverriddenResourceInResourceProdWithUser() {
    
        runAsUserWithGroup("user", "user","AD-GRUPPE", new SerializableFunction<Void, Void>() {
            public Void process(Void input) {
                Resource resource = new Resource("testResource", ResourceType.BaseUrl, new Scope(EnvironmentClass.p));
                resource.putPropertyAndValidate("url", "http://someserver.no");
                resource.getAccessControl().setAdGroups("AD-GRUPPE");
                Resource stored = fasitRepository.store(resource);
                fasitRepository.delete(stored);
                return null;
            }
        });
    }
   
    @Test
    public void updateSecurityOverriddenResourceInResourceProdWithProdUser() {
    
        runAsUser("prodAdmin", "prodAdmin", new SerializableFunction<Void, Void>() {
            public Void process(Void input) {
                Resource resource = new Resource("testResource", ResourceType.BaseUrl, new Scope(EnvironmentClass.p));
                resource.putPropertyAndValidate("url", "http://someserver.no");
                resource.getAccessControl().setAdGroups("AD-GRUPPE");
                Resource stored = fasitRepository.store(resource);
                fasitRepository.delete(stored);
                return null;
            }
        });
    }
    
    @Test
    public void updateSecurityOverriddenResourceInResourceProdWithUserAndWrongAdGroup() {
        Assertions.assertThrows(AccessException.class, () -> {
            runAsUserWithGroup("user", "user", "AD-WRONG", new SerializableFunction<Void, Void>() {
                public Void process(Void input) {
                    Resource resource = new Resource("testResource", ResourceType.BaseUrl, new Scope(EnvironmentClass.p));
                    resource.putPropertyAndValidate("url", "http://someserver.no");
                    resource.getAccessControl().setAdGroups("AD-GRUPPE");
                    Resource stored = fasitRepository.store(resource);
                    fasitRepository.delete(stored);
                    return null;
                }
            });
        });
    }

}
