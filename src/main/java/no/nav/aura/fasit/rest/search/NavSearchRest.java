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
 * Api for å typeahead search i fasit. Git nok info til å populere dropdown i søkeboks
 */
@Component
@Path("api/v1/navsearch")
public class NavSearchRest {

    @Inject
    private SearchRepository searchRepository;

    @Context
    private UriInfo uriInfo;

    /**
     * Søker etter henholdsvis miljø, appinstans (på formatet miljønanvn appnanv), applikasjon, node, ressurs, miljø
     *  @param query    søkestreng. To ord separarert med mellomrom søkker etter appinstans i miljø. Miljø må være første ord
     * @param maxCount Antall søkeresultet som skal returneres
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<SearchResultPayload> navigationalSearch(
            @QueryParam("q") String query,
            @QueryParam("maxcount") @DefaultValue("20") Integer maxCount) {
        return searchRepository.navigationSearch(query, maxCount, uriInfo.getBaseUri());
    }
}
