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
package io.gravitee.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.analytics.query.LogQuery;
import io.gravitee.management.model.healthcheck.Log;
import io.gravitee.management.model.healthcheck.SearchLogResponse;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.resource.param.healthcheck.HealthcheckFieldParam;
import io.gravitee.management.rest.resource.param.healthcheck.HealthcheckTypeParam;
import io.gravitee.management.rest.resource.param.healthcheck.LogsParam;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.HealthCheckService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API"})
public class ApiHealthResource extends AbstractResource {

    @Inject
    private HealthCheckService healthCheckService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Health-check statistics for API")
    @Permissions({
            @Permission(value = RolePermission.API_HEALTH, acls = RolePermissionAction.READ)
    })
    public Response health(
            @PathParam("api") String api,
            @QueryParam("type") @DefaultValue("availability") HealthcheckTypeParam healthcheckTypeParam,
            @QueryParam("field") @DefaultValue("endpoint") HealthcheckFieldParam healthcheckFieldParam) {

        switch (healthcheckTypeParam.getValue()) {
            case RESPONSE_TIME:
                return Response.ok(healthCheckService.getResponseTime(api, healthcheckFieldParam.getValue().name())).build();
            default:
                return Response.ok(healthCheckService.getAvailability(api, healthcheckFieldParam.getValue().name())).build();
        }
    }

    @GET
    @Path("logs")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Health-check logs")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API logs"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({@Permission(value = RolePermission.API_HEALTH, acls = RolePermissionAction.READ)})
    public SearchLogResponse healthcheckLogs(
            @PathParam("api") String api,
            @BeanParam LogsParam param) {

        param.validate();

        LogQuery logQuery = new LogQuery();
        logQuery.setQuery(param.getQuery());
        logQuery.setPage(param.getPage());
        logQuery.setSize(param.getSize());

        return healthCheckService.findByApi(api, logQuery);
    }

    @GET
    @Path("logs/{log}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Health-check log")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Single health-check log"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({@Permission(value = RolePermission.API_HEALTH, acls = RolePermissionAction.READ)})
    public Log healthcheckLog(
            @PathParam("api") String api,
            @PathParam("log") String logId) {

        return healthCheckService.findLog(logId);
    }
}
