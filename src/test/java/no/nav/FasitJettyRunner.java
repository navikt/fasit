package no.nav;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import javax.naming.NamingException;
import javax.sql.DataSource;

import no.nav.aura.envconfig.util.TestDatabaseHelper;
import org.apache.commons.dbcp.BasicDataSource;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.plus.jndi.Resource;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.google.common.collect.Lists;

public class FasitJettyRunner {
    private static final String WEB_SRC = "src/main/webapp";
    private Server server;

    public FasitJettyRunner(int port, DataSource dataSource, String overrideDescriptor) throws IOException {
        server = new Server(port);
        setSystemProperties();
        WebAppContext context = getContext(overrideDescriptor);

        // Respect the X-Forwarded-Proto header
        // Thanks to https://stackoverflow.com/a/28520946
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.addCustomizer(new ForwardedRequestCustomizer());
        HttpConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfig);
        ServerConnector connector = new ServerConnector(server, connectionFactory);
        connector.setPort(port);
        server.setConnectors(new ServerConnector[] { connector });

        // Graceful shutdown
        StatisticsHandler statsHandler = new StatisticsHandler();
        statsHandler.setHandler(context);
        server.setStopTimeout(3000L);
        
        server.setHandler(context);
        try {
            new Resource("java:/jdbc/envconfDB", dataSource);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    public WebAppContext getContext(String overrideDescriptor) {

        final WebAppContext context = new WebAppContext();
        context.setServer(server);
        context.setResourceBase(setupResourceBase());
        Configuration[] configurations = { new AnnotationConfiguration(), new WebXmlConfiguration(), new WebInfConfiguration() };
        context.setConfigurations(configurations);
        if (overrideDescriptor != null) {
            context.setOverrideDescriptors(Lists.newArrayList(overrideDescriptor));
        }
        return context;
    }

    public <T> T getBean(Class<T> clazz) {
        return getSpringContext().getBean(clazz);
    }

    private String setupResourceBase() {
        try {
            File file = new File(getClass().getResource("/logback-test.xml").toURI());
            File projectDirectory = file.getParentFile().getParentFile().getParentFile();
            File webappDir = new File(projectDirectory, WEB_SRC);
            return webappDir.getCanonicalPath();
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException("Could not find webapp directory", e);
        }
    }

    public WebApplicationContext getSpringContext() {
        WebAppContext webApp = (WebAppContext) server.getHandler();
        return WebApplicationContextUtils.getWebApplicationContext(webApp.getServletContext());
    }

    public void start() {
        try {
            server.start();
            System.out.println("Jetty started on port " + getPort());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            server.stop();
            System.out.println("Jetty stopped");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public int getPort() {
        return ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        FasitJettyRunner jetty = new FasitJettyRunner(8088, createDatasourceFromPropertyfile(), null);
        jetty.start();
        jetty.server.join();
    }

    private static DataSource createDatasourceFromPropertyfile() {
        Properties dbProperties = new Properties();
        try {
            File propertyFile = new File(System.getProperty("user.home"), "database.properties");
            if (!propertyFile.exists()) {
                throw new IllegalArgumentException("Propertyfile does not exist " + propertyFile.getAbsolutePath());
            }
            dbProperties.load(new FileInputStream(propertyFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return createDataSource(dbProperties.getProperty("db.type"), dbProperties.getProperty("db.url"), dbProperties.getProperty("db.username"), dbProperties.getProperty("db.password"));
    }

    public static DataSource createDataSource(String type, String url, String username, String password) {
        return TestDatabaseHelper.createDataSource(type, url, username, password);
    }

    private void setSystemProperties() {
        System.setProperty("ldap.url", "ldap://ldapgw.adeo.no");
        System.setProperty("ldap.domain", "adeo.no");
        System.setProperty("ldap.user.basedn", "ou=NAV,ou=BusinessUnits,dc=adeo,dc=no");

        System.setProperty("ldap.username", "binduser");
        System.setProperty("ldap.password", "binduserpasswd");

        System.setProperty("ROLE_OPERATIONS.groups", "0000-GA-env-config-TestAdmin");
        System.setProperty("ROLE_PROD_OPERATIONS.groups", "0000-GA-env-config-ProdAdmin");
        // select encryption keys
        System.setProperty("fasit.encryptionkeys.username", "junit");
        System.setProperty("fasit.encryptionkeys.password", "password");

        // Use for testing Vera integration locally
        System.setProperty("deployLog_v1.url", "http://vera.com/api/v1/deployLog");
        System.setProperty("environment.name", "dev");
        System.setProperty("environment.class", "u");

        // Used for Kafka integration
        System.setProperty("publish.deployment.events.to.kafka", "false");
        System.setProperty("kafka.servers", "localhost:9092");
        System.setProperty("kafka.sasl.enabled", "false");
        System.setProperty("kafka.username", "tull");
        System.setProperty("kafka.password", "fjas");
        System.setProperty("kafka.deployment.event.topic", "deployment-event");

        System.setProperty("vault.url", "https://avaulturl.com");
        System.setProperty("systemuser.srvfasit.username", "srvfasit");
        System.setProperty("systemuser.srvfasit.password", "secret");
    }
}
