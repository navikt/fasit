package no.nav.aura.fasit.rest.config;

import no.nav.aura.envconfig.filter.LdapUserLookup;
import no.nav.aura.envconfig.spring.SpringSecurityHandlersConfig;
import no.nav.aura.fasit.rest.config.security.RestAuthenticationSuccessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;


//@EnableWebSecurity
@Configuration
@ImportResource({ "classpath:spring-security-rest.xml" })
@Import({ SpringSecurityHandlersConfig.class })
public class SpringSecurityRestConfig { // extends WebSecurityConfigurerAdapter{

    //http://www.codesandnotes.be/2014/10/31/restful-authentication-using-spring-security-on-spring-boot-and-jquery-as-a-web-client/
    //http://www.baeldung.com/2011/10/31/securing-a-restful-web-service-with-spring-security-3-1-part-3/

////    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        http.authorizeRequests()
//                .antMatchers("/secrets/**").authenticated()
//                .antMatchers(HttpMethod.POST, "/login").permitAll()
//                .antMatchers(HttpMethod.GET,"/**" ).permitAll()
//                .anyRequest().authenticated();
//        http.csrf().disable();
////        http.exceptionHandling().authenticationEntryPoint(restEntryPoint());
//        http.formLogin()
//            .successHandler(restfulAuthenticationSuccessHandler())
//            .failureHandler(restfulAuthenticationFailureHandler());
//        http.httpBasic();
//                
//        
//    }
      
      /**
       * @return 200 status instead of a 301 normally sent by a Spring Security form login
       */
      /*
      @Bean(name="restLoginSuccessHandler")
      public SimpleUrlAuthenticationSuccessHandler restfulAuthenticationSuccessHandler() {
          return new RestAuthenticationSuccessHandler();
        
      }
    */

    @Bean
    public LdapUserLookup ldapUserLookup(Environment env) {
        return new LdapUserLookup(env);
    }
    
}
