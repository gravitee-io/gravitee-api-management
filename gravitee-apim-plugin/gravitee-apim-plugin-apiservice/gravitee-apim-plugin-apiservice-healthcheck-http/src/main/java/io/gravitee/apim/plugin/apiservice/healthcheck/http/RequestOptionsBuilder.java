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
package io.gravitee.apim.plugin.apiservice.healthcheck.http;

import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.node.vertx.client.http.VertxHttpClientFactory;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import java.net.URL;

class RequestOptionsBuilder {

    private RequestOptionsBuilder() {}

    static RequestOptions build(ExecutionContext ctx, HttpHealthCheckServiceConfiguration hcConfiguration) {
        final RequestOptions options = new RequestOptions();

        final HttpHeaders configHeaders = ctx.request().headers();
        if (configHeaders != null && !configHeaders.isEmpty()) {
            final MultiMap headers = new HeadersMultiMap();
            configHeaders.forEach(header -> headers.add(header.getKey(), header.getValue()));
            options.setHeaders(headers);
        }

        final URL target = VertxHttpClientFactory.buildUrl(hcConfiguration.getTarget());
        final boolean isSsl = VertxHttpClientFactory.isSecureProtocol(target.getProtocol());
        final int targetPort = VertxHttpClientFactory.getPort(target, isSsl);

        options
            .setMethod(HttpMethod.valueOf(ctx.request().method().name()))
            .setURI(target.getQuery() == null ? target.getPath() : target.getPath() + "?" + target.getQuery())
            .setPort(targetPort)
            .setSsl(isSsl)
            .setHost(target.getHost());

        applyCustomHostIfPresent(options, targetPort, target);

        return options;
    }

    private static void applyCustomHostIfPresent(RequestOptions options, int port, URL target) {
        String customHost = options.getHeaders() != null ? options.getHeaders().get(io.vertx.core.http.HttpHeaders.HOST) : null;
        if (customHost == null || customHost.isBlank()) {
            return;
        }
        // Pin the TCP connection to the actual backend so that the custom Host value
        // only affects the HTTP Host header and not the connection address.
        options.setServer(io.vertx.core.net.SocketAddress.inetSocketAddress(port, target.getHost()));
        options.setHost(customHost);
    }
}
