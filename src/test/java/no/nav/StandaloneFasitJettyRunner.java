package no.nav;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;

import jakarta.inject.Inject;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@ComponentScan(basePackages = "no.nav.aura", excludeFilters = {@ComponentScan.Filter(Configuration.class ), @ComponentScan.Filter(SpringBootApplication.class)})
@Import(StandaloneRunnerTestConfig.class)
public class StandaloneFasitJettyRunner implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {

    private final ApplicationContext context;


    @Inject
    public StandaloneFasitJettyRunner(ApplicationContext context) {
        assertNotNull(context, "Context can not be null");
        this.context = context;
//        createTestData();
    }

    @Override
    public void customize(JettyServletWebServerFactory container) {
        container.setPort(1337);
    }

    public static void main(String[] args) {
        // Default value has changed in Spring5, need to allow overriding of beans in tests
//        System.setProperty("spring.main.allow-bean-definition-overriding", "true");

        SpringApplication springApp = new SpringApplication(StandaloneFasitJettyRunner.class);
        springApp.setBannerMode(Banner.Mode.OFF);
        springApp.run(args);
    }
    
}
