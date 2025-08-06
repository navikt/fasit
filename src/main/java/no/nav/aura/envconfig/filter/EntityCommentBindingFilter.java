package no.nav.aura.envconfig.filter;

import no.nav.aura.envconfig.auditing.EntityCommenter;
import no.nav.aura.envconfig.auditing.NavUser;
import no.nav.aura.envconfig.util.Effect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class EntityCommentBindingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(EntityCommentBindingFilter.class);

    private UserLookup userLookup;

    @SuppressWarnings("unchecked")
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        userLookup = WebApplicationContextUtils.getRequiredWebApplicationContext(filterConfig.getServletContext()).getBean(UserLookup.class);
    }

    @SuppressWarnings("serial")
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if ("GET".equals(httpRequest.getMethod())) {
            chain.doFilter(request, response);
        } else {
            String commentFromHeader = httpRequest.getHeader("x-comment");
            if (commentFromHeader == null) {
                commentFromHeader = "";
            }
            String commentFromQuery = mkString("\n", request.getParameterValues("entityStoreComment"));
            if (commentFromQuery == null) {
                commentFromQuery = "";
            }
            String onBehalfOfHeader = httpRequest.getHeader("x-onbehalfof");
            NavUser onBehalfOf = null;
            if (onBehalfOfHeader != null) {
                onBehalfOf = userLookup.lookup(onBehalfOfHeader);
                if (!onBehalfOf.isServiceUser() && !onBehalfOf.exists()) {
                    HttpServletResponse httpResponse = (HttpServletResponse) response;
                    log.warn("Onbehalfof user {} not found in ldap", onBehalfOfHeader);
                    httpResponse.setStatus(400);
                    httpResponse.setContentType("text/plain");
                    httpResponse.getWriter().write("User from requestheader 'x-onbehalfof' " + onBehalfOf.getId() + " is not found in ldap. Correct the data input");
                    return;
                }
                log.debug("Setting onbehalfof user to {}", onBehalfOf.getDisplayName());
            }
            String comment = commentFromHeader + commentFromQuery;
            EntityCommenter.applyComment(comment, onBehalfOf, new Effect() {

                @Override
                public void perform() {
                    try {
                        chain.doFilter(request, response);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    private String mkString(String delimiter, String... parameterValues) {
        String string = "";
        if (parameterValues != null) {
            for (String value : parameterValues) {
                if (!string.isEmpty()) {
                    string += delimiter;
                }
                string += value;
            }
        }
        return string;
    }

    @Override
    public void destroy() {

    }
}
