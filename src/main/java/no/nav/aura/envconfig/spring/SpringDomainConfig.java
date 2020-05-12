package no.nav.aura.envconfig.spring;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.JPAFasitRepository;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;
import no.nav.aura.sensu.SensuClient;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.Oracle10gDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.orm.hibernate5.HibernateExceptionTranslator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackageClasses = ApplicationInstanceRepository.class)
public class SpringDomainConfig {
    private static Logger log = LoggerFactory.getLogger(SpringDomainConfig.class);

    @Bean
    public FasitRepository getEnvConfigRepository() {
        return new JPAFasitRepository();
    }

    @Bean
    public DataSource dataSource() {
        JndiObjectFactoryBean jndiObjectFactoryBean;
        try {
            jndiObjectFactoryBean = new JndiObjectFactoryBean();
            jndiObjectFactoryBean.setJndiName("java:/jdbc/envconfDB");
            jndiObjectFactoryBean.setExpectedType(DataSource.class);
            jndiObjectFactoryBean.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return (DataSource) jndiObjectFactoryBean.getObject();
    }

    @Bean
    public HibernateExceptionTranslator getHibernateExceptionTranslator() {
        return new HibernateExceptionTranslator();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        System.out.println("Running LocalContainerEntityManagerFactoryBean. Dialect: " + getHbm2DllAuto());
        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(dataSource());
        factoryBean.setPackagesToScan("no.nav.aura.envconfig.model");
        factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties jpaProperties = new Properties();
        jpaProperties.setProperty("hibernate.hbm2ddl.auto", getHbm2DllAuto());
        jpaProperties.setProperty("hibernate.show_sql", "false");
        jpaProperties.setProperty("hibernate.dialect", getDialect());
        jpaProperties.setProperty("hibernate.cache.use_second_level_cache", "true");
        jpaProperties.setProperty("hibernate.cache.use_query_cache", "true");
        jpaProperties.setProperty("hibernate.cache.region.factory_class", "org.hibernate.cache.jcache.JCacheRegionFactory");
        jpaProperties.setProperty("hibernate.search.default.directory_provider", "filesystem");
        jpaProperties.setProperty("hibernate.search.default.indexBase", "./lucene");
        jpaProperties.setProperty("hibernate.id.new_generator_mappings", "false");

        factoryBean.setJpaProperties(jpaProperties);

        return factoryBean;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean
    public SensuClient sensuClient(
            @Value("${environment.name:dev}") String environmentName,
            @Value("${sensu_client_port:3030}") int sensuPort,
            @Value("${sensu_client_host:sensu.nais}") String hostname
    ) {
        return new SensuClient(environmentName, sensuPort, hostname);
    }

    private String getDialect() {
        if ("true".equals(System.getProperty("useH2"))) {
            return H2Dialect.class.getName();
        }
        return Oracle10gDialect.class.getName();
    }

    private String getHbm2DllAuto() {
        if (H2Dialect.class.getName().equals(getDialect())) {
            log.warn("Updating database using hibernate, This should only be used in test with h2 database");
            return "update";
        }
        return "validate";
    }

}