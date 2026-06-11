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
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
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
 *   <li>{@code GET /{filterName}/values?query=&page=&perPage=} — paginated selectable values for one
 *       filter. ENUM filters return their labelled static values (optionally substring-filtered by
 *       {@code query}); other types are handled in the use case.</li>
 * </ul>
 *
 * <h2>Authorization</h2>
 * Coarse env-level metadata guard only (no per-row data scoping): the caller must be able to read
 * dashboards or APIs in the current environment, otherwise {@code 403}. Gamma resources do not
 * extend the apim management {@code AbstractResource}, so the guard is implemented here against the
 * injected {@link PermissionService} and the {@link GraviteeContext} execution context.
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

    @Inject
    private PermissionService permissionService;

    @GET
    @Path("/definition")
    @Produces(MediaType.APPLICATION_JSON)
    public FilterSpecsResponseDto getFilterDefinitions(
        @QueryParam("signal") List<String> signals,
        @QueryParam("apiType") List<String> apiTypes
    ) {
        checkReadObservabilityPermission();
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
        @QueryParam("perPage") Integer perPage
    ) {
        checkReadObservabilityPermission();
        var output = getFilterValuesUseCase.execute(
            new GetObservabilityFilterValuesUseCase.Input(filterName, query, from, to, page, perPage)
        );
        List<FilterValueDto> data = output.values().data().stream().map(FilterValueDto::from).toList();
        return PaginatedResponseDto.of(data, output.values().totalElements(), output.page(), output.perPage());
    }

    @POST
    @Path("/resolve")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResolveFilterLabelsResponseDto resolveFilterLabels(ResolveFilterLabelsRequestDto request) {
        checkReadObservabilityPermission();
        List<ResolveObservabilityFilterLabelsUseCase.Entry> entries = request == null || request.entries() == null
            ? List.of()
            : request
                .entries()
                .stream()
                .map(e -> new ResolveObservabilityFilterLabelsUseCase.Entry(e.filterName(), e.ids()))
                .toList();
        var output = resolveFilterLabelsUseCase.execute(new ResolveObservabilityFilterLabelsUseCase.Input(entries));
        return ResolveFilterLabelsResponseDto.from(output);
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

    private void checkReadObservabilityPermission() {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        String environmentId = executionContext.getEnvironmentId();
        boolean allowed =
            permissionService.hasPermission(
                executionContext,
                RolePermission.ENVIRONMENT_DASHBOARD,
                environmentId,
                RolePermissionAction.READ
            ) ||
            permissionService.hasPermission(executionContext, RolePermission.ENVIRONMENT_API, environmentId, RolePermissionAction.READ);
        if (!allowed) {
            throw new ForbiddenAccessException();
        }
    }
}
