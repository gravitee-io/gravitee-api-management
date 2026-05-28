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
package io.gravitee.gamma.rest.resources.tracing;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.gamma.rest.core.tracing.exception.MissingTracingScopeException;
import io.gravitee.gamma.rest.core.tracing.exception.TraceNotFoundException;
import io.gravitee.gamma.rest.core.tracing.model.FilterCondition;
import io.gravitee.gamma.rest.core.tracing.use_case.GetTraceDetailUseCase;
import io.gravitee.gamma.rest.core.tracing.use_case.GetTraceFilterDefinitionsUseCase;
import io.gravitee.gamma.rest.core.tracing.use_case.GetTraceFilterValuesUseCase;
import io.gravitee.gamma.rest.core.tracing.use_case.SearchTracesUseCase;
import io.gravitee.gamma.rest.resources.tracing.dto.FilterConditionDto;
import io.gravitee.gamma.rest.resources.tracing.dto.PaginatedResponseDto;
import io.gravitee.gamma.rest.resources.tracing.dto.SearchTracesRequestDto;
import io.gravitee.gamma.rest.resources.tracing.dto.TraceDetailDto;
import io.gravitee.gamma.rest.resources.tracing.dto.TraceFilterSpecDto;
import io.gravitee.gamma.rest.resources.tracing.dto.TraceFilterSpecsResponseDto;
import io.gravitee.gamma.rest.resources.tracing.dto.TraceFilterValueDto;
import io.gravitee.gamma.rest.resources.tracing.dto.TraceSummaryDto;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * Per-API trace explorer endpoints, shared across every gamma module. Mounted under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/observability/traces} by
 * {@code GammaRootResource}.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code POST /search?page=&perPage=} — paginated trace listing. Body carries {@code apiId},
 *       a {@code timeRange} (ISO {@code from}/{@code to}), and an optional list of filter
 *       conditions. Pagination is 1-based, lives in the query string — same convention as the apim
 *       management v2 {@code /logs/search} endpoint. Response is the shared paginated envelope
 *       {@code { data, pagination: { totalCount, page, pageCount } }}.</li>
 *   <li>{@code GET /{traceId}?apiId=} — fetch one trace's full span tree. {@code apiId} is required
 *       as a security defense: a user with access to API_A can't fetch traces from API_B by knowing
 *       their id — the resource-attribute filter on {@code gravitee.api.id} rejects them, returning
 *       404 (collapsing "doesn't exist" and "wrong scope" so cross-API existence can't be probed).</li>
 *   <li>{@code GET /filters/definition?module=} — self-describing filter spec list. {@code module}
 *       is optional; supplied to pull in module-specific contributor filters alongside the
 *       cross-module ones.</li>
 *   <li>{@code GET /filters/{filterName}/values?module=&query=&page=&perPage=} — paginated values
 *       for one filter; same envelope as {@code /search}.</li>
 * </ul>
 *
 * <h2>Required {@code apiId} on search + detail (not module)</h2>
 * Per-API auth scope is enforced by the caller picking from APIs they can see (a global trace
 * explorer page presents a mandatory API picker before the search runs). Module isn't carried on the
 * wire for search + detail because every {@code apiId} belongs to exactly one module (an API's type
 * defines its module — LLM_PROXY → AIM, HTTP_PROXY → APIM, etc.), so api-id alone is sufficient to
 * scope. The filter-discovery endpoints still take {@code module} because their entire purpose is
 * "which filters does this module's contributor expose?".
 *
 * <h2>Tenancy</h2>
 * Both {@code organizationId} and {@code environmentId} come from {@link GraviteeContext} — the
 * URL's {@code envId} placeholder is consumed by the auth filter chain (which may receive an hrid
 * like {@code "default"} rather than the canonical id) and resolved to the canonical env id before
 * this resource runs. Filtering against the path-string directly would mis-match the
 * {@code gravitee.env.id} resource attribute the gateway emits, which is always the canonical id.
 *
 * @author GraviteeSource Team
 */
public class TracingResource {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PER_PAGE = 20;

    @Inject
    private SearchTracesUseCase searchTracesUseCase;

    @Inject
    private GetTraceDetailUseCase getTraceDetailUseCase;

    @Inject
    private GetTraceFilterDefinitionsUseCase getTraceFilterDefinitionsUseCase;

    @Inject
    private GetTraceFilterValuesUseCase getTraceFilterValuesUseCase;

    @POST
    @Path("/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponseDto<TraceSummaryDto> searchTraces(
        @QueryParam("page") @DefaultValue("1") int page,
        @QueryParam("perPage") @DefaultValue("20") int perPage,
        SearchTracesRequestDto request
    ) {
        if (request == null) {
            // Defensive: a totally empty body still requires the apiId scope param.
            throw new MissingTracingScopeException("apiId");
        }
        requireParam("apiId", request.apiId());

        List<FilterCondition> filters = request.filters() == null
            ? List.of()
            : request.filters().stream().map(FilterConditionDto::toCore).toList();

        var ctx = GraviteeContext.getExecutionContext();
        var output = searchTracesUseCase.execute(
            new SearchTracesUseCase.Input(
                ctx.getOrganizationId(),
                ctx.getEnvironmentId(),
                request.apiId(),
                filters,
                request.timeRange() != null ? request.timeRange().from() : null,
                request.timeRange() != null ? request.timeRange().to() : null,
                page,
                perPage
            )
        );
        Page<io.gravitee.gamma.rest.core.tracing.model.Trace> tracesPage = output.traces();
        List<TraceSummaryDto> data = tracesPage.getContent().stream().map(TraceSummaryDto::from).toList();
        return PaginatedResponseDto.of(data, tracesPage.getTotalElements(), page, perPage);
    }

    /**
     * Self-describing list of filter specs the UI uses to build its chip palette without hardcoded
     * knowledge of what the backend supports. {@code module} is optional — when omitted, only the
     * cross-module ({@code moduleId() == null}) contributions are returned, useful for an
     * "introspect every always-available filter" UI. When provided, the module-specific
     * contributors' filters are unioned in (last-wins by {@code name}).
     */
    @GET
    @Path("/filters/definition")
    @Produces(MediaType.APPLICATION_JSON)
    public TraceFilterSpecsResponseDto getTraceFilterDefinitions(@QueryParam("module") String module) {
        var output = getTraceFilterDefinitionsUseCase.execute(new GetTraceFilterDefinitionsUseCase.Input(module));
        return new TraceFilterSpecsResponseDto(output.filters().stream().map(TraceFilterSpecDto::from).toList());
    }

    /**
     * Returns a paginated list of allowed values for a given filter. Behaviour matches the apim
     * management v2 {@code GET /observability/filters/{name}/values}: ENUM filters return their
     * static value list (optionally substring-filtered by {@code query}); KEYWORD filters return
     * distinct values from the data store; NUMBER / STRING / BOOLEAN return 400 because they have
     * no enumerable value pool.
     *
     * <p>Pagination is 1-based; response uses the shared {@link PaginatedResponseDto} envelope.
     */
    @GET
    @Path("/filters/{filterName}/values")
    @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponseDto<TraceFilterValueDto> getTraceFilterValues(
        @PathParam("filterName") String filterName,
        @QueryParam("module") String module,
        @QueryParam("query") String query,
        @QueryParam("page") @DefaultValue("1") int page,
        @QueryParam("perPage") @DefaultValue("10") int perPage
    ) {
        var output = getTraceFilterValuesUseCase.execute(new GetTraceFilterValuesUseCase.Input(module, filterName, query, page, perPage));
        List<TraceFilterValueDto> data = output.page().data().stream().map(TraceFilterValueDto::from).toList();
        return PaginatedResponseDto.of(data, output.page().totalElements(), page, perPage);
    }

    @GET
    @Path("/{traceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTrace(@PathParam("traceId") String traceId, @QueryParam("apiId") String apiId) {
        requireParam("apiId", apiId);
        var ctx = GraviteeContext.getExecutionContext();
        var output = getTraceDetailUseCase.execute(
            new GetTraceDetailUseCase.Input(ctx.getOrganizationId(), ctx.getEnvironmentId(), apiId, traceId)
        );
        return output
            .trace()
            .map(detail -> Response.ok(TraceDetailDto.from(detail)).build())
            .orElseThrow(() -> new TraceNotFoundException(traceId, apiId));
    }

    /**
     * Centralised guard so the 400 message is identical across endpoints / parameters and the
     * validation lives next to the doc explaining why each param is required (see class Javadoc).
     * The {@link MissingTracingScopeException} is mapped to HTTP 400 by the apim
     * {@code ValidationDomainExceptionMapper} already registered in {@code GammaModuleApplication}.
     */
    private static void requireParam(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new MissingTracingScopeException(name);
        }
    }
}
