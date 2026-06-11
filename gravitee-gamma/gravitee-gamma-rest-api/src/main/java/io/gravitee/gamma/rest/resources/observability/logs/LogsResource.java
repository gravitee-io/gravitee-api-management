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
import io.gravitee.gamma.rest.core.observability.logs.use_case.SearchObservabilityLogsUseCase;
import io.gravitee.gamma.rest.resources.observability.logs.dto.FilterConditionDto;
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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import java.util.List;

/**
 * Environment-wide logs search endpoint, mounted under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/observability/logs} by
 * {@code GammaRootResource}.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code POST /search?page=&perPage=} — paginated logs listing. Body carries an optional
 *       {@code timeRange} and optional filter conditions. Pagination is 1-based in the query
 *       string. Response uses the shared paginated envelope
 *       {@code { data, pagination: { totalCount, page, pageCount } }}.</li>
 * </ul>
 *
 * <h2>Authorization</h2>
 * Same coarse env-level guard as the filter catalog: the caller must be able to read dashboards
 * or APIs in the current environment, otherwise {@code 403}. Row-level scoping (which APIs the
 * caller sees) is handled server-side by the use case via {@code AccessibleApiScopeDomainService}.
 *
 * @author GraviteeSource Team
 */
public class LogsResource {

    @Inject
    private SearchObservabilityLogsUseCase searchLogsUseCase;

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
