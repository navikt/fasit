package no.nav.aura.fasit.rest.config.security;

import no.nav.aura.envconfig.spring.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Copy of {@link org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler} with no redirect
 *
 */
public class RestAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {


    public RestAuthenticationSuccessHandler() {
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response, Authentication authentication)
            throws ServletException, IOException {


        response.setStatus(HttpServletResponse.SC_OK);
        
        PrintWriter writer = response.getWriter();
        writer.write(User.getCurrentUser().getDisplayName() + " is logged in");

       writer.flush();
    }

}
