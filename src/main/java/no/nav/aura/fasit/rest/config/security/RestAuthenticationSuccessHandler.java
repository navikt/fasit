package no.nav.aura.fasit.rest.config.security;

import java.io.IOException;
import java.io.PrintWriter;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import no.nav.aura.envconfig.spring.User;

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
