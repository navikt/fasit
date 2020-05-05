package no.nav.aura.integration;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.client.utils.URIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BastaRestClient {

    private static final Logger log = LoggerFactory.getLogger(BastaRestClient.class);

    private RestEasyClient restClient;
    private URI bastaUrl;
    private URI bastaOracleUrl;

    public BastaRestClient(String bastaAPI, String bastaOracleAPI, String username, String password) {
        log.info("Using basta from {}  with user {}", bastaAPI, username);
        this.restClient = new RestEasyClient(username, password);
        this.bastaUrl = UriBuilder.fromUri(bastaAPI).build();
        this.bastaOracleUrl = UriBuilder.fromUri(bastaOracleAPI).build();
    }

    public URI stop(String... hostnames) {
        URI url = UriBuilder.fromUri(bastaUrl).path("/api/orders/vm/stop").build();

        String bastaOrderId = postToBasta(url, hostnames);
        log.info("Created stopjob in basta with id {}", bastaOrderId);
        return getBastaOrderUrl(bastaOrderId);
    }

    private String postToBasta(URI url, Object payload) {
        Response response = restClient.createRequest(url).request().post(Entity.entity(payload, MediaType.APPLICATION_JSON));
        restClient.checkResponseAndThrowExeption(response, url);
        @SuppressWarnings("unchecked")
        Map<String, String> order = response.readEntity(Map.class);
        response.close();
        String bastaOrderId = String.valueOf(order.get("orderId"));
        return bastaOrderId;
    }

    public URI getBastaOrderUrl(String orderId) {
        return URIUtils.resolve(bastaUrl, "/#/order_details/" + orderId);
    }

    public URI getBastaNodeUrl(String hostname) {
        return URIUtils.resolve(bastaUrl, "/#/order_list?hostname=" + hostname);
    }

    public URI decommision(String... hostnames) {
        URI url = UriBuilder.fromUri(bastaUrl).path("/api/orders/vm/remove").build();
        String bastaOrderId = postToBasta(url, hostnames);
        log.info("Created decommisionjob in basta with id {}", bastaOrderId);
        return getBastaOrderUrl(bastaOrderId);
    }

    public URI getBastaUrl() {
        return bastaUrl;
    }

    public Response deleteDatabase(long fasitId) {
        URI url = UriBuilder.fromUri(bastaOracleUrl).path("/" + fasitId).build();
        try {
            Response response = restClient.createRequest(url).request().delete();
            log.info("Sent database delete request to Basta for database with Fasit ID {}. Got response {}", fasitId, response.getStatus());
            response.close();
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Response stopDatabase(long fasitId) {
        URI url = UriBuilder.fromUri(bastaOracleUrl).path("/" + fasitId + "/stop").build();
        try {
            Response response = restClient.createRequest(url).request().put(Entity.entity("", MediaType.TEXT_PLAIN));
            response.close();
            // final ClientResponse response = request.put();
            log.info("Sent database stop request to Basta for database with Fasit ID {}. Got response {}", fasitId, response.getStatus());
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean stopCredential(String environmentClass, String zone, String fasitAlias) {
        URI url = UriBuilder.fromUri(bastaUrl).path("/api/orders/serviceuser/stop").build();
        Map<String, String> params = createCredentialParams(environmentClass, zone, fasitAlias);
        try {
            postToBasta(url, params);
        } catch (WebApplicationException e) {
          log.info("unable to stop credential {}", e.getMessage());
            return false;
        }
        return true;
    }

    public boolean deleteCredential(String environmentClass, String zone, String fasitAlias) {
        URI url = UriBuilder.fromUri(bastaUrl).path("/api/orders/serviceuser/remove").build();
        Map<String, String> params = createCredentialParams(environmentClass, zone, fasitAlias);
        try {
            postToBasta(url, params);
        } catch (WebApplicationException e) {
          log.info("unable to delete credential {}", e.getMessage());
            return false;
        }
        return true;
    }

    private Map<String, String> createCredentialParams(String environmentClass, String zone, String fasitAlias) {
        Map<String, String> params = new HashMap<>();
        params.put("fasitAlias", fasitAlias);
        params.put("zone", zone);
        params.put("environmentClass", environmentClass);
        return params;
    }
}
