package no.nav.aura.envconfig.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import javax.sql.DataSource;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.Node;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.model.secrets.Secret;
import no.nav.aura.envconfig.spring.SpringDomainConfig;
import no.nav.aura.envconfig.spring.SpringOracleUnitTestConfig;
import no.nav.aura.envconfig.util.FlywayUtil;
import no.nav.aura.envconfig.util.TestDatabaseHelper;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

import com.google.common.collect.Lists;

/**
 * Kept only as template for future database refactorings. Usage:
 * 
 * <ol>
 * <li>Keep the database update scripts that you are writing in the same path as test (and only there). The test will first run
 * the scripts in the db/migration/envconfigDB, then data inserts in createData()-method and last it will run the new scripts.
 * <li>Comment out the 'hibernate.hbm2ddl.auto'-property from {@link SpringDomainConfig#entityManagerFactory()} in order to keep
 * hibernate from creating hickups before the new scripts are ran.
 * <li>Write input data, write tests, refactor, run test, repeat if necessary.
 * </ol>
 * 
 * Remove abstract if you want to run this test
 */
@SpringJUnitConfig(classes = {SpringOracleUnitTestConfig.class})
public abstract class MigrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Inject
    private FasitRepository repository;

    @Inject
    private DataSource dataSource;

    @Inject
    private PlatformTransactionManager transactionManager;

    private static BasicDataSource dataSourceToClose;

    @BeforeEach
    public void createData() {
        TestDatabaseHelper.annihilateAndRebuildDatabaseSchema(dataSource);
        insertData();
        String packageName = "/" + getClass().getPackage().getName().replace('.', '/');
        TestDatabaseHelper.updateDatabaseSchema(dataSource, FlywayUtil.DB_MIGRATION_ENVCONF_DB, packageName);
    }

    @BeforeAll
    public static void createDatabase() {
        TestDatabaseHelper.createTemporaryDatabase();
    }

    @AfterAll
    public static void deleteDatabase() throws SQLException {
        if (dataSourceToClose != null) {
            dataSourceToClose.close();
        }
        TestDatabaseHelper.dropTemporaryDatabase();
    }

    private static final Object PK = new Object();

    private void insertData() {
        insertNodeData();
        insertCredentialsResourceData();
        insertCertificateResourceData();
        insertDataSourceResourceData();
        insertLdapResourceData();
    }

    @Test
    public void nodeRefactored() {
        Node node = repository.findNodeBy("hostenmin");
        assertEquals("hostenmin", node.getHostname());
        assertEquals("sa", node.getUsername());
        assertEquals("hemmelig", node.getPassword().getClearTextString());
    }

    private void insertNodeData() {
        Secret secret = Secret.withValueAndAuthLevel("hemmelig", EnvironmentClass.u);
        long loginId = insertRow("Login", "entid", PK, "envClass", EnvironmentClass.u.toString(), "env_domain",
                Domain.Devillo.toString(), "environmentName", "environment", "cred_type", "LOCAL_LINUX", "description",
                "Tull", "username", "sa", "secret", secret.getContent(), "iv", secret.getIV(), "keyId", secret.getKeyId());
        insertRow("Node", "entid", PK, "hostname", "hostenmin", "cpucount", 4, "memorymb", 1024, "deployuser_entid", loginId);
    }

    @Test
    public void credentialResourceRefactored() {
        Collection<Resource> resources = repository.findResourcesByExactAlias(new Scope(EnvironmentClass.u), ResourceType.Credential, "creddy");
        Resource resource = (Resource) getSingle(resources);
        assertEquals("creddy", resource.getAlias());
        assertEquals("hemmeligCreddy", getItem(resource.getSecrets(), "password").getClearTextString());
    }

    private void insertCredentialsResourceData() {
        Secret secret = Secret.withValueAndAuthLevel("hemmeligCreddy", EnvironmentClass.u);
        long resourceId = insertRow("Resource_table", "entid", PK, "resourcetype", "props", "resource_alias", "creddy", "env_domain", null,
                "resource_type", ResourceType.Credential.toString(), "envclass", EnvironmentClass.u.toString(), "environmentname", null);
        insertRow("RESOURCE_SECRETS", "Resource_ENTID", resourceId, "IV", secret.getIV(), "KEYID", secret.getKeyId(), "SECRET", secret.getContent(), "SECRET_KEY", "password");
    }

    @Test
    public void certificateResourceRefactored() {
        Collection<Resource> resources = repository.findResourcesByExactAlias(new Scope(EnvironmentClass.u), ResourceType.Certificate, "certified");
        Resource resource = (Resource) getSingle(resources);
        assertEquals("certified", resource.getAlias());
        assertEquals("hemmeligKPCert", getItem(resource.getSecrets(), "keystorepassword").getClearTextString());
    }

    private void insertCertificateResourceData() {
        Secret secret = Secret.withValueAndAuthLevel("hemmeligKPCert", EnvironmentClass.u);
        long resourceId = insertRow("Resource_table", "entid", PK, "resourcetype", "props", "resource_alias", "certified", "env_domain", null,
                "resource_type", ResourceType.Certificate.toString(), "envclass", EnvironmentClass.u.toString(), "environmentname", null);
        insertRow("RESOURCE_SECRETS", "Resource_ENTID", resourceId, "IV", secret.getIV(), "KEYID", secret.getKeyId(), "SECRET", secret.getContent(), "SECRET_KEY", "keystorepassword");
    }

    @Test
    public void dataSourceResourceRefactored() {
        Resource resource = getSingle(repository.findResourcesByExactAlias(new Scope(EnvironmentClass.u), ResourceType.DataSource, "myDb"));
        assertEquals("myDb", resource.getAlias());
        assertEquals("saHvaBja", getItem(resource.getProperties(), "username"));
        assertEquals("hemmeligDb", getItem(resource.getSecrets(), "password").getClearTextString());
    }

    private void insertDataSourceResourceData() {
        long resourceId = insertRow("Resource_table", "entid", PK, "resourcetype", "props", "resource_alias", "myDb", "env_domain", null,
                "resource_type", ResourceType.DataSource.toString(), "envclass", EnvironmentClass.u.toString(), "environmentname", null);
        Secret secret = Secret.withValueAndAuthLevel("hemmeligDb", EnvironmentClass.u);
        long loginId = insertRow("Login", "entid", PK, "envClass", EnvironmentClass.u.toString(), "env_domain", Domain.Devillo.toString(),
                "environmentName", "environment", "cred_type", "DB", "description", "Tull",
                "username", "saHvaBja", "secret", secret.getContent(), "iv", secret.getIV(), "keyId", secret.getKeyId());
        insertRow("RESOURCE_LOGINS", "RESOURCE_TABLE_ENTID", resourceId, "LOGINS_ENTID", loginId, "LOGIN_KEY", "schemauser");
    }

    @Test
    public void ldapResourceRefactored() {
        Resource resource = getSingle(repository.findResourcesByExactAlias(new Scope(EnvironmentClass.u), ResourceType.LDAP, "dappenMin"));
        assertEquals("dappenMin", resource.getAlias());
        assertEquals("dapUser", getItem(resource.getProperties(), "username"));
        assertEquals("hemmeligLdap", getItem(resource.getSecrets(), "password").getClearTextString());
    }

    private void insertLdapResourceData() {
        long resourceId = insertRow("Resource_table", "entid", PK, "resourcetype", "props", "resource_alias", "dappenMin", "env_domain", null,
                "resource_type", ResourceType.LDAP.toString(), "envclass", EnvironmentClass.u.toString(), "environmentname", null);
        Secret secret = Secret.withValueAndAuthLevel("hemmeligLdap", EnvironmentClass.u);
        long loginId = insertRow("Login", "entid", PK, "envClass", EnvironmentClass.u.toString(), "env_domain", Domain.Devillo.toString(),
                "environmentName", "environment", "cred_type", "LDAP", "description", "Tull",
                "username", "dapUser", "secret", secret.getContent(), "iv", secret.getIV(), "keyId", secret.getKeyId());
        insertRow("RESOURCE_LOGINS", "RESOURCE_TABLE_ENTID", resourceId, "LOGINS_ENTID", loginId, "LOGIN_KEY", "bind");
    }

    private <T> T getItem(Map<String, T> properties, String key) {
        return properties.get(key);
    }

    private <T> T getSingle(Collection<T> ts) {
        assertEquals(1, ts.size());
        return ts.iterator().next();
    }

    private long insertRow(final String table, final Object... os) {
        return new SpringInTransaction<Long>(transactionManager) {
            public Long run() {
                String sqlp1 = "insert into " + table + " (";
                String sqlp2 = " values (";
                List<Object> values = Lists.newArrayList();
                for (int i = 0; i < os.length; i += 2) {
                    sqlp1 += ((String) os[i]) + ", ";
                    Object value = os[i + 1];
                    sqlp2 += (value == PK) ? "hibernate_sequence.nextval, " : "?, ";
                    if (value != PK) {
                        values.add(value);
                    }
                }
                String sql = sqlp1.replaceAll(", *$", "") + ")" + sqlp2.replaceAll(", *$", "") + ")";
                Query query = entityManager.createNativeQuery(sql);
                for (int i = 0; i < values.size(); ++i) {
                    query = query.setParameter(i + 1, values.get(i));
                }
                query.executeUpdate();
                return ((Number) entityManager.createNativeQuery("select hibernate_sequence.currval from dual").getSingleResult()).longValue();
            };
        }.perform();
    }

}
