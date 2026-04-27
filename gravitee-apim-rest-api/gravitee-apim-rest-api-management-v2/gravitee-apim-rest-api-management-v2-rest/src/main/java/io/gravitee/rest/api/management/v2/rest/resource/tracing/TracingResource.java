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
package io.gravitee.rest.api.management.v2.rest.resource.tracing;

import io.gravitee.apim.core.tracing.model.Trace;
import io.gravitee.apim.core.tracing.model.TraceSearchCriteria;
import io.gravitee.apim.core.tracing.model.TracingGraph;
import io.gravitee.apim.core.tracing.use_case.GetTraceUseCase;
import io.gravitee.apim.core.tracing.use_case.GetTracingGraphUseCase;
import io.gravitee.apim.core.tracing.use_case.SearchTracesUseCase;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * POC tracing resource — exposes Tempo-backed tracing data under {@code /environments/{envId}/tracing}. Returns domain records as
 * JSON directly; a DTO/OpenAPI layer can be introduced later.
 */
@Produces(MediaType.APPLICATION_JSON)
public class TracingResource extends AbstractResource {

    @Inject
    SearchTracesUseCase searchTracesUseCase;

    @Inject
    GetTraceUseCase getTraceUseCase;

    @Inject
    GetTracingGraphUseCase getTracingGraphUseCase;

    @GET
    @Path("/traces")
    public List<Trace> listTraces(
        @QueryParam("tags") String tags,
        @QueryParam("limit") @DefaultValue("20") int limit,
        @QueryParam("start") Long start,
        @QueryParam("end") Long end
    ) {
        var input = new SearchTracesUseCase.Input(getAuditInfo(), new TraceSearchCriteria(tags, limit, start, end));
        return searchTracesUseCase.execute(input).traces();
    }

    @GET
    @Path("/traces/{traceId}")
    public Trace getTrace(@PathParam("traceId") String traceId) {
        return getTraceUseCase.execute(new GetTraceUseCase.Input(getAuditInfo(), traceId)).trace();
    }

    @GET
    @Path("/traces/{traceId}/graph")
    public TracingGraph getTracingGraph(@PathParam("traceId") String traceId) {
        return getTracingGraphUseCase.execute(new GetTracingGraphUseCase.Input(getAuditInfo(), traceId)).graph();
    }
}
