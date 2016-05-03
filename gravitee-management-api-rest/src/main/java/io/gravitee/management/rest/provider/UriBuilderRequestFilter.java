package io.gravitee.management.rest.provider;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.List;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@Provider
@PreMatching
public class UriBuilderRequestFilter implements ContainerRequestFilter {

    @Override
    public void filter( ContainerRequestContext ctx ) throws IOException {
        List<String> schemes = ctx.getHeaders().get("x-forwarded-proto");
        if (schemes != null && !schemes.isEmpty()) {
            String scheme = schemes.get(0);
            UriBuilder builder = ctx.getUriInfo().getRequestUriBuilder();
            ctx.setRequestUri(builder.scheme(scheme).build());
        }

        List<String> hosts = ctx.getHeaders().get("x-forwarded-for");
        if (hosts != null && !hosts.isEmpty()) {
            String host = hosts.get(0);
            UriBuilder builder = ctx.getUriInfo().getRequestUriBuilder();
            ctx.setRequestUri(builder.host(host).build());
        }
    }
}
