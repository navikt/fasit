package no.nav.aura.fasit.rest.helpers;

import java.net.URI;
import java.util.ArrayList;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.springframework.data.domain.Page;

public abstract class PagingBuilder {

    public static ResponseBuilder pagingResponseBuilder(Page<?> page, URI request) {
        UriBuilder requestUriBuilder = UriBuilder.fromUri(request).replaceQueryParam("page", "{page}");

        int currentPage = page.getNumber();
        ArrayList<Link> links = new ArrayList<>();
        if (currentPage + 1 < page.getTotalPages()) {
            links.add(Link.fromUri(requestUriBuilder.build(currentPage + 1)).rel("next").build());
        }
        links.add(Link.fromUri(requestUriBuilder.build(page.getTotalPages() - 1)).rel("last").build());
        return Response.ok().links(links.toArray(new Link[]{})).header("total_count", page.getTotalElements());
    }


}
