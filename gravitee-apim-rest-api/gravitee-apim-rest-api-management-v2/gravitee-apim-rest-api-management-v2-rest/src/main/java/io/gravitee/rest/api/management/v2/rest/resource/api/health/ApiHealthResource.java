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
package io.gravitee.rest.api.management.v2.rest.resource.api.health;

import io.gravitee.apim.core.api_health.use_case.AvailabilityUseCase;
import io.gravitee.apim.core.api_health.use_case.SearchAverageHealthCheckResponseTimeOvertimeUseCase;
import io.gravitee.apim.core.api_health.use_case.SearchAverageHealthCheckResponseTimeUseCase;
import io.gravitee.apim.core.api_health.use_case.SearchHealthCheckLogsUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiHealthMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiHealthAvailabilityResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiHealthAverageResponseTimeOvertimeResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiHealthAverageResponseTimeResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiHealthLogsResponse;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class ApiHealthResource extends AbstractResource {

    @PathParam("apiId")
    private String apiId;

    @Inject
    private SearchAverageHealthCheckResponseTimeUseCase searchAverageHealthCheckResponseTimeUseCase;

    @Inject
    private AvailabilityUseCase availabilityUseCase;

    @Inject
    private SearchAverageHealthCheckResponseTimeOvertimeUseCase searchAverageHealthCheckResponseTimeOvertimeUseCase;

    @Inject
    private SearchHealthCheckLogsUseCase searchHealthCheckLogsUseCase;

    @Path("/average-response-time")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_HEALTH, acls = RolePermissionAction.READ) })
    public ApiHealthAverageResponseTimeResponse getAverageResponseTime(
        @QueryParam("field") String field,
        @QueryParam("from") Long from,
        @QueryParam("to") Long to
    ) {
        var context = GraviteeContext.getExecutionContext();
        return searchAverageHealthCheckResponseTimeUseCase
            .execute(
                new SearchAverageHealthCheckResponseTimeUseCase.Input(
                    context.getOrganizationId(),
                    context.getEnvironmentId(),
                    apiId,
                    field,
                    Instant.ofEpochMilli(from),
                    Instant.ofEpochMilli(to)
                )
            )
            .map(SearchAverageHealthCheckResponseTimeUseCase.Output::averageHealthCheckResponseTime)
            .map(ApiHealthMapper.INSTANCE::map)
            .blockingGet();
    }

    @Path("/average-response-time-overtime")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_HEALTH, acls = RolePermissionAction.READ) })
    public ApiHealthAverageResponseTimeOvertimeResponse getAverageResponseTimeOvertime(
        @QueryParam("from") Long from,
        @QueryParam("to") Long to
    ) {
        var context = GraviteeContext.getExecutionContext();
        return searchAverageHealthCheckResponseTimeOvertimeUseCase
            .execute(
                new SearchAverageHealthCheckResponseTimeOvertimeUseCase.Input(
                    context.getOrganizationId(),
                    context.getEnvironmentId(),
                    apiId,
                    Instant.ofEpochMilli(from),
                    Instant.ofEpochMilli(to)
                )
            )
            .map(SearchAverageHealthCheckResponseTimeOvertimeUseCase.Output::averageHealthCheckResponseTimeOvertime)
            .map(ApiHealthMapper.INSTANCE::map)
            .blockingGet();
    }

    @Path("availability")
    @GET
    @Produces(io.gravitee.common.http.MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_HEALTH, acls = RolePermissionAction.READ) })
    public ApiHealthAvailabilityResponse getApiById(
        @QueryParam("from") long from,
        @QueryParam("to") long to,
        @QueryParam("field") String field
    ) {
        Instant since = Instant.ofEpochMilli(from);
        Instant until = Instant.ofEpochMilli(to);
        return availabilityUseCase
            .execute(new AvailabilityUseCase.Input(GraviteeContext.getExecutionContext(), since, until, apiId, field))
            .map(output -> new ApiHealthAvailabilityResponse().global(output.global()).group(output.byField()))
            .blockingGet();
    }

    @Path("/logs")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_HEALTH, acls = RolePermissionAction.READ) })
    public ApiHealthLogsResponse getHealthCheckLogs(
        @QueryParam("from") Long from,
        @QueryParam("to") Long to,
        @QueryParam("success") Boolean success,
        @BeanParam @Valid PaginationParam paginationParam
    ) {
        var context = GraviteeContext.getExecutionContext();
        var result = searchHealthCheckLogsUseCase
            .execute(
                new SearchHealthCheckLogsUseCase.Input(
                    context.getOrganizationId(),
                    context.getEnvironmentId(),
                    apiId,
                    Instant.ofEpochMilli(from),
                    Instant.ofEpochMilli(to),
                    Optional.ofNullable(success),
                    Optional.ofNullable(paginationParam).map(PaginationParam::toPageable)
                )
            )
            .blockingGet();

        if (result == null) {
            return new ApiHealthLogsResponse()
                .data(List.of())
                .pagination(PaginationInfo.computePaginationInfo(0, 0, paginationParam))
                .links(computePaginationLinks(0, paginationParam));
        }

        var page = result.logs();
        return new ApiHealthLogsResponse()
            .data(page.getContent().stream().map(ApiHealthMapper.INSTANCE::map).toList())
            .pagination(
                PaginationInfo.computePaginationInfo(page.getTotalElements(), Math.toIntExact(page.getPageElements()), paginationParam)
            )
            .links(computePaginationLinks(page.getTotalElements(), paginationParam));
    }
}
