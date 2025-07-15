package no.nav.aura.fasit.rest.config;

import no.nav.aura.envconfig.filter.LdapUserLookup;
import no.nav.aura.envconfig.spring.AuthoritiesMapper;
import no.nav.aura.fasit.rest.config.security.RestAuthenticationSuccessHandler;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;


@Configuration
@EnableWebSecurity
@EnableAspectJAutoProxy 
//@ImportResource({ "classpath:spring-security-rest.xml" })
//@Import({ SpringSecurityHandlersConfig.class })
public class SpringSecurityRestConfig { 

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
    LdapUserLookup ldapUserLookup(Environment env) {
        return new LdapUserLookup(env);
    }
    
    @Bean(name = "grantedAuthoritiesMapper")
    AuthoritiesMapper grantedAuthoritiesMapper(Environment env) {
        return new AuthoritiesMapper(env);
    }

    @Bean(name = "restLoginSuccessHandler")
    RestAuthenticationSuccessHandler restLoginSuccessHandler() {
        return new RestAuthenticationSuccessHandler();
    }

    @Bean(name = "restLoginFailureHandler")
    SimpleUrlAuthenticationFailureHandler restfulAuthenticationFailureHandler() {
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
    SimpleUrlLogoutSuccessHandler restfulLogoutSuccessHandler() {
        return new SimpleUrlLogoutSuccessHandler(){
            @Override
            public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
//                String refererUrl = request.getHeader("referer");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().flush();
            }
        };

    }


    /**
     * @return 401 i stedet for redirect
     */
    @Bean(name = "restEntryPoint")
	AuthenticationEntryPoint restEntryPoint(){
        return new AuthenticationEntryPoint() {

            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
                response.setHeader("WWW-Authenticate","Basic realm=\"fasit\"" );
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
            }
        };
    }
    
}
