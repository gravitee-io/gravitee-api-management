/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.rest.resource;

import static java.util.Collections.singletonList;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.param.AnalyticsAverageParam;
import io.gravitee.rest.api.management.rest.resource.param.healthcheck.HealthcheckField;
import io.gravitee.rest.api.management.rest.resource.param.healthcheck.HealthcheckType;
import io.gravitee.rest.api.management.rest.resource.param.healthcheck.LogsParam;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.analytics.Analytics;
import io.gravitee.rest.api.model.analytics.query.Aggregation;
import io.gravitee.rest.api.model.analytics.query.AggregationType;
import io.gravitee.rest.api.model.analytics.query.DateHistogramQuery;
import io.gravitee.rest.api.model.analytics.query.LogQuery;
import io.gravitee.rest.api.model.healthcheck.Log;
import io.gravitee.rest.api.model.healthcheck.SearchLogResponse;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.HealthCheckService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "API Health")
public class ApiHealthResource extends AbstractResource {

    @Inject
    private HealthCheckService healthCheckService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @Parameter(name = "api", hidden = true)
    private String api;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Health-check statistics for API")
    @Permissions({ @Permission(value = RolePermission.API_HEALTH, acls = RolePermissionAction.READ) })
    public Response getApiHealth(
        @QueryParam("type") @DefaultValue("availability") HealthcheckType healthcheckType,
        @QueryParam("field") @DefaultValue("endpoint") HealthcheckField healthcheckField
    ) {
        switch (healthcheckType) {
            case RESPONSE_TIME:
                return Response
                    .ok(healthCheckService.getResponseTime(GraviteeContext.getExecutionContext(), api, healthcheckField.name()))
                    .build();
            default:
                return Response
                    .ok(healthCheckService.getAvailability(GraviteeContext.getExecutionContext(), api, healthcheckField.name()))
                    .build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Health-check average statistics for API")
    @Permissions({ @Permission(value = RolePermission.API_HEALTH, acls = RolePermissionAction.READ) })
    @Path("/average")
    public Response getApiHealthAverage(@BeanParam AnalyticsAverageParam analyticsAverageParam) {
        return Response.ok(executeDateHisto(api, analyticsAverageParam)).build();
    }

    private Analytics executeDateHisto(final String api, final AnalyticsAverageParam analyticsAverageParam) {
        final DateHistogramQuery query = new DateHistogramQuery();
        query.setFrom(analyticsAverageParam.getFrom());
        query.setTo(analyticsAverageParam.getTo());
        query.setInterval(analyticsAverageParam.getInterval());
        query.setRootField("api");
        query.setRootIdentifier(api);
        query.setAggregations(
            singletonList(
                new Aggregation() {
                    @Override
                    public AggregationType type() {
                        switch (analyticsAverageParam.getType()) {
                            case AVAILABILITY:
                                return AggregationType.FIELD;
                            default:
                                return AggregationType.AVG;
                        }
                    }

                    @Override
                    public String field() {
                        switch (analyticsAverageParam.getType()) {
                            case AVAILABILITY:
                                return "available";
                            default:
                                return "response-time";
                        }
                    }
                }
            )
        );
        return healthCheckService.query(query);
    }

    @GET
    @Path("logs")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Health-check logs")
    @ApiResponse(
        responseCode = "200",
        description = "API logs",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SearchLogResponse.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_HEALTH, acls = RolePermissionAction.READ) })
    public SearchLogResponse getApiHealthCheckLogs(@BeanParam LogsParam param) {
        param.validate();

        LogQuery logQuery = new LogQuery();
        logQuery.setQuery(param.getQuery());
        logQuery.setPage(param.getPage());
        logQuery.setSize(param.getSize());
        logQuery.setFrom(param.getFrom());
        logQuery.setTo(param.getTo());

        return healthCheckService.findByApi(GraviteeContext.getExecutionContext(), api, logQuery, param.isTransition());
    }

    @GET
    @Path("logs/{log}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Health-check log")
    @ApiResponse(
        responseCode = "200",
        description = "Single health-check log",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Log.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_HEALTH, acls = RolePermissionAction.READ) })
    public Log getApiHealthCheckLog(@PathParam("log") String logId) {
        return healthCheckService.findLog(logId);
    }
}
