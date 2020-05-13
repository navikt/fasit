package no.nav.aura.envconfig.spring;

import no.nav.aura.fasit.rest.config.security.RestAuthenticationSuccessHandler;
import no.nav.aura.sensu.SensuClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SpringSecurityHandlersConfig {
    private static final Logger log = LoggerFactory.getLogger(SpringSecurityHandlersConfig.class);

    @Bean(name="grantedAuthoritiesMapper")
    public AuthoritiesMapper grantedAuthoritiesMapper(Environment env) {
        return new AuthoritiesMapper(env);
    }

    @Bean(name="restLoginSuccessHandler")
    public RestAuthenticationSuccessHandler restLoginSuccessHandler(SensuClient sensuClient) {
        return new RestAuthenticationSuccessHandler(sensuClient);
    }

    @Bean(name="restLoginFailureHandler")
    public SimpleUrlAuthenticationFailureHandler restfulAuthenticationFailureHandler() {
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

    @Bean(name="restLogoutSuccessHandler")
    public SimpleUrlLogoutSuccessHandler restfulLogoutSuccessHandler() {
        return new SimpleUrlLogoutSuccessHandler(){
            @Override
            public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                log.debug("restLogoutSuccessHandler logging out");
                String refererUrl = request.getHeader("referer");
                log.debug("Referer url for logout " + refererUrl);
                log.debug("isAuthenticated? " + authentication.isAuthenticated() + " " + authentication.getName() + " " + authentication.toString());
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().flush();
            }
        };

    }


    /**
     * @return 401 i stedet for redirect
     */
    @Bean(name="restEntryPoint")
    public AuthenticationEntryPoint restEntryPoint(){
        return new AuthenticationEntryPoint() {

            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
                response.setHeader("WWW-Authenticate","Basic realm=\"fasit\"" );
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
            }
        };
    }
}
