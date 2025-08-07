package no.nav.aura.envconfig.filter;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import no.nav.aura.sensu.SensuClient;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static org.springframework.web.context.support.WebApplicationContextUtils.getRequiredWebApplicationContext;

public class HttpMetricFilter implements Filter {

    private static final String CONF_ENVIRONMENTS_API = "/conf/environments";
    private static final String CONF_APPLICATIONSINSTANCES_API = "/conf/v1/applicationinstances";
    private static final String CONF_APPLICATIONINSTANCES_API_2 = "/conf/v1/environments/";
    private static final String CONF_APPLICATIONS_API = "/conf/applications";
    private static final String CONF_NODES_API = "/conf/nodes";
    private static final String CONF_RESOURCES_API = "/conf/resources";

    private static final String REST_ENVIRONMENTS_API = "/api/v2/environments";
    private static final String REST_APPLICATIONSINSTANCES_API = "/api/v2/applicationinstances";
    private static final String REST_APPLICATIONS_API = "/api/v2/applications";
    private static final String REST_NODES_API = "/api/v2/nodes";
    private static final String REST_RESOURCES_API = "/api/v2/resources";
    private static final String REST_SEARCH_API = "/api/v1/search";
    private static final String REST_NAVSEARCH_API = "/api/v1/navsearch";

    private SensuClient sensuClient;

    @SuppressWarnings("unchecked")
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        sensuClient = getRequiredWebApplicationContext(filterConfig.getServletContext()).
                getBean(SensuClient.class);
    }

    @SuppressWarnings("serial")
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = mapToTimedService(httpRequest.getRequestURI());

        if (uri != null) {
            long start = System.currentTimeMillis();
            chain.doFilter(request, response);
            long end = System.currentTimeMillis();
            sensuClient.sendEvent("fasitrequests.all", createEventTags((HttpServletRequest) request, (HttpServletResponse)response, uri), ImmutableMap.of("responsetime", end - start));
        } else {
            chain.doFilter(request, response);
        }
    }


    protected static String mapToTimedService(String uri) {
        String[] splitUri = splitShift(uri);

        if (uri.startsWith(CONF_ENVIRONMENTS_API)) {
            if (splitUri.length == 2) { // /environments
                return CONF_ENVIRONMENTS_API;
            } else if (splitUri.length == 3) { // /environments/{name}
                return CONF_ENVIRONMENTS_API + "/{name}";
            } else if (splitUri.length == 4 && splitUri[3].equalsIgnoreCase("applications")) { // /environments/{name}/applications
                return CONF_ENVIRONMENTS_API + "/{name}/applications";
            } else if (splitUri.length == 5 && splitUri[3].equalsIgnoreCase("applications")) { // /environments/{name}/applications/{appnanme}
                return CONF_ENVIRONMENTS_API + "/{name}/applications/{appname}";
            } else if (splitUri.length == 6 && splitUri[5].equalsIgnoreCase("appconfig")) { // /environments/{name}/applications/{appname}/appconfig
                return CONF_ENVIRONMENTS_API + "/{name}/applications/{appname}/appconfig";
            } else if (splitUri.length == 6 && splitUri[5].equalsIgnoreCase("verify")) { // /environments/{name}/applications/{appname}/verify
                return CONF_ENVIRONMENTS_API + "/{name}/applications/{appname}/verify";
            }
        } else if (uri.startsWith(CONF_APPLICATIONSINSTANCES_API)) {
            if (splitUri.length == 3) { // /v1/applicationinstances
                return CONF_APPLICATIONSINSTANCES_API;
            }
        } else if (uri.startsWith(CONF_APPLICATIONS_API)) {
            if (splitUri.length == 2) { // /applications
                return CONF_APPLICATIONS_API;
            } else if (splitUri.length == 3) { // /applications/{name}
                return CONF_APPLICATIONS_API + "/{name}";
            }
        } else if (uri.startsWith(CONF_NODES_API)) {
            if (splitUri.length == 2) { // /nodes
                return CONF_NODES_API;
            } else if (splitUri.length == 3 ) { // /nodes/{name}
                return CONF_NODES_API + "/{name}";
            }
        } else if (uri.startsWith(CONF_RESOURCES_API)) {
            if (splitUri.length == 2) { // /resources
                return CONF_RESOURCES_API;
            } else if (splitUri.length == 3 && splitUri[2].equalsIgnoreCase("bestmatch")) { // /resources/bestmatch
                return CONF_RESOURCES_API + "/bestmatch";
            } else if (splitUri.length == 3 ) { // resources/{id}
                return CONF_RESOURCES_API + "/{id}";
            }
        } else if(uri.startsWith(CONF_APPLICATIONINSTANCES_API_2)) {
            if(splitUri.length  == 6 && splitUri[4].equalsIgnoreCase("applications")) { // /v1/environments/t1/applications/gosys
                return CONF_APPLICATIONINSTANCES_API_2 + "{envname}/applications/{appname}";
            } else if(splitUri.length == 7 && splitUri[6].equalsIgnoreCase("full")) { // /v1/environments/t1/applications/gosys/full
                return CONF_APPLICATIONINSTANCES_API_2 + "{envname}/applications/{appname}/full";
            }
        } else if (uri.startsWith(REST_ENVIRONMENTS_API)) {
            if (splitUri.length == 3) { // v2/environments
                return REST_ENVIRONMENTS_API;
            } else if (splitUri.length == 4) { // v2/environments/{name}
                return REST_ENVIRONMENTS_API + "/{name}";
            } else if (splitUri.length == 5 && splitUri[4].equalsIgnoreCase("revisions")) { // /v2/environments/{name}/revisions
                return REST_ENVIRONMENTS_API + "/{name}/revisions";
            } else if (splitUri.length == 6 && splitUri[4].equalsIgnoreCase("revisions")) { // /v2/environments/{name}/revisions/{revision}
                return REST_ENVIRONMENTS_API + "/{name}/revisions/{revision}";
            } else if (splitUri.length == 5 && splitUri[4].equalsIgnoreCase("clusters")) { // /v2/environments/{name}/clusters
                return REST_ENVIRONMENTS_API + "/{name}/clusters";
            } else if (splitUri.length == 6 && splitUri[4].equalsIgnoreCase("clusters")) { /// v2/environments/{name}/clusters/{clustername}
                return REST_ENVIRONMENTS_API + "/{name}/clusters/{clustername}";
            } else if (splitUri.length == 7 && splitUri[4].equalsIgnoreCase("clusters") && splitUri[6].equalsIgnoreCase("revisions")) { // /v2/environments/{name}/clusters/{clustername}/revisions
                return REST_ENVIRONMENTS_API + "/{name}/clusters/{clustername}/revisions";
            } else if (splitUri.length == 8 && splitUri[4].equalsIgnoreCase("clusters") && splitUri[6].equalsIgnoreCase("revisions")) { // /v2/environments/{name}/clusters/{clustername}/revisions/{revision}
                return REST_ENVIRONMENTS_API + "/{name}/clusters/{clustername}/revisions/{revision}";
            }
        } else if (uri.startsWith(REST_APPLICATIONSINSTANCES_API)) {
            if (splitUri.length == 3) { // /v2/applicationinstances
                return REST_APPLICATIONSINSTANCES_API;
            } else if (splitUri.length == 4) { // /v2/applicationinstances/{name}
                return REST_APPLICATIONSINSTANCES_API + "/{name}";
            } else if (splitUri.length == 5 && splitUri[4].equalsIgnoreCase("revisions")) { // /v2/applicationinstances/{name}/revisions
                return REST_APPLICATIONSINSTANCES_API + "/{name}/revisions";
            } else if (splitUri.length == 5 && splitUri[4].equalsIgnoreCase("appconfig")) { // /v2/applicationinstances/{name}/appconfig
                return REST_APPLICATIONSINSTANCES_API + "/{name}/appconfig";
            } else if (splitUri.length == 6 && splitUri[4].equalsIgnoreCase("revisions")) { // /v2/applicationinstances/{name}/revisions/{revision}
                return REST_APPLICATIONSINSTANCES_API + "/{name}/revisions/{revision}";
            } else if (splitUri.length == 7 && splitUri[4].equalsIgnoreCase("revisions")) { // /v2/applicationinstances/{name}/revisions/{revision}/appconfig
                return REST_APPLICATIONSINSTANCES_API + "/{name}/revisions/{revision}/appconfig";
            }
        } else if (uri.startsWith(REST_APPLICATIONS_API)) {
            if (splitUri.length == 3) { // /v2/applications
                return REST_APPLICATIONS_API;
            } else if (splitUri.length == 4) { // /v2/applications/{name}
                return REST_APPLICATIONS_API + "/{name}";
            } else if (splitUri.length == 5 && splitUri[4].equalsIgnoreCase("revisions")) { // v2/applications/{name}/revisions
                return REST_APPLICATIONS_API + "/{name}/revisions";
            } else if (splitUri.length == 6 && splitUri[4].equalsIgnoreCase("revisions")) { // v2/applications/{name}/revisions/{revision}
                return REST_APPLICATIONS_API + "/{name}/revisions/{revision}";
            }
        } else if (uri.startsWith(REST_NODES_API)) {
            if (splitUri.length == 3) { // /v2/nodes
                return REST_NODES_API;
            } else if (splitUri.length == 4 && splitUri[3].equalsIgnoreCase("types")) { // v2/nodes/types
                return REST_NODES_API + "/types";
            } else if (splitUri.length == 4) { // /v2/nodes/{name}
                return REST_NODES_API + "/{name}";
            } else if (splitUri.length == 5 && splitUri[4].equalsIgnoreCase("revisions")) { // v2/nodes/{name}/revisions
                return REST_NODES_API + "/{name}/revisions";
            } else if (splitUri.length == 6 && splitUri[4].equalsIgnoreCase("revisions")) { // v2/nodes/{name}/revisions/{revision}
                return REST_NODES_API + "/{name}/revisions/{revision}";
            }
        } else if (uri.startsWith(REST_RESOURCES_API)) {
            if (splitUri.length == 3) { // /v2/nodes
                return REST_RESOURCES_API;
            } else if (splitUri.length == 4 && splitUri[3].equalsIgnoreCase("types")) { // v2/resources/types
                return REST_RESOURCES_API + "/types";
            } else if (splitUri.length == 5 && splitUri[3].equalsIgnoreCase("types")) { // v2/resources/types/{type}
                return REST_RESOURCES_API + "/types/{type}";
            } else if (splitUri.length == 4) { // /v2/resources/{name}
                return REST_RESOURCES_API + "/{name}";
            } else if (splitUri.length == 5 && splitUri[4].equalsIgnoreCase("revisions")) { // v2/resources/{name}/revisions
                return REST_RESOURCES_API + "/{name}/revisions";
            } else if (splitUri.length == 6 && splitUri[4].equalsIgnoreCase("revisions")) { // v2/resources/{name}/revisions/{revision}
                return REST_RESOURCES_API + "/{name}/revisions/{revision}";
            } else if (splitUri.length == 6 && splitUri[4].equalsIgnoreCase("file")) { // v2/resources/{name}/file
                return REST_RESOURCES_API + "/{name}/file/{file}";
            }
        } else if(uri.startsWith(REST_SEARCH_API)) {
            return REST_SEARCH_API;
        } else if(uri.startsWith(REST_NAVSEARCH_API)) {
            return REST_NAVSEARCH_API;
        }

        return null;
    }

    private static String[] splitShift(String uri) {
        String[] split = uri.split("/");
        return Arrays.copyOfRange(split, 1, split.length);
    }

    private static Map<String, Object> createEventTags(HttpServletRequest request, HttpServletResponse response, String uri) {
        Map<String, Object> tags = Maps.newHashMap();
        tags.put("uri", uri);
        tags.put("method", request.getMethod());
        tags.put("statuscode", response.getStatus());
        return tags;
    }

    @Override
    public void destroy() {
    }
}
