/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.management.rest.provider;

import io.gravitee.common.http.HttpHeaders;

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
        List<String> schemes = ctx.getHeaders().get(HttpHeaders.X_FORWARDED_PROTO);
        if (schemes != null && !schemes.isEmpty()) {
            String scheme = schemes.get(0);
            UriBuilder builder = ctx.getUriInfo().getRequestUriBuilder();
            ctx.setRequestUri(builder.scheme(scheme).build());
        }

        List<String> hosts = ctx.getHeaders().get(HttpHeaders.X_FORWARDED_HOST);
        if (hosts != null && !hosts.isEmpty()) {
            String host = hosts.get(0);
            UriBuilder builder = ctx.getUriInfo().getRequestUriBuilder();

            if (host.contains(":")) {
                // Forwarded host contains both host and port
                String [] parts = host.split(":");
                builder.host(parts[0]).port(Integer.parseInt(parts[1]));
            } else {
                builder.host(host);
            }

            ctx.setRequestUri(builder.build());
        }
    }
}
