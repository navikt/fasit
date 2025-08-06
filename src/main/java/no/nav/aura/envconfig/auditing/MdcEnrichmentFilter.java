package no.nav.aura.envconfig.auditing;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import no.nav.aura.envconfig.spring.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class MdcEnrichmentFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(MdcEnrichmentFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            try {
                MDC.put("user", User.getCurrentUser().getIdentity());
            } catch (Exception e) {
                logger.error("Error retrieving user", e);
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove("user");
        }
    }

    @Override
    public void destroy() {
    }

}
