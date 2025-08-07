package no.nav.aura.envconfig.spring;

import java.util.Collection;
import java.util.Collections;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.TestingAuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.web.cors.CorsConfigurationSource;

import no.nav.aura.envconfig.DataIntegrityRulesEvaluator;
import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.filter.DummyUserLookup;
import no.nav.aura.fasit.rest.config.security.RestAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableAspectJAutoProxy
@Import({ SpringSecurityHandlersConfig.class })
public class SpringSecurityTestConfig {
    
    @Bean
    UserDetailsService inMemoryUserDetails() {
        UserDetails prodAdmin = User.withUsername("prodadmin")
                .password("{noop}prodadmin")
                .roles("USER", "OPERATIONS", "PROD_OPERATIONS")
                .build();
        
        UserDetails user = User.withUsername("user")
                .password("{noop}user")
                .roles("USER")
                .build();
        
        UserDetails operation = User.withUsername("operation")
                .password("{noop}operation")
                .roles("OPERATIONS")
                .build();
        
        UserDetails superuser = User.withUsername("superuser")
                .password("{noop}superuser")
                .roles("OPERATIONS", "SUPERUSER")
                .build();
        
        return new InMemoryUserDetailsManager(prodAdmin, user, operation, superuser);
    }
    
    
    @Bean
    TestingAuthenticationProvider testingAuthProvider() {
        return new TestingAuthenticationProvider();
    }
    
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
                .csrf(csrf -> csrf
                		.disable())
                .authorizeRequests(requests -> requests
                		.dispatcherTypeMatchers(DispatcherType.REQUEST, DispatcherType.ERROR).permitAll() // allow access to error dispatcher and drop redirects
                        .requestMatchers("/conf/secrets/**").authenticated()
                        .requestMatchers("/api/v2/secrets/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/conf/environments/*/applications/*/verify").permitAll()
                        .requestMatchers(HttpMethod.GET, "/conf/**").permitAll()
                        .requestMatchers("/conf/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v2/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/api/v2/**").permitAll()
                        .requestMatchers("/api/v2/**").authenticated()
                        .requestMatchers("/api/**").permitAll())	
                .httpBasic(basic -> basic
                		.authenticationEntryPoint(restEntryPoint))
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
    DummyUserLookup ldapUserLookup() {
        return new DummyUserLookup();
    }
    
    @Bean
    NAVLdapUserDetailsMapper myUserDetails() {
        return new NAVLdapUserDetailsMapper();
    }

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
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
    
    // This is a dummy authentication provider for testing purposes
	@Bean
	AuthenticationProvider ldapAuthProvider() {
	    return new AuthenticationProvider() {
	        @Override
	        public Authentication authenticate(Authentication authentication) {
	            String username = authentication.getName();
	            UserDetailsService userDetailsService = inMemoryUserDetails();
	            
	            try {
	                // Try to load user from in-memory service
	                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
	                return new UsernamePasswordAuthenticationToken(
	                    userDetails, 
	                    authentication.getCredentials(),
	                    userDetails.getAuthorities());
	            } catch (Exception e) {
	                // Default to authenticated with USER role if not found
	                Collection<GrantedAuthority> authorities = 
	                    Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
	                return new UsernamePasswordAuthenticationToken(
	                    username, 
	                    authentication.getCredentials(),
	                    authorities);
	            }
	        }
	
	        @Override
	        public boolean supports(Class<?> authentication) {
	            return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	        }
	    };
	}

}
