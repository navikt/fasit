package no.nav.aura.envconfig.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import no.nav.StandaloneFasitJettyRunner;
import no.nav.StandaloneRunnerTestConfig;
import no.nav.StandaloneRunnerTestOracleConfig;
import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.auditing.FasitRevision;
import no.nav.aura.envconfig.model.ModelEntity;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.application.ApplicationGroup;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.spring.SecurityByPass;
import no.nav.aura.envconfig.util.InsideJobService;
import no.nav.aura.envconfig.util.Producer;
import no.nav.aura.envconfig.util.Tuple;
import no.nav.aura.fasit.repository.ApplicationRepository;
import no.nav.aura.fasit.repository.EnvironmentRepository;
import no.nav.aura.fasit.repository.NodeRepository;
import no.nav.aura.fasit.repository.ResourceRepository;
import no.nav.aura.fasit.rest.model.EntityPayload;
import org.hibernate.envers.RevisionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = {StandaloneFasitJettyRunner.class, StandaloneRunnerTestConfig.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = {StandaloneFasitJettyRunner.class, StandaloneRunnerTestOracleConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableTransactionManagement
abstract public class RestTest {
    private final static Logger log = LoggerFactory.getLogger(RestTest.class);

    @Inject
    public TestRestTemplate testRestTemplate;
    
    @Inject
    protected FasitRepository unwrappedRepository;

    protected FasitRepository repository;
    
    @Inject
    protected AuthenticationManager authenticationManager;
    
    @Inject
    protected InsideJobService insideJobService;

    @PersistenceContext
	protected EntityManager entityManager;
	
	@Inject
    private ApplicationRepository applicationRepository;

	@Inject
    private EnvironmentRepository environmentRepo;
	
	@Inject
	private ResourceRepository resourceRepository;
	
	@Inject
	NodeRepository nodeRepository;
	
	@Inject
	private ObjectMapper objectMapper;

    @BeforeAll
    public void setUpRestTest() {
    	System.setProperty("spring.main.allow-bean-definition-overriding", "true");
    	
    	// Set up RestAssured to use Jackson 2 for JSON serialization/deserialization
		// This is necessary to ensure that the tests can handle JSON payloads correctly
        System.setProperty("io.restassured.config.ObjectMapperConfig.defaultObjectMapperType", "JACKSON_2");

        RestAssured.port = 1337;
        repository = SecurityByPass.wrapWithByPass(FasitRepository.class, unwrappedRepository);
    }


    @Test
    public void verifyRestTemplate() {
        assertNotNull(testRestTemplate);
    }

    @SuppressWarnings({ "unchecked"})
    protected <T extends ModelEntity> FasitRevision<T> getHeadrevision(final Class<T> entityClass, final long entityid) {
        return insideJobService.produce(new Producer<FasitRevision<T>>() {

            @Override
            public FasitRevision<T> get() {
                List<Tuple<Long, RevisionType>> history = repository.getRevisionsFor(entityClass, entityid);
                if (!history.isEmpty()) {
                    FasitRevision<? extends ModelEntity> revision = repository.getRevision(entityClass, entityid, history.get(0).fst);
                    return (FasitRevision<T>) revision;
                }
                fail("No revision found");
                return null;
            }

        });
    }

    protected void cleanupApplicationGroup() {
		try {
            List<ApplicationGroup> appGroups = entityManager.createQuery(
                "SELECT DISTINCT g FROM ApplicationGroup g LEFT JOIN FETCH g.applications", 
                ApplicationGroup.class).getResultList();
            log.info("Found {} ApplicationGroups to clean up", appGroups.size());
            for (ApplicationGroup group : appGroups) {
                log.info("Cleaning up ApplicationGroup: {}", group.getName());
                Set<Application> apps = new HashSet<>(group.getApplications());
                for (Application app : apps) {
                    log.info("Removing application {} from group {}", app.getName(), group.getName());
                    group.removeApplication(app);
                }
                repository.store(group);
                repository.delete(group);
            }
        	} catch (Exception e) {
        		log.error(e.getMessage());
        	}
	}
	
//    @Transactional
    protected void cleanupApplications() {
        List<Application> applications = applicationRepository.findAll();
        log.info("Found {} applications to clean up", applications.size());
        for (Application app : applications) {
            log.info("Deleting application: {} with id: {}", app.getName(), app.getID());
            Application managedApp = entityManager.find(Application.class, app.getID());
        	repository.delete(managedApp);
        }
	}
    
//    @Transactional
    protected void cleanupResources() {
        resourceRepository.deleteAll();
    }

//    @Transactional
    protected void cleanupEnvironments() {
//    	nodeRepository.deleteAll();
//    	environmentRepo.deleteAll();
    	try {
        List<Environment> environments = environmentRepo.findAll();
//        repository.findEnvironmentBy(null)
        log.info("Found {} environments to clean up", environments.size());
        for (Environment env : environments) {
//        	for (Node node : env.getNodes()) {
//        		env.removeNode(node);
//        		repository.store(env);
//        		repository.delete(node);
//        	}
            repository.delete(env);
        }
//        environmentRepo.flush();
    	} catch (Exception e) {
    		e.printStackTrace();
    		log.error("Error during environment cleanup: {}", e.getMessage());
    	}
    }

    protected <T extends EntityPayload> String toJson(T payload) {
//        Gson gson = new GsonMessageBodyHandler().getGson();
//        return gson.toJson(payload);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing object to JSON", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T extends ModelEntity> FasitRevision<T> getHeadrevision(final T entity) {
        return (FasitRevision<T>) getHeadrevision(entity.getClass(), entity.getID());

    }
}
