import no.nav.aura.envconfig.util.FlywayUtil;
import org.apache.commons.dbcp.BasicDataSource;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.plus.jndi.Resource;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Properties;

public class ServerMain {
    private static final String WEB_SRC = "src/main/webapp";
    private Server server;

    public ServerMain(int port, DataSource dataSource) throws IOException {
        server = new Server(port);

        WebAppContext context = getContext();

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

    public WebAppContext getContext() {

        final WebAppContext context = new WebAppContext();

        ProtectionDomain domain = ServerMain.class.getProtectionDomain();
        URL location = domain.getCodeSource().getLocation();

        context.setContextPath("/");
        String descriptor = "src/main/webapp/WEB-INF/web.xml";
        System.out.println("Descriptor: " + descriptor);
        context.setDescriptor(descriptor);
        context.setServer(server);
        context.setWar(location.toExternalForm());

        context.setServer(server);
        // context.setResourceBase(setupResourceBase());
        Configuration[] configurations = { new AnnotationConfiguration(), new WebXmlConfiguration(), new WebInfConfiguration() };
        context.setConfigurations(configurations);
        return context;
    }

    public <T> T getBean(Class<T> clazz) {
        return getSpringContext().getBean(clazz);
    }

    private String setupResourceBase() {
        return System.getProperty("user.dir") + "src/main/webapp";
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
        System.out.println("Starting fasit...");
        setSystemProperties();
        ServerMain jetty = new ServerMain(8080, createDatasource());
        jetty.start();
        jetty.server.join();
    }

    public static DataSource createDatasource() {
        String url = System.getProperty("envconfDB.url");
        String username = System.getProperty("envconfDB.username");
        String password = System.getProperty("envconfDB.password");

        System.setProperty("hibernate.default_schema", username);
        BasicDataSource ds = new BasicDataSource();
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setMaxWait(20000);
        System.out.println("using database " + ds.getUsername() + "@" + ds.getUrl());
        FlywayUtil.migrateFlyway(ds);
        return ds;
    }

    private static void setSystemProperties() {
        //System.setProperty("environment.name", "dev");
        //System.setProperty("environment.class", "u");
      File configFile = new File(System.getProperty("fasit.configDir") + "/" + "config.properties");
        if (configFile.isFile()) {
            try {
                Properties props = new Properties();
                props.load(new FileInputStream(configFile));
                props.entrySet().forEach(entry -> {
                    System.setProperty(entry.getKey().toString(), entry.getValue().toString());
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
