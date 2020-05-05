package no.nav.aura.envconfig.client;

import static no.nav.aura.envconfig.client.ResourceTypeDO.BaseUrl;
import static no.nav.aura.envconfig.client.ResourceTypeDO.Credential;
import static no.nav.aura.envconfig.client.ResourceTypeDO.DB2DataSource;
import static no.nav.aura.envconfig.client.ResourceTypeDO.DataSource;
import static no.nav.aura.envconfig.client.ResourceTypeDO.EmailAddress;
import static no.nav.aura.envconfig.client.ResourceTypeDO.LDAP;
import static no.nav.aura.envconfig.client.ResourceTypeDO.Queue;
import static no.nav.aura.envconfig.client.ResourceTypeDO.QueueManager;
import static no.nav.aura.envconfig.client.ResourceTypeDO.SMTPServer;
import static no.nav.aura.envconfig.client.ResourceTypeDO.WebserviceEndpoint;
import static no.nav.aura.envconfig.client.ResourceTypeDO.findTypeFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import no.nav.aura.appconfig.resource.BaseUrl;
import no.nav.aura.appconfig.resource.Credential;
import no.nav.aura.appconfig.resource.DBTypeAware.DBType;
import no.nav.aura.appconfig.resource.Datasource;
import no.nav.aura.appconfig.resource.EmailAddress;
import no.nav.aura.appconfig.resource.Ldap;
import no.nav.aura.appconfig.resource.Queue;
import no.nav.aura.appconfig.resource.QueueManager;
import no.nav.aura.appconfig.resource.SmtpServer;
import no.nav.aura.appconfig.resource.Webservice;
import no.nav.aura.appconfig.resource.XmlDatasource;

import org.junit.jupiter.api.Test;

public class ResourceTypeDOTest {

    @Test
    public void datasource() {
        assertEquals(DataSource, findTypeFor(new Datasource()));
        assertEquals(DataSource, findTypeFor(new XmlDatasource()));
    }

    @Test
    public void DB2datasource() {
        assertEquals(DB2DataSource, findTypeFor(new Datasource("alias", DBType.DB2, "jndi")));
        assertEquals(DB2DataSource, findTypeFor(new XmlDatasource("alias", DBType.DB2, "jndi")));
    }

    @Test
    public void webservice() {
        assertEquals(WebserviceEndpoint, findTypeFor(new Webservice()));
    }

    @Test
    public void ldap() {
        assertEquals(LDAP, findTypeFor(new Ldap()));
    }

    @Test
    public void baseurl() {
        assertEquals(BaseUrl, findTypeFor(new BaseUrl()));
    }

    @Test
    public void credential() {
        assertEquals(Credential, findTypeFor(new Credential()));
    }

    @Test
    public void queueManager() {
        assertEquals(QueueManager, findTypeFor(new QueueManager()));
    }

    @Test
    public void smtpServer() {
        assertEquals(SMTPServer, findTypeFor(new SmtpServer()));
    }

    @Test
    public void emailAddress() {
        assertEquals(EmailAddress, findTypeFor(new EmailAddress()));
    }

    @Test
    public void queue() {
        assertEquals(Queue, findTypeFor(new Queue()));
    }

    // @Test
    // public void rolemapping() {
    // assertEquals(RoleMapping, findTypeFor(new RoleMapping()));
    // }

}
