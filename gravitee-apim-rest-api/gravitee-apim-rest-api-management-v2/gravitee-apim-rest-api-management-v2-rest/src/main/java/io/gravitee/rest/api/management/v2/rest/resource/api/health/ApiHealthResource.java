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

import io.gravitee.apim.core.api_health.use_case.SearchAverageHealthCheckResponseTimeOvertimeUseCase;
import io.gravitee.apim.core.api_health.use_case.SearchAverageHealthCheckResponseTimeUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiHealthMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiHealthAverageResponseTimeOvertimeResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiHealthAverageResponseTimeResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class ApiHealthResource extends AbstractResource {

    @PathParam("apiId")
    private String apiId;

    @Inject
    private SearchAverageHealthCheckResponseTimeUseCase searchAverageHealthCheckResponseTimeUseCase;

    @Inject
    private SearchAverageHealthCheckResponseTimeOvertimeUseCase searchAverageHealthCheckResponseTimeOvertimeUseCase;

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
            .averageHealthCheckResponseTime()
            .map(ApiHealthMapper.INSTANCE::map)
            .orElse(null);
    }

    @Path("/average-response-time-overtime")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_HEALTH, acls = RolePermissionAction.READ) })
    public ApiHealthAverageResponseTimeOvertimeResponse getAverageResponseTimeOvertime(
        @QueryParam("from") Long from,
        @QueryParam("to") Long to,
        @QueryParam("interval") Long interval
    ) {
        var context = GraviteeContext.getExecutionContext();
        return searchAverageHealthCheckResponseTimeOvertimeUseCase
            .execute(
                new SearchAverageHealthCheckResponseTimeOvertimeUseCase.Input(
                    context.getOrganizationId(),
                    context.getEnvironmentId(),
                    apiId,
                    Instant.ofEpochMilli(from),
                    Instant.ofEpochMilli(to),
                    Duration.ofMillis(interval)
                )
            )
            .averageHealthCheckResponseTimeOvertime()
            .map(ApiHealthMapper.INSTANCE::map)
            .orElse(null);
    }
}
