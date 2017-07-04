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
package io.gravitee.management.services.healthcheck.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.management.services.healthcheck.Probe;
import io.gravitee.management.services.healthcheck.Result;
import io.gravitee.management.services.healthcheck.vertx.VertxCompletableFuture;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HealthcheckHandler implements Handler<RoutingContext> {

    private final Logger LOGGER = LoggerFactory.getLogger(HealthcheckHandler.class);

    private List<Probe> probes;

    @Override
    public void handle(RoutingContext ctx) {
        Map<String, CompletableFuture<Result>> probeResults = this.probes.stream().collect(
                Collectors.toMap(Probe::id, Probe::check));

        VertxCompletableFuture.allOf(ctx.vertx(), probeResults.values().toArray(new CompletableFuture[]{})).thenAccept(aVoid -> {
            boolean unhealthyProbe = probeResults.values().stream().anyMatch(completableFuture -> {
                try {
                    return !completableFuture.get().isHealthy();
                } catch (Exception ex) {
                    LOGGER.error("Unable to check probe health", ex);
                    return false;
                }
            });

            HttpServerResponse response = ctx.response();
            response.setStatusCode((unhealthyProbe) ? HttpStatusCode.INTERNAL_SERVER_ERROR_500 : HttpStatusCode.OK_200);
            response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            response.setChunked(true);

            Map<String, Result> results = new HashMap<>();

            probeResults.forEach((probe, resultCompletableFuture) -> {
                try {
                    results.put(probe, resultCompletableFuture.get());
                } catch (Exception ex) {
                    LOGGER.error("Unable to check probe health", ex);
                }
            });

            try {
                Json.prettyMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                response.write(Json.prettyMapper.writeValueAsString(results));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            // End the response
            response.end();
        });
    }

    public void setProbes(List<Probe> probes) {
        this.probes = probes;
    }
}
