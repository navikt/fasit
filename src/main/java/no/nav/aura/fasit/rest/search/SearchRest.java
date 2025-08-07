package no.nav.aura.fasit.rest.search;

import no.nav.aura.fasit.rest.model.SearchResultPayload;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.Set;

/**
 * Api søk i fasit.
 */
@Component
@Path("/api/v1/search")
public class SearchRest {

    @Inject
    private SearchRepository searchRepository;

    @Context
    private UriInfo uriInfo;

    /**
     * Søker i henholdsvis miljø, applikasjonsinstans, node, ressurs, cluster, innhold i ressurser, innhold i appconfig
     *
     * @param query    søkeord. Hvis man angir to ord adskilt med space, søkes det etter appinstans i miljø. Miljønavn må være første ord
     * @param maxCount Antall søkeresultet som skal returneres
     * @param type Filtrerer søkeresultatet på angitt type.
     *
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<SearchResultPayload> search(
            @QueryParam("q") String query,
            @QueryParam("maxcount") @DefaultValue("100") Integer maxCount,
            @QueryParam("type") @DefaultValue("ALL") SearchResultType type) {
        return searchRepository.search(query, maxCount, type, uriInfo.getBaseUri());
    }
}
