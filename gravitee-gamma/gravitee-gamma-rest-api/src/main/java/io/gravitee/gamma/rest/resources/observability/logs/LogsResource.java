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
package io.gravitee.gamma.rest.resources.observability.logs;

import io.gravitee.common.http.MediaType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.logs.exception.LogDetailNotFoundException;
import io.gravitee.gamma.rest.core.observability.logs.exception.MissingLogScopeException;
import io.gravitee.gamma.rest.core.observability.logs.use_case.GetObservabilityLogDetailUseCase;
import io.gravitee.gamma.rest.core.observability.logs.use_case.SearchObservabilityLogsUseCase;
import io.gravitee.gamma.rest.resources.observability.logs.dto.FilterConditionDto;
import io.gravitee.gamma.rest.resources.observability.logs.dto.LogDetailDto;
import io.gravitee.gamma.rest.resources.observability.logs.dto.LogEntryDto;
import io.gravitee.gamma.rest.resources.observability.logs.dto.SearchLogsRequestDto;
import io.gravitee.gamma.rest.resources.tracing.dto.PaginatedResponseDto;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * Environment-wide logs endpoints, mounted under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/observability/logs} by
 * {@code GammaRootResource}.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code POST /search?page=&perPage=} — paginated logs listing. Body carries an optional
 *       {@code timeRange} and optional filter conditions. Pagination is 1-based in the query
 *       string. Response uses the shared paginated envelope
 *       {@code { data, pagination: { totalCount, page, pageCount } }}.</li>
 *   <li>{@code GET /{requestId}?apiId=} — single merged log detail. {@code apiId} is required as a
 *       security defense (same pattern as the trace detail endpoint): prevents cross-API existence
 *       probing. Merges enriched metadata from {@code v4-metrics} with HTTP payloads from
 *       {@code v4-log}. Returns 404 when not found or permission denied (collapse).</li>
 * </ul>
 *
 * <h2>Authorization</h2>
 * <ul>
 *   <li><b>Search:</b> env-level guard ({@code ENVIRONMENT_DASHBOARD:READ} or
 *       {@code ENVIRONMENT_API:READ}). Row-level scoping handled by
 *       {@code AccessibleApiScopeDomainService}.</li>
 *   <li><b>Detail:</b> {@code API_LOG[READ]} on the given {@code apiId}, with 404 collapse.</li>
 * </ul>
 *
 * @author GraviteeSource Team
 */
public class LogsResource {

    @Inject
    private SearchObservabilityLogsUseCase searchLogsUseCase;

    @Inject
    private GetObservabilityLogDetailUseCase getLogDetailUseCase;

    @Inject
    private PermissionService permissionService;

    @POST
    @Path("/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponseDto<LogEntryDto> searchLogs(
        @QueryParam("page") Integer page,
        @QueryParam("perPage") Integer perPage,
        SearchLogsRequestDto request
    ) {
        checkReadObservabilityPermission();

        List<FilterCondition> filters = List.of();
        if (request != null && request.filters() != null) {
            filters = request.filters().stream().map(FilterConditionDto::toCore).toList();
        }

        var ctx = GraviteeContext.getExecutionContext();
        var output = searchLogsUseCase.execute(
            new SearchObservabilityLogsUseCase.Input(
                ctx.getOrganizationId(),
                ctx.getEnvironmentId(),
                filters,
                request != null && request.timeRange() != null ? request.timeRange().from() : null,
                request != null && request.timeRange() != null ? request.timeRange().to() : null,
                page,
                perPage
            )
        );

        List<LogEntryDto> data = output.data().data().stream().map(LogEntryDto::from).toList();
        return PaginatedResponseDto.of(data, output.data().totalCount(), output.page(), output.perPage());
    }

    /**
     * Fetches a single merged log detail by request id. {@code apiId} is required as a security
     * defense: prevents cross-API existence probing (a known request id from API_B cannot be read
     * via API_A). Permission is {@code API_LOG[READ]} on the given API; 404 collapse when the log
     * doesn't exist OR the caller can't access that API.
     */
    @GET
    @Path("/{requestId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogDetail(@PathParam("requestId") String requestId, @QueryParam("apiId") String apiId) {
        requireParam("apiId", apiId);
        checkApiLogReadPermissionOrCollapse(apiId, requestId);

        var ctx = GraviteeContext.getExecutionContext();
        var output = getLogDetailUseCase.execute(
            new GetObservabilityLogDetailUseCase.Input(ctx.getOrganizationId(), ctx.getEnvironmentId(), apiId, requestId)
        );
        return output
            .detail()
            .map(detail -> Response.ok(LogDetailDto.from(detail)).build())
            .orElseThrow(() -> new LogDetailNotFoundException(requestId, apiId));
    }

    private static void requireParam(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new MissingLogScopeException(name);
        }
    }

    /**
     * Checks {@code API_LOG[READ]} on the given API. Returns 404 (not 403) when denied, so the
     * response is indistinguishable from "log doesn't exist" — prevents cross-API existence probing.
     */
    private void checkApiLogReadPermissionOrCollapse(String apiId, String requestId) {
        ExecutionContext ctx = GraviteeContext.getExecutionContext();
        boolean allowed = permissionService.hasPermission(ctx, RolePermission.API_LOG, apiId, RolePermissionAction.READ);
        if (!allowed) {
            throw new LogDetailNotFoundException(requestId, apiId);
        }
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
