package no.nav.aura.envconfig.spring;

import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.servlet.Filter;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.orm.hibernate5.HibernateExceptionTranslator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.filter.CharacterEncodingFilter;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.JPAFasitRepository;
import no.nav.aura.envconfig.auditing.MdcEnrichmentFilter;
import no.nav.aura.envconfig.filter.CorsFilter;
import no.nav.aura.envconfig.filter.EntityCommentBindingFilter;
import no.nav.aura.envconfig.filter.HttpMetricFilter;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackageClasses = ApplicationInstanceRepository.class)
public class SpringDomainConfig {
    private static Logger log = LoggerFactory.getLogger(SpringDomainConfig.class);

    @Bean
    @DependsOn("entityManagerFactory")
    FasitRepository getEnvConfigRepository() {
        return new JPAFasitRepository();
    }

    @Bean
    HibernateExceptionTranslator getHibernateExceptionTranslator() {
        return new HibernateExceptionTranslator();
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        //System.out.println("Running LocalContainerEntityManagerFactoryBean. Dialect: " + getHbm2DllAuto());
        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPackagesToScan("no.nav.aura.envconfig.model");

        Properties jpaProperties = new Properties();
//        jpaProperties.setProperty("hibernate.hbm2ddl.auto", getHbm2DllAuto());
//        jpaProperties.setProperty("hibernate.show_sql", "false");
//        jpaProperties.setProperty("hibernate.dialect", getDialect());
        jpaProperties.setProperty("hibernate.cache.use_second_level_cache", "true");
        jpaProperties.setProperty("hibernate.cache.use_query_cache", "true");
        jpaProperties.setProperty("hibernate.cache.region.factory_class", "org.hibernate.cache.jcache.JCacheRegionFactory");
        jpaProperties.setProperty("hibernate.search.default.directory_provider", "filesystem");
        jpaProperties.setProperty("hibernate.search.default.indexBase", "./lucene");
        jpaProperties.setProperty("hibernate.id.new_generator_mappings", "false");

        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        Database databaseType = Database.valueOf(System.getProperty("envconfDB.dbtype", Database.ORACLE.name()).toUpperCase());
        log.info("Database type: " + databaseType);
        jpaVendorAdapter.setGenerateDdl(databaseType == Database.H2);
        jpaVendorAdapter.setDatabase(databaseType);
        jpaVendorAdapter.setShowSql(false);
        factoryBean.setJpaVendorAdapter(jpaVendorAdapter);
        factoryBean.setJpaProperties(jpaProperties);

        return factoryBean;
    }

    @Bean
    @DependsOnDatabaseInitialization
    JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
    	JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
    	jpaTransactionManager.setEntityManagerFactory(entityManagerFactory);
        return jpaTransactionManager;
    }
    
    
    @Bean
    Filter commentBindingFilter() {
		return new EntityCommentBindingFilter();
	}
    
    
    @Bean
    Filter httpMetricFilter() {
		return new HttpMetricFilter();
	}
    
    
    @Bean
    Filter encodingFilter() {
		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setEncoding("UTF-8");
		filter.setForceEncoding(true);
		return filter;
	}
    

    @Bean
    Filter corsFilter() {
        return new CorsFilter();
    }
    
    @Bean
    Filter openEMinViewFilter() {
        return new OpenEntityManagerInViewFilter();
    }

    @Bean
    Filter mdcEnrichmentFilter() {
        return new MdcEnrichmentFilter();
    }

    // To maintain backward compatibility with existing clients, we use the same cookie name as before
    @Bean
    CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("JSESSIONID");
        return serializer;
    }

    
//    private String getDialect() {
//        if ("true".equals(System.getProperty("useH2"))) {
//            return H2Dialect.class.getName();
//        }
//        return Oracle10gDialect.class.getName();
//    }

//    private String getHbm2DllAuto() {
//        if (H2Dialect.class.getName().equals(getDialect())) {
//            log.warn("Updating database using hibernate, This should only be used in test with h2 database");
//            return "update";
//        }
//        return "validate";
//    }

}