package no.nav.aura.envconfig.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CorsFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // to allow browser to auto-include cookie, specific origin needs to be set dynamically
        String origin = httpRequest.getHeader("Origin");

        httpResponse.addHeader("Access-Control-Allow-Origin", origin);
        httpResponse.addHeader("Access-Control-Allow-Credentials", "true");
        httpResponse.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
        httpResponse.addHeader("Access-Control-Allow-Headers", "Content-Type, X-Comment");
        httpResponse.addHeader("Access-Control-Expose-Headers", "total_count, Link, Location");
        chain.doFilter(request, response);
    }

}
