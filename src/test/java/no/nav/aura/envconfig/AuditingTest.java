package no.nav.aura.envconfig;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import no.nav.aura.envconfig.auditing.EntityCommenter;
import no.nav.aura.envconfig.auditing.FasitRevision;
import no.nav.aura.envconfig.model.AdditionalRevisionInfo;
import no.nav.aura.envconfig.model.ModelEntity;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.spring.SpringUnitTestConfig;
import no.nav.aura.envconfig.util.Effect;
import no.nav.aura.envconfig.util.SerializableFunction;
import no.nav.aura.envconfig.util.Tuple;
import org.hibernate.envers.RevisionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = {SpringUnitTestConfig.class})
public class AuditingTest {

    @Inject
    private PlatformTransactionManager platformTransactionManager;

    @Inject
    private FasitRepository repository;

    private TransactionTemplate template;

    @Inject
    private DataSource dataSource;

    @BeforeEach
    public void transactionTemplate() {
        template = new TransactionTemplate(platformTransactionManager);
    }


    @AfterEach
    public void cleanUp() {
        try (Connection connection = dataSource.getConnection()) {
            HashSet<String> tables = getTableSet(connection);

            for (String table : tables) {
                if (table.endsWith("_AUD")) {
                    connection.prepareStatement("delete from " + table).execute();
                }
            }

            if (tables.contains("ADDITIONALREVISIONINFO")) {
                connection.prepareStatement("delete from ADDITIONALREVISIONINFO").execute();
            }
            if (tables.contains("APPLICATION")) {
                connection.prepareStatement("delete from APPLICATION").execute();
            }
            if (tables.contains("RESOURCE_SECRETS")) {
                connection.prepareStatement("delete from RESOURCE_SECRETS").execute();
            }
            if (tables.contains("RESOURCE_PROPERTIES")) {
                connection.prepareStatement("delete from RESOURCE_PROPERTIES").execute();
            }
            if (tables.contains("RESOURCE_TABLE")) {
                connection.prepareStatement("delete from RESOURCE_TABLE").execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static HashSet<String> getTableSet(Connection connection) throws SQLException {
        HashSet<String> tables = Sets.newHashSet();
        ResultSet resultSet = connection.prepareStatement("select table_name from information_schema.tables").executeQuery();
        while (resultSet.next())
            tables.add(resultSet.getString(1));
        return tables;
    }

    @Test
    public void getEntitiesForRevision() {
        final Application entity = template.execute(new TransactionCallback<Application>() {
            public Application doInTransaction(TransactionStatus status) {
                return repository.store(new Application("dull" + new Random().nextLong()));
            }
        });
        template.execute(new TransactionCallback<Void>() {
            public Void doInTransaction(TransactionStatus status) {
                List<Tuple<Long, RevisionType>> revisions = repository.getRevisionsFor(entity.getClass(), entity.getID());
                assertFalse(revisions.isEmpty());
                List<Tuple<Application, RevisionType>> entities = repository.getEntitiesForRevision(Application.class, revisions.get(0).fst);
                assertFalse(entities.isEmpty());
                assertEquals(entity.getID(), entities.get(0).fst.getID());
                assertEquals(RevisionType.ADD, entities.get(0).snd);
                return null;
            }
        });
    }

    @Test
    public void testGetHistory() {
        Application entity = new Application("dull" + new Random().nextLong());
        for (int i = 0; i < 15; i++) {
            entity.setName("dull" + new Random().nextLong());
            entity = repository.store(entity);
        }
        assertEquals(10, checkDescending(0, Long.MAX_VALUE, getHistory(0, 10)).size());
        assertEquals(5, checkDescending(0, Long.MAX_VALUE, getHistory(10, 10)).size());
    }

    private List<? extends AdditionalRevisionInfo<?>> getHistory(int startIdx, int count) {
        return repository.findHistory(null, null, null, startIdx, count, "revision", false);
    }

    @SuppressWarnings("serial")
    @Test
    public void testAddingComment() {
        assertNull(EntityCommenter.getComment());
        String comment = "testfest";
        EntityCommenter.applyComment(comment, new Effect() {
            public void perform() {
                Application entity = new Application("dull" + new Random().nextLong());
                entity.setName("dull" + new Random().nextLong());
                entity = repository.store(entity);
            }
        });
        assertNull(EntityCommenter.getComment());
        List<? extends AdditionalRevisionInfo<?>> history = checkDescending(0, Long.MAX_VALUE, getHistory(0, 10));
        assertEquals(1, history.size());
        assertEquals(comment, history.get(0).getMessage());
    }

    private List<? extends AdditionalRevisionInfo<?>> checkDescending(int index, long last, List<? extends AdditionalRevisionInfo<?>> history) {
        if (index < history.size()) {
            long revision = history.get(index).getRevision();
            assertTrue(last > revision);
            checkDescending(index + 1, revision, history);
        }
        return history;
    }

    @Test
    public void testGetEmptyHistory() {
        assertTrue(repository.getRevisionsFor(Application.class, null).isEmpty());
    }

    @Test
    public void testApplicationAuditing() {
        List<String> artifactIds = ImmutableList.of("testapp-app", "testapp-appc", "testapp-appco", "testapp-appcon", "testapp-appconfi", "testapp-appconfig");

        Application init = repository.store(new Application("testapp", "testapp", "no.nav.testapp"));
        @SuppressWarnings("serial")
        final Application app = fold(artifactIds, init, new SerializableFunction<Tuple<String, Application>, Application>() {
            public Application process(@Nullable final Tuple<String, Application> tuple) {
                return template.execute(new TransactionCallback<Application>() {
                    public Application doInTransaction(TransactionStatus status) {
                        tuple.snd.setArtifactId(tuple.fst);
                        return repository.store(tuple.snd);
                    }
                });
            }
        });

        template.execute(new TransactionCallback<Void>() {
            public Void doInTransaction(TransactionStatus status) {
                List<FasitRevision<Application>> revisionHistory = getHistory(app);
                assertEquals(7, revisionHistory.size());
                assertEquals(Application.class, revisionHistory.get(0).getModifiedEntityType());
                assertEquals("testapp-appc", revisionHistory.get(revisionHistory.size() - 3).getModelEntity().getArtifactId());
                return null;
            }
        });
    }

    @Test
    public void testResourceAuditing() {
        List<String> urls = ImmutableList.of("initial_value", "value", "value2");
        Resource init = new Resource("myAlias", ResourceType.BaseUrl, new Scope(EnvironmentClass.p));
        @SuppressWarnings("serial")
        final Resource resource = fold(urls, init, new SerializableFunction<Tuple<String, Resource>, Resource>() {
            public Resource process(@Nullable final Tuple<String, Resource> tuple) {
                return template.execute(new TransactionCallback<Resource>() {
                    public Resource doInTransaction(TransactionStatus status) {
                        tuple.snd.putPropertyAndValidate("url", tuple.fst);
                        return repository.store(tuple.snd);
                    }
                });
            }
        });
        template.execute(new TransactionCallback<Void>() {
            public Void doInTransaction(TransactionStatus status) {
                Map<FasitRevision<Resource>, Map<String, String>> revisions = getResourceRevisionHistory(resource);
                assertEquals(3, revisions.size());

                for (Map.Entry<FasitRevision<Resource>, Map<String, String>> revision : revisions.entrySet()) {
                    assertEquals(Resource.class, revision.getKey().getModifiedEntityType());
                }

                HashMap<String, String> valueCheck = Maps.newHashMap();
                valueCheck.put("url", "value");
                assertTrue(revisions.containsValue(valueCheck));
                return null;
            }
        });
    }

    private static <S, T> S fold(List<T> ts, S init, Function<Tuple<T, S>, S> function) {
        S current = init;
        for (T t : ts) {
            current = function.apply(Tuple.of(t, current));
        }
        return current;
    }

    @Test
    public void testAuditingWhileModifyingMultipleValues() {
        template.execute(new TransactionCallback<Void>() {
            public Void doInTransaction(TransactionStatus status) {
                Resource resource = new Resource("alias", ResourceType.Datapower, new Scope(EnvironmentClass.u));
                resource.putPropertyAndValidate("adminurl", "http://url.com");
                resource.putPropertyAndValidate("adminweburl", "http://weburl.com");
                resource.putPropertyAndValidate("username", "francisbacon");
                resource.putSecretAndValidate("password", "ubersecret");
                resource = repository.store(resource);
                resource.putPropertyAndValidate("username", "franceisbacon");
                resource.putSecretAndValidate("password", "ub3r53cr37");
                repository.store(resource);
                return null;
            }
        });
    }

    @Test
    public void auditResourceWithSecret() {
        template.execute(new TransactionCallback<Void>() {
            public Void doInTransaction(TransactionStatus status) {
                Resource resource = new Resource("what", ResourceType.Credential, new Scope(EnvironmentClass.u));
                resource.putPropertyAndValidate("username", "maximus");
                resource.putSecretAndValidate("password", "mittpass");
                repository.store(resource);

                Resource resource2 = new Resource("db", ResourceType.DataSource, new Scope(EnvironmentClass.u));
                resource2.putPropertyAndValidate("username", "maximus");
                resource2.putPropertyAndValidate("url", "http://url.com");
                resource2.putPropertyAndValidate("oemEndpoint", "http://url.com");
                resource2.putPropertyAndValidate("onsHosts", "url.com:6200,url1.com:6200");
                resource2.putSecretAndValidate("password", "schmassword");
                repository.store(resource2);
                return null;
            }
        });
    }

    @Transactional
    @Test
    public void testDeleteWithAuditing() {
        Assertions.assertThrows(NoResultException.class, () -> {
            template.execute(new TransactionCallback<Void>() {
                public Void doInTransaction(TransactionStatus status) {
                    Application app = (Application) repository.store(new Application("lapp", "dapp", "no.nav.smapp"));

                    repository.delete(app);
                    repository.getById(Application.class, app.getID());
                    return null;
                }
            });
        });
    }

    private <T extends Resource> Map<FasitRevision<T>, Map<String, String>> getResourceRevisionHistory(final T entity) {
        Map<FasitRevision<T>, Map<String, String>> map = Maps.newHashMap();
        List<FasitRevision<T>> revisions = getHistory(entity);
        for (FasitRevision<T> revision : revisions) {
            map.put(revision, revision.getModelEntity().getProperties());
        }
        return map;
    }

    @SuppressWarnings({ "unchecked", "serial" })
    private <T extends ModelEntity> List<FasitRevision<T>> getHistory(final T entity) {
        return Lists.transform(repository.getRevisionsFor(entity.getClass(), entity.getID()), new SerializableFunction<Tuple<Long, RevisionType>, FasitRevision<T>>() {
            public FasitRevision<T> process(Tuple<Long, RevisionType> tuple) {
                return (FasitRevision<T>) repository.getRevision(entity.getClass(), entity.getID(), tuple.fst);
            }
        });
    }
}
