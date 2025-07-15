package no.nav.aura.envconfig.spring;

import no.nav.aura.envconfig.DataIntegrityRulesEvaluator;
import no.nav.aura.envconfig.FasitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@Configuration
@EnableJdbcHttpSession
@EnableAspectJAutoProxy
public class SpringSecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SpringSecurityConfig.class);

    @Bean
    DataIntegrityRulesEvaluator getDataIntegrityRulesEvaluator(FasitRepository repository) {
        return new DataIntegrityRulesEvaluator(repository);
    }

    @Bean
    SecurityAccessCheckAspect getSecurityAccessCheck() {
        return new SecurityAccessCheckAspect();
    }

    @Bean
    DataIntegrityRulesAspect getDataIntegrityRulesAspect() {
        return new DataIntegrityRulesAspect();
    }

    @Bean
    PerformanceMeasureAspect getPerformanceMeasureAspect() {
        return new PerformanceMeasureAspect();
    }

    @Bean
    ActiveDirectoryLdapAuthenticationProvider ldapAuthProvider(
        @Value("${ldap.domain}") String ldapDomain,
        @Value("${ldap.url}") String ldapUrl,
        GrantedAuthoritiesMapper grantedAuthoritiesMapper) {
    	
    	ActiveDirectoryLdapAuthenticationProvider provider = new ActiveDirectoryLdapAuthenticationProvider(ldapDomain, ldapUrl);
        provider.setAuthoritiesMapper(grantedAuthoritiesMapper);
        provider.setUserDetailsContextMapper(new NAVLdapUserDetailsMapper());
        provider.setUseAuthenticationRequestCredentials(true);
        provider.setConvertSubErrorCodesToExceptions(true);
        provider.setSearchFilter("(&(objectClass=user)(|(sAMAccountName={1})(userPrincipalName={0})(mail={0})))");
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
    	log.info("Creating AuthenticationManager");
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    AuthenticationConfiguration authenticationConfiguration() {
		log.info("Creating AuthenticationConfiguration");
		return new AuthenticationConfiguration();
	}
    
}
