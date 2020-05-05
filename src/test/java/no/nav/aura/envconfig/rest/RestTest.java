package no.nav.aura.envconfig.rest;

import com.google.gson.Gson;
import io.restassured.RestAssured;
import no.nav.FasitJettyRunner;
import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.auditing.FasitRevision;
import no.nav.aura.envconfig.model.ModelEntity;
import no.nav.aura.envconfig.spring.SecurityByPass;
import no.nav.aura.envconfig.util.InsideJobService;
import no.nav.aura.envconfig.util.Producer;
import no.nav.aura.envconfig.util.Tuple;
import no.nav.aura.fasit.rest.jaxrs.GsonMessageBodyHandler;
import no.nav.aura.fasit.rest.model.EntityPayload;
import org.hibernate.envers.RevisionType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;


abstract public class RestTest {

    protected static FasitJettyRunner jetty;
    protected static FasitRepository repository;
    protected static AuthenticationManager authenticationManager;
    protected static InsideJobService testBean;

    @BeforeAll
    public static void setUpJetty() throws Exception {
        jetty = new FasitJettyRunner(0, FasitJettyRunner.createDataSource("h2", "jdbc:h2:mem:", "sa", ""), "src/test/resources/override-test-web.xml");
        jetty.start();
        RestAssured.port = jetty.getPort();

        authenticationManager = jetty.getSpringContext().getBean("authenticationManager", AuthenticationManager.class);
        repository = SecurityByPass.wrapWithByPass(FasitRepository.class, jetty.getSpringContext().getBean(FasitRepository.class));
        testBean = jetty.getSpringContext().getBean(InsideJobService.class);
    }

    @AfterAll
    public static void close() {
        jetty.stop();
    }

    public <T> T getBean(Class<T> clazz) {
        return jetty.getSpringContext().getBean(clazz);
    }

    @SuppressWarnings({ "unchecked", "serial" })
    protected <T extends ModelEntity> FasitRevision<T> getHeadrevision(final Class<T> entityClass, final long entityid) {
        return testBean.produce(new Producer<FasitRevision<T>>() {

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

    protected <T extends EntityPayload> String toJson(T payload) {
        Gson gson = new GsonMessageBodyHandler().getGson();
        return gson.toJson(payload);
    }

    @SuppressWarnings("unchecked")
    protected <T extends ModelEntity> FasitRevision<T> getHeadrevision(final T entity) {
        return (FasitRevision<T>) getHeadrevision(entity.getClass(), entity.getID());

    }
}
