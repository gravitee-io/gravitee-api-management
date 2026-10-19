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
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.use_case.GetObservabilityFilterDefinitionsUseCase;
import io.gravitee.gamma.rest.core.observability.filter.use_case.GetObservabilityFilterValuesUseCase;
import io.gravitee.gamma.rest.core.observability.filter.use_case.ResolveObservabilityFilterLabelsUseCase;
import io.gravitee.gamma.rest.resources.observability.filters.dto.FilterSpecDto;
import io.gravitee.gamma.rest.resources.observability.filters.dto.FilterSpecsResponseDto;
import io.gravitee.gamma.rest.resources.observability.filters.dto.FilterValueDto;
import io.gravitee.gamma.rest.resources.observability.filters.dto.ResolveFilterLabelsRequestDto;
import io.gravitee.gamma.rest.resources.observability.filters.dto.ResolveFilterLabelsResponseDto;
import io.gravitee.gamma.rest.resources.tracing.dto.PaginatedResponseDto;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Unified observability filter discovery surface, shared across every gamma module's logs and
 * analytics UI. Mounted under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/observability/filters} by
 * {@code GammaRootResource}.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code GET /definition?signal=&apiType=} — self-describing filter catalog. Both axes are
 *       optional and multi-valued: an absent axis is unconstrained, both absent yields the full
 *       catalog; a present axis narrows by set intersection. Each returned spec carries both axes so
 *       the consumer can route a filter to the right panel (logs table vs analytics histogram).</li>
 *   <li>{@code GET /{filterName}/values?query=&page=&perPage=&apiProduct=} — paginated selectable values for one
 *       filter. ENUM filters return their labelled static values (optionally substring-filtered by
 *       {@code query}); other types are handled in the use case.</li>
 *   <li>{@code POST /resolve?apiProduct=} — labels of stored filter ids, grouped per filter.</li>
 * </ul>
 *
 * <h2>Authorization</h2>
 * No permission guard: the catalog holds no traffic data, and values and labels come from the rows the
 * caller reads, so a caller who reads no API gets empty answers. A request scoped to one AI workspace
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
        var output = getFilterDefinitionsUseCase.execute(
            new GetObservabilityFilterDefinitionsUseCase.Input(
                parse(signals, Signal.class, Signal::valueOf, "signal"),
                parse(apiTypes, ApiType.class, ApiType::valueOf, "apiType")
            )
        );
        return new FilterSpecsResponseDto(output.filters().stream().map(FilterSpecDto::from).toList());
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
        var output = getFilterValuesUseCase.execute(
            new GetObservabilityFilterValuesUseCase.Input(
                filterName,
                query,
                from,
                to,
                page,
                perPage,
                parse(apiTypes, ApiType.class, ApiType::valueOf, "apiType"),
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

    /**
     * Parses a multi-valued query param into an {@link EnumSet}, case-insensitively. {@code null} /
     * empty means "unconstrained" (empty set). An unknown token is a client error → {@code 400}.
     */
    private static <E extends Enum<E>> Set<E> parse(List<String> raw, Class<E> type, Function<String, E> valueOf, String paramName) {
        if (raw == null || raw.isEmpty()) {
            return Set.of();
        }
        Set<E> result = EnumSet.noneOf(type);
        for (String token : raw) {
            if (token == null || token.isBlank()) {
                continue;
            }
            try {
                result.add(valueOf.apply(token.trim().toUpperCase(java.util.Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Unknown " + paramName + " value '" + token + "'");
            }
        }
        return result;
    }
}
