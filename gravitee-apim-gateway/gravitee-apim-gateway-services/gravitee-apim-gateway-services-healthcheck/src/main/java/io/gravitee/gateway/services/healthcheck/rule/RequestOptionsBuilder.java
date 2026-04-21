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
package io.gravitee.gateway.services.healthcheck.rule;

import io.gravitee.common.http.HttpHeader;
import io.gravitee.common.utils.UUID;
import io.gravitee.definition.model.services.healthcheck.HealthCheckStep;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.exceptions.ExpressionEvaluationException;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.utils.NodeUtils;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import java.net.URL;
import lombok.CustomLog;

@CustomLog
class RequestOptionsBuilder {

    private static final String HTTPS_SCHEME = "https";

    private final Node node;
    private final TemplateEngine templateEngine;
    private final String apiId;

    RequestOptionsBuilder(Node node, TemplateEngine templateEngine, String apiId) {
        this.node = node;
        this.templateEngine = templateEngine;
        this.apiId = apiId;
    }

    RequestOptions build(URL request, HealthCheckStep step) {
        final int port = resolvePort(request);

        RequestOptions options = new RequestOptions()
            .setURI(relativeUrl(request))
            .setPort(port)
            .setHost(request.getHost())
            .setMethod(HttpMethod.valueOf(step.getRequest().getMethod().name().toUpperCase()))
            .putHeader(io.vertx.rxjava3.core.http.HttpHeaders.USER_AGENT, NodeUtils.userAgent(node))
            .putHeader("X-Gravitee-Request-Id", UUID.toString(UUID.random()));

        if (step.getRequest().getHeaders() != null) {
            step
                .getRequest()
                .getHeaders()
                .forEach(h -> options.putHeader(h.getName(), resolveHeaderValue(h)));
            applyCustomHostIfPresent(options, port, request);
        }

        return options;
    }

    private static int resolvePort(URL url) {
        if (url.getPort() != -1) {
            return url.getPort();
        }
        return HTTPS_SCHEME.equals(url.getProtocol()) ? 443 : 80;
    }

    private static String relativeUrl(URL url) {
        return url.getQuery() == null ? url.getPath() : url.getPath() + '?' + url.getQuery();
    }

    private String resolveHeaderValue(HttpHeader httpHeader) {
        try {
            String value = templateEngine.getValue(httpHeader.getValue(), String.class);
            return value != null ? value : "";
        } catch (ExpressionEvaluationException e) {
            log.warn("Expression {} cannot be evaluated for healthcheck of API {}", httpHeader.getValue(), apiId);
            return "";
        }
    }

    private static void applyCustomHostIfPresent(RequestOptions options, int port, URL request) {
        String customHost = options.getHeaders() != null ? options.getHeaders().get(io.vertx.core.http.HttpHeaders.HOST) : null;
        if (customHost == null || customHost.isBlank()) {
            return;
        }
        // Pin the TCP connection to the actual backend so that the custom Host value
        // only affects the HTTP Host header and not the connection address.
        options.setServer(io.vertx.core.net.SocketAddress.inetSocketAddress(port, request.getHost()));
        options.setHost(customHost);
    }
}
