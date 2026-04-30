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
package io.gravitee.gamma.module.apim.rest.resource.tracing;

import io.gravitee.gamma.module.apim.core.tracing.model.TracingGraph;
import io.gravitee.gamma.module.apim.core.tracing.use_case.GetTraceUseCase;
import io.gravitee.gamma.module.apim.core.tracing.use_case.GetTracingGraphUseCase;
import io.gravitee.gamma.module.apim.core.tracing.use_case.SearchTracesUseCase;
import io.gravitee.node.api.opentelemetry.query.model.Trace;
import io.gravitee.node.api.opentelemetry.query.model.TraceSearchCriteria;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracing resource — exposes Tempo-backed tracing data under {@code /tracing}. Returns domain records as JSON directly; a DTO/OpenAPI
 * layer can be introduced later.
 */
@Produces(MediaType.APPLICATION_JSON)
public class TracingResource {

    @Inject
    private SearchTracesUseCase searchTracesUseCase;

    @Inject
    private GetTraceUseCase getTraceUseCase;

    @Inject
    private GetTracingGraphUseCase getTracingGraphUseCase;

    @GET
    @Path("/traces")
    public List<Trace> listTraces(
        @QueryParam("tags") String tags,
        @QueryParam("limit") @DefaultValue("20") int limit,
        @QueryParam("start") Long start,
        @QueryParam("end") Long end
    ) {
        var criteria = new TraceSearchCriteria(parseTags(tags), limit, toInstant(start), toInstant(end));
        return searchTracesUseCase.execute(new SearchTracesUseCase.Input(criteria)).traces();
    }

    @GET
    @Path("/traces/{traceId}")
    public Trace getTrace(@PathParam("traceId") String traceId) {
        return getTraceUseCase.execute(new GetTraceUseCase.Input(traceId)).trace();
    }

    @GET
    @Path("/traces/{traceId}/graph")
    public TracingGraph getTracingGraph(@PathParam("traceId") String traceId) {
        return getTracingGraphUseCase.execute(new GetTracingGraphUseCase.Input(traceId)).graph();
    }

    private static Map<String, String> parseTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String pair : raw.split("\\s+")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && eq < pair.length() - 1) {
                result.put(pair.substring(0, eq), pair.substring(eq + 1));
            }
        }
        return result;
    }

    private static Instant toInstant(Long epochSeconds) {
        return epochSeconds == null ? null : Instant.ofEpochSecond(epochSeconds);
    }
}
