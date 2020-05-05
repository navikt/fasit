package no.nav.aura.fasit.rest.config.security;

import com.google.common.collect.ImmutableMap;
import no.nav.aura.envconfig.spring.User;
import no.nav.aura.sensu.SensuClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;

/**
 * Copy of {@link org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler} with no redirect
 *
 */
public class RestAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final SensuClient sensuClient;

    @Autowired
    public RestAuthenticationSuccessHandler(SensuClient sensuClient) {
        this.sensuClient = sensuClient;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response, Authentication authentication)
            throws ServletException, IOException {


        sensuClient.sendEvent("fasit.rest.logins", Collections.emptyMap(), ImmutableMap.of("loginsuccess", 1));
        response.setStatus(HttpServletResponse.SC_OK);
        
        PrintWriter writer = response.getWriter();
        writer.write(User.getCurrentUser().getDisplayName() + " is logged in");

       writer.flush();
    }

}
