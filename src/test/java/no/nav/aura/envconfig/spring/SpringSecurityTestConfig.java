package no.nav.aura.envconfig.spring;

import no.nav.aura.envconfig.DataIntegrityRulesEvaluator;
import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.filter.DummyUserLookup;
import no.nav.aura.fasit.rest.config.security.RestAuthenticationSuccessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
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
import org.springframework.security.core.AuthenticationException;
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

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableAspectJAutoProxy
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
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
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
                		.authenticationEntryPoint(restEntryPoint()))
                .csrf(csrf -> csrf
                        .disable())
                .formLogin(login -> login
                        .loginProcessingUrl("/api/login")
                        .successHandler(restLoginSuccessHandler())
                        .failureHandler(restfulAuthenticationFailureHandler()))
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler(restfulLogoutSuccessHandler()))
                .exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(restEntryPoint())
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


    
    @Bean(name = "grantedAuthoritiesMapper")
    AuthoritiesMapper grantedAuthoritiesMapper(Environment env) {
        return new AuthoritiesMapper(env);
    }

    @Bean(name = "restLoginSuccessHandler")
    static RestAuthenticationSuccessHandler restLoginSuccessHandler() {
        return new RestAuthenticationSuccessHandler();
    }

    @Bean(name = "restLoginFailureHandler")
    static SimpleUrlAuthenticationFailureHandler restfulAuthenticationFailureHandler() {
        return new SimpleUrlAuthenticationFailureHandler(){
            @Override
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {

                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                PrintWriter writer = response.getWriter();
                writer.write(exception.getMessage());
                writer.flush();

            }
        };
    }

    @Bean(name = "restLogoutSuccessHandler")
    static SimpleUrlLogoutSuccessHandler restfulLogoutSuccessHandler() {
        return new SimpleUrlLogoutSuccessHandler(){
            @Override
            public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                String refererUrl = request.getHeader("referer");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().flush();
            }
        };

    }


    /**
     * @return 401 i stedet for redirect
     */
    @Bean(name = "restEntryPoint")
    static AuthenticationEntryPoint restEntryPoint(){
        return new AuthenticationEntryPoint() {

            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
                response.setHeader("WWW-Authenticate","Basic realm=\"fasit\"" );
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
            }
        };
    }
    
}
