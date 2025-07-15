package no.nav.aura.fasit.rest.search;

import no.nav.aura.fasit.rest.model.SearchResultPayload;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.inject.Inject;

import java.net.URI;
import java.util.Set;

/**
 * Api for å typeahead search i fasit. Git nok info til å populere dropdown i søkeboks
 */
@RestController
@RequestMapping("/api/v1/navsearch")
public class NavSearchRest {

    @Inject
    private SearchRepository searchRepository;

    /**
     * Søker etter henholdsvis miljø, appinstans (på formatet miljønanvn appnanv), applikasjon, node, ressurs, miljø
     *  @param query    søkestreng. To ord separarert med mellomrom søkker etter appinstans i miljø. Miljø må være første ord
     * @param maxCount Antall søkeresultet som skal returneres
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Set<SearchResultPayload> navigationalSearch(
            @RequestParam(name = "q") String query,
            @RequestParam(name = "maxcount", defaultValue = "20") Integer maxCount) {
        URI baseUri = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();
        return searchRepository.navigationSearch(query, maxCount, baseUri);
    }
}
