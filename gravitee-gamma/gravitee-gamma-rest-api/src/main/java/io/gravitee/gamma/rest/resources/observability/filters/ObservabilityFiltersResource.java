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
package io.gravitee.gamma.rest.resources.observability.filters;

import io.gravitee.common.http.MediaType;
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.use_case.GetObservabilityFilterDefinitionsUseCase;
import io.gravitee.gamma.rest.core.observability.filter.use_case.GetObservabilityFilterValuesUseCase;
import io.gravitee.gamma.rest.core.observability.filter.use_case.ResolveObservabilityFilterLabelsUseCase;
import io.gravitee.gamma.rest.resources.observability.EnumQueryParam;
import io.gravitee.gamma.rest.resources.observability.filters.dto.FilterSpecDto;
import io.gravitee.gamma.rest.resources.observability.filters.dto.FilterSpecsResponseDto;
import io.gravitee.gamma.rest.resources.observability.filters.dto.FilterValueDto;
import io.gravitee.gamma.rest.resources.observability.filters.dto.ResolveFilterLabelsRequestDto;
import io.gravitee.gamma.rest.resources.observability.filters.dto.ResolveFilterLabelsResponseDto;
import io.gravitee.gamma.rest.resources.tracing.dto.PaginatedResponseDto;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import java.util.List;

/**
 * Unified observability filter discovery surface, shared across every gamma module's logs and
 * analytics UI. Mounted under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/observability/filters} by
 * {@code GammaRootResource}.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code GET /definition?signal=&apiType=} — self-describing filter catalog: the built-in filters,
 *       then the exposed metric definitions of the environment as {@code DECLARED} entries. Both axes are
 *       optional and multi-valued: an absent axis is unconstrained, both absent yields the full
 *       catalog; a present axis narrows by set intersection. Each returned spec carries both axes so
 *       the consumer can route a filter to the right panel (logs table vs analytics histogram).</li>
 *   <li>{@code GET /{filterName}/values?query=&page=&perPage=&apiProduct=} — paginated selectable values for one
 *       filter. ENUM filters return their labelled static values (optionally substring-filtered by
 *       {@code query}); other types are handled in the use case.</li>
 *   <li>{@code GET /values?name=…} — the same, the filter named by a query parameter: an additional metric field
 *       name may hold a {@code /}, which cannot travel in a path segment.</li>
 *   <li>{@code POST /resolve?apiProduct=} — labels of stored filter ids, grouped per filter.</li>
 * </ul>
 *
 * <h2>Authorization</h2>
 * No permission guard: the catalog serves its traffic data ({@code DISCOVERED} entries, {@code lastSeen}) to a
 * caller who reads the environment's traffic only, and values and labels come from the rows the caller reads, so a
 * caller who reads no API gets empty answers. A request scoped to one AI workspace
 * ({@code apiProduct}) follows the analytics workspace rule: its reader also gets what the workspace's
 * aggregates show them.
 *
 * @author GraviteeSource Team
 */
public class ObservabilityFiltersResource {

    @Inject
    private GetObservabilityFilterDefinitionsUseCase getFilterDefinitionsUseCase;

    @Inject
    private GetObservabilityFilterValuesUseCase getFilterValuesUseCase;

    @Inject
    private ResolveObservabilityFilterLabelsUseCase resolveFilterLabelsUseCase;

    @GET
    @Path("/definition")
    @Produces(MediaType.APPLICATION_JSON)
    public FilterSpecsResponseDto getFilterDefinitions(
        @QueryParam("signal") List<String> signals,
        @QueryParam("apiType") List<String> apiTypes
    ) {
        var executionContext = GraviteeContext.getExecutionContext();
        var output = getFilterDefinitionsUseCase.execute(
            new GetObservabilityFilterDefinitionsUseCase.Input(
                executionContext.getOrganizationId(),
                executionContext.getEnvironmentId(),
                EnumQueryParam.parse(signals, Signal.class, "signal"),
                EnumQueryParam.parse(apiTypes, ApiType.class, "apiType")
            )
        );
        return new FilterSpecsResponseDto(output.filters().stream().map(FilterSpecDto::from).toList(), output.truncated() ? true : null);
    }

    @GET
    @Path("/{filterName}/values")
    @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponseDto<FilterValueDto> getFilterValues(
        @PathParam("filterName") String filterName,
        @QueryParam("query") String query,
        @QueryParam("from") Long from,
        @QueryParam("to") Long to,
        @QueryParam("page") Integer page,
        @QueryParam("perPage") Integer perPage,
        @QueryParam("apiType") List<String> apiTypes,
        @QueryParam("apiProduct") String apiProduct
    ) {
        return values(filterName, query, from, to, page, perPage, apiTypes, apiProduct);
    }

    @GET
    @Path("/values")
    @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponseDto<FilterValueDto> getFilterValuesByName(
        @QueryParam("name") String name,
        @QueryParam("query") String query,
        @QueryParam("from") Long from,
        @QueryParam("to") Long to,
        @QueryParam("page") Integer page,
        @QueryParam("perPage") Integer perPage,
        @QueryParam("apiType") List<String> apiTypes,
        @QueryParam("apiProduct") String apiProduct
    ) {
        if (name == null || name.isBlank()) {
            throw UnsupportedObservabilityFilterException.nameRequired();
        }
        return values(name, query, from, to, page, perPage, apiTypes, apiProduct);
    }

    private PaginatedResponseDto<FilterValueDto> values(
        String filterName,
        String query,
        Long from,
        Long to,
        Integer page,
        Integer perPage,
        List<String> apiTypes,
        String apiProduct
    ) {
        var output = getFilterValuesUseCase.execute(
            new GetObservabilityFilterValuesUseCase.Input(
                filterName,
                query,
                from,
                to,
                page,
                perPage,
                EnumQueryParam.parse(apiTypes, ApiType.class, "apiType"),
                presentOrNull(apiProduct)
            )
        );
        List<FilterValueDto> data = output.values().data().stream().map(FilterValueDto::from).toList();
        return PaginatedResponseDto.of(data, output.values().totalElements(), output.page(), output.perPage());
    }

    @POST
    @Path("/resolve")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResolveFilterLabelsResponseDto resolveFilterLabels(
        @QueryParam("apiProduct") String apiProduct,
        ResolveFilterLabelsRequestDto request
    ) {
        List<ResolveObservabilityFilterLabelsUseCase.Entry> entries = request == null || request.entries() == null
            ? List.of()
            : request
                .entries()
                .stream()
                .map(e -> new ResolveObservabilityFilterLabelsUseCase.Entry(e.filterName(), e.ids()))
                .toList();
        var output = resolveFilterLabelsUseCase.execute(
            new ResolveObservabilityFilterLabelsUseCase.Input(entries, presentOrNull(apiProduct))
        );
        return ResolveFilterLabelsResponseDto.from(output);
    }

    /** A blank {@code apiProduct} scopes nothing: it reads as absent. */
    private static String presentOrNull(String apiProduct) {
        return apiProduct == null || apiProduct.isBlank() ? null : apiProduct;
    }
}
