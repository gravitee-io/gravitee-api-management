/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.rest.filter;

import io.gravitee.common.http.HttpHeaders;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.List;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@Provider
@PreMatching
public class UriBuilderRequestFilter implements ContainerRequestFilter {

    private static final int NO_EXPLICIT_PORT = -1; // this resets explicit port in UriBuilder

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        UriBuilder baseBuilder = ctx.getUriInfo().getBaseUriBuilder();
        UriBuilder requestBuilder = ctx.getUriInfo().getRequestUriBuilder();

        // order matters as each process method may override values set by previous one(s)
        processProtocolHeader(ctx, baseBuilder, requestBuilder);
        processHostHeader(ctx, baseBuilder, requestBuilder);
        processPortHeader(ctx, baseBuilder, requestBuilder);
    }

    private void processProtocolHeader(ContainerRequestContext ctx, UriBuilder baseBuilder, UriBuilder requestBuilder) {
        String protoHeaderValue = getFirstHeaderValueOrNull(ctx, HttpHeaders.X_FORWARDED_PROTO);
        if (protoHeaderValue != null) {
            baseBuilder.scheme(protoHeaderValue);
            requestBuilder.scheme(protoHeaderValue);
            ctx.setRequestUri(baseBuilder.build(), requestBuilder.build());
        }
    }

    private void processHostHeader(ContainerRequestContext ctx, UriBuilder baseBuilder, UriBuilder requestBuilder) {
        String hostHeaderValue = getFirstHeaderValueOrNull(ctx, HttpHeaders.X_FORWARDED_HOST);
        if (hostHeaderValue != null) {
            if (hostHeaderValue.contains(":")) {
                int lastColonIdx = hostHeaderValue.lastIndexOf(':');
                String host = hostHeaderValue.substring(0, lastColonIdx);
                int port = Integer.parseInt(hostHeaderValue.substring(lastColonIdx + 1));
                baseBuilder.host(host).port(port);
                requestBuilder.host(host).port(port);
            } else {
                baseBuilder.host(hostHeaderValue).port(NO_EXPLICIT_PORT);
                requestBuilder.host(hostHeaderValue).port(NO_EXPLICIT_PORT);
            }

            ctx.setRequestUri(baseBuilder.build(), requestBuilder.build());
        }
    }

    private void processPortHeader(ContainerRequestContext ctx, UriBuilder baseBuilder, UriBuilder requestBuilder) {
        String portHeaderValue = getFirstHeaderValueOrNull(ctx, HttpHeaders.X_FORWARDED_PORT);
        if (portHeaderValue != null) {
            int port = Integer.parseInt(portHeaderValue);
            baseBuilder.port(port);
            requestBuilder.port(port);
            ctx.setRequestUri(baseBuilder.build(), requestBuilder.build());
        }
    }

    private String getFirstHeaderValueOrNull(ContainerRequestContext ctx, String headerName) {
        List<String> headerValues = ctx.getHeaders().get(headerName);
        if (headerValues != null && !headerValues.isEmpty()) {
            return headerValues.get(0);
        } else {
            return null;
        }
    }
}
