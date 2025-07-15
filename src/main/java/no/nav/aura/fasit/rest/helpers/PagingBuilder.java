package no.nav.aura.fasit.rest.helpers;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public abstract class PagingBuilder {
    public static ResponseEntity.BodyBuilder pagingResponseBuilder(Page<?> page, URI request) {
        int currentPage = page.getNumber();
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("total_count", String.valueOf(page.getTotalElements()));
        
        // Add Link header similar to JAX-RS Links
        StringBuilder linkHeader = new StringBuilder();
        
        if (currentPage + 1 < page.getTotalPages()) {
            URI nextUri = ServletUriComponentsBuilder.fromUri(request)
                .replaceQueryParam("page", currentPage + 1)
                .build()
                .toUri();
            linkHeader.append("<").append(nextUri).append(">; rel=\"next\"");
        }
        
        if (page.getTotalPages() > 0) {
            if (linkHeader.length() > 0) {
                linkHeader.append(", ");
            }
            URI lastUri = ServletUriComponentsBuilder.fromUri(request)
                .replaceQueryParam("page", page.getTotalPages() - 1)
                .build()
                .toUri();
            linkHeader.append("<").append(lastUri).append(">; rel=\"last\"");
        }
        
        if (linkHeader.length() > 0) {
            headers.set("Link", linkHeader.toString());
        }
        return ResponseEntity.ok().headers(headers);
    }


}
