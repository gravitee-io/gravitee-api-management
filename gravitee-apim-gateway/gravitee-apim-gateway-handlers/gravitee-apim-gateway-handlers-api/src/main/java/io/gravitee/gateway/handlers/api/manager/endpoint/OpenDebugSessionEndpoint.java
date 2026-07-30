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
package io.gravitee.gateway.handlers.api.manager.endpoint;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.reactive.core.v4.analytics.DebugSessionRegistry;
import io.gravitee.node.management.http.endpoint.ManagementEndpoint;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Opens a debug session on a deployed API, raising its analytics detail without
 * redeploying it.
 *
 * A session is deliberately short-lived: it makes the gateway buffer payloads it
 * would otherwise stream, so it expires on its own and is capped here rather than
 * trusting the caller to close it.
 */
@CustomLog
public class OpenDebugSessionEndpoint implements Handler<RoutingContext>, ManagementEndpoint {

    /** Longest session the gateway will hold, whatever the caller asks for. */
    static final int MAX_TTL_SECONDS = 3_600;
    static final int DEFAULT_TTL_SECONDS = 600;
    static final int DEFAULT_SAMPLING_PERCENT = 100;

    @Autowired
    private ApiManager apiManager;

    @Autowired
    private DebugSessionRegistry debugSessionRegistry;

    @Override
    public HttpMethod method() {
        return HttpMethod.POST;
    }

    @Override
    public String path() {
        return "/debug-sessions/:apiId";
    }

    @Override
    public void handle(final RoutingContext ctx) {
        final HttpServerResponse response = ctx.response();
        final String apiId = ctx.request().getParam("apiId");

        if (apiManager.get(apiId) == null) {
            response.setStatusCode(HttpStatusCode.NOT_FOUND_404).end();
            return;
        }

        final int ttlSeconds = intParam(ctx, "ttlSeconds", DEFAULT_TTL_SECONDS, 1, MAX_TTL_SECONDS);
        final int samplingPercent = intParam(ctx, "sampling", DEFAULT_SAMPLING_PERCENT, 1, 100);
        final long expiresAt = System.currentTimeMillis() + ttlSeconds * 1_000L;

        debugSessionRegistry.open(apiId, expiresAt, samplingPercent);
        log.info("Debug session opened on API {} for {}s at {}% sampling", apiId, ttlSeconds, samplingPercent);

        response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        response.setStatusCode(HttpStatusCode.OK_200);
        response.end(new JsonObject().put("apiId", apiId).put("expiresAt", expiresAt).put("samplingPercent", samplingPercent).encode());
    }

    /** Reads a bounded integer query parameter, falling back on anything unusable. */
    private static int intParam(final RoutingContext ctx, final String name, final int defaultValue, final int min, final int max) {
        final String raw = ctx.request().getParam(name);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Math.clamp(Integer.parseInt(raw), min, max);
        } catch (NumberFormatException e) {
            log.debug("Ignoring unparsable '{}' parameter on debug session request: {}", name, raw, e);
            return defaultValue;
        }
    }
}
