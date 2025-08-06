package no.nav.aura.fasit.rest.config;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.web.cors.CorsConfigurationSource;

import no.nav.aura.envconfig.filter.LdapUserLookup;
import no.nav.aura.envconfig.spring.AuthoritiesMapper;
import no.nav.aura.envconfig.spring.NAVLdapUserDetailsMapper;
import no.nav.aura.envconfig.spring.SpringSecurityHandlersConfig;
import no.nav.aura.fasit.rest.config.security.RestAuthenticationSuccessHandler;


@Configuration
@EnableWebSecurity
@EnableAspectJAutoProxy 
//@ImportResource({ "classpath:spring-security-rest.xml" })
@Import({ SpringSecurityHandlersConfig.class })
public class SpringSecurityRestConfig { 

    @Bean
    SecurityFilterChain securityFilterChain(
    		HttpSecurity http,
    		@Autowired CorsConfigurationSource corsConfigurationSource,
    		@Autowired AuthenticationEntryPoint restEntryPoint,
    		@Autowired RestAuthenticationSuccessHandler restLoginSuccessHandler,
    		@Autowired SimpleUrlAuthenticationFailureHandler restfulAuthenticationFailureHandler,
    		@Autowired SimpleUrlLogoutSuccessHandler restfulLogoutSuccessHandler) throws Exception {
        http
        		.cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(management -> management
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeRequests(requests -> requests
                		.dispatcherTypeMatchers(DispatcherType.REQUEST, DispatcherType.ERROR).permitAll() // allow access to error dispatcher and drop redirects
                        .antMatchers("/conf/secrets/**").authenticated()
                        .antMatchers("/api/v2/secrets/**").authenticated()
                        .antMatchers(HttpMethod.PUT, "/conf/environments/*/applications/*/verify").permitAll()
                        .antMatchers(HttpMethod.GET, "/conf/**").permitAll()
                        .antMatchers("/conf/**").authenticated()
                        .antMatchers(HttpMethod.GET, "/api/v2/**").permitAll()
                        .antMatchers(HttpMethod.OPTIONS, "/api/v2/**").permitAll()
                        .antMatchers("/api/v2/**").authenticated()
                        .antMatchers("/api/**").permitAll())	
                .httpBasic(basic -> basic
                		.authenticationEntryPoint(restEntryPoint))
                .csrf(csrf -> csrf
                        .disable())
                .formLogin(login -> login
                        .loginProcessingUrl("/api/login")
                        .successHandler(restLoginSuccessHandler)
                        .failureHandler(restfulAuthenticationFailureHandler))
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler(restfulLogoutSuccessHandler))
                .exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(restEntryPoint)
		                .accessDeniedHandler((request, response, accessDeniedException) -> {
		                    response.sendError(HttpServletResponse.SC_FORBIDDEN, accessDeniedException.getMessage());
		                })
	                );
                
        return http.build();
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
    LdapUserLookup ldapUserLookup(Environment env) {
        return new LdapUserLookup(env);
    }

    @Bean
    NAVLdapUserDetailsMapper myUserDetails() {
        return new NAVLdapUserDetailsMapper();
    }
}
