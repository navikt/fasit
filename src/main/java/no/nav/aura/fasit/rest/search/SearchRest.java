package no.nav.aura.fasit.rest.search;

import no.nav.aura.fasit.rest.model.SearchResultPayload;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.inject.Inject;

import java.net.URI;
import java.util.Set;

/**
 * Api søk i fasit.
 */
@RestController
@RequestMapping("/api/v1/search")
public class SearchRest {

    @Inject
    private SearchRepository searchRepository;

    /**
     * Søker i henholdsvis miljø, applikasjonsinstans, node, ressurs, cluster, innhold i ressurser, innhold i appconfig
     *
     * @param query    søkeord. Hvis man angir to ord adskilt med space, søkes det etter appinstans i miljø. Miljønavn må være første ord
     * @param maxCount Antall søkeresultet som skal returneres
     * @param type Filtrerer søkeresultatet på angitt type.
     *
     */
    @GetMapping(produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public Set<SearchResultPayload> search(
    		@RequestParam(name = "q") String query,
    		@RequestParam(name ="maxcount", defaultValue = "100") Integer maxCount,
    		@RequestParam(name = "type", defaultValue = "ALL") SearchResultType type) {
        URI baseUri = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();
        return searchRepository.search(query, maxCount, type, baseUri);
    }
}
