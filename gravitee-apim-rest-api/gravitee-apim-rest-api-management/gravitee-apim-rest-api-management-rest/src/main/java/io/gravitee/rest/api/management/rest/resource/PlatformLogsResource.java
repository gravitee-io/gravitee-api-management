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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.rest.api.model.permissions.RolePermission.API_LOG;
import static io.gravitee.rest.api.model.permissions.RolePermission.APPLICATION_LOG;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static java.lang.String.format;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.model.wrapper.PlatformRequestItemSearchLogResponse;
import io.gravitee.rest.api.management.rest.resource.param.LogsParam;
import io.gravitee.rest.api.model.analytics.query.LogQuery;
import io.gravitee.rest.api.model.log.ApiRequest;
import io.gravitee.rest.api.model.log.PlatformRequestItem;
import io.gravitee.rest.api.model.log.SearchLogResponse;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.LogsService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Platform Logs")
public class PlatformLogsResource extends AbstractResource {

    @Inject
    private LogsService logsService;

    @Inject
    private ApplicationService applicationService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get platform logs", description = "User must have the MANAGEMENT_PLATFORM[READ] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Platform logs",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = PlatformRequestItemSearchLogResponse.class)
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_PLATFORM, acls = RolePermissionAction.READ) })
    public SearchLogResponse<PlatformRequestItem> getPlatformLogs(@BeanParam LogsParam param) {
        param.validate();

        LogQuery logQuery = new LogQuery();
        logQuery.setQuery(param.getQuery());
        logQuery.setPage(param.getPage());
        logQuery.setSize(param.getSize());
        logQuery.setFrom(param.getFrom());
        logQuery.setTo(param.getTo());
        logQuery.setField(param.getField());
        logQuery.setOrder(param.isOrder());

        Map<String, Set<String>> terms = getExtraFilterForAccessibleAPIsAndApplications();
        logQuery.setTerms(terms);
        if (terms.isEmpty()) {
            return new SearchLogResponse<>(0);
        }

        return logsService.findPlatform(GraviteeContext.getExecutionContext(), logQuery);
    }

    private Map<String, Set<String>> getExtraFilterForAccessibleAPIsAndApplications() {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        Map<String, Set<String>> terms = new HashMap<>();

        Set<String> applicationIds;
        if (isAdmin()) {
            applicationIds = applicationService.findIdsByEnvironment(executionContext);
        } else {
            applicationIds = applicationService.findIdsByUserAndPermission(
                executionContext,
                getAuthenticatedUser(),
                null,
                APPLICATION_LOG,
                READ
            );
        }
        if (!applicationIds.isEmpty()) {
            terms.put("application", applicationIds);
        }

        Set<String> apiIds;
        if (isAdmin()) {
            apiIds = apiAuthorizationService.findIdsByEnvironment(executionContext.getEnvironmentId());
        } else {
            apiIds = apiAuthorizationService
                .findIdsByUser(executionContext, getAuthenticatedUser(), null, true)
                .stream()
                .filter(appId -> permissionService.hasPermission(executionContext, API_LOG, appId, READ))
                .collect(Collectors.toSet());
        }
        if (!apiIds.isEmpty()) {
            terms.put("api", apiIds);
        }

        return terms;
    }

    @GET
    @Path("/{log}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a specific log", description = "User must have the MANAGEMENT_PLATFORM[READ] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Single log",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiRequest.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_PLATFORM, acls = RolePermissionAction.READ) })
    public ApiRequest getPlatformLog(@PathParam("log") String logId, @QueryParam("timestamp") Long timestamp) {
        return logsService.findApiLog(GraviteeContext.getExecutionContext(), logId, timestamp);
    }

    @GET
    @Path("export")
    @Produces("text/csv")
    @Operation(
        summary = "Export platform logs as CSV",
        description = "User must have the MANAGEMENT_PLATFORM[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Platform logs as CSV",
        content = @Content(mediaType = "text/csv", schema = @Schema(type = "string"))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_PLATFORM, acls = RolePermissionAction.READ) })
    public Response exportPlatformLogsAsCSV(@BeanParam LogsParam param) {
        final SearchLogResponse<PlatformRequestItem> searchLogResponse = getPlatformLogs(param);
        return Response.ok(logsService.exportAsCsv(GraviteeContext.getExecutionContext(), searchLogResponse))
            .header(HttpHeaders.CONTENT_DISPOSITION, format("attachment;filename=logs-%s-%s.csv", "platform", System.currentTimeMillis()))
            .build();
    }
}
