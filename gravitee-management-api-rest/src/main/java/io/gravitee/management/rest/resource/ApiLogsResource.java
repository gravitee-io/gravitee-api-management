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
import io.gravitee.management.model.log.ApiRequest;
import io.gravitee.management.model.log.SearchLogResponse;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.resource.param.LogsParam;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.LogsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static java.lang.String.format;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API"})
public class ApiLogsResource extends AbstractResource {

    @Inject
    private LogsService logsService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get API logs")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API logs"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({@Permission(value = RolePermission.API_LOG, acls = RolePermissionAction.READ)})
    public SearchLogResponse apiLogs(
            @PathParam("api") String api,
            @BeanParam LogsParam param) {

        param.validate();

        LogQuery logQuery = new LogQuery();
        logQuery.setQuery(param.getQuery());
        logQuery.setPage(param.getPage());
        logQuery.setSize(param.getSize());
        logQuery.setFrom(param.getFrom());
        logQuery.setTo(param.getTo());
        logQuery.setField(param.getField());
        logQuery.setOrder(param.isOrder());

        return logsService.findByApi(api, logQuery);
    }

    @GET
    @Path("/{log}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a specific log")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Single log"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({@Permission(value = RolePermission.API_LOG, acls = RolePermissionAction.READ)})
    public ApiRequest apiLog(
            @PathParam("api") String api,
            @PathParam("log") String logId,
            @QueryParam("timestamp") Long timestamp) {
        return logsService.findApiLog(logId, timestamp);
    }

    @GET
    @Path("export")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Export API logs as CSV")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API logs as CSV"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({@Permission(value = RolePermission.API_LOG, acls = RolePermissionAction.READ)})
    public Response exportAPILogsAsCSV(
            @PathParam("api") String api,
            @BeanParam LogsParam param) {
        final SearchLogResponse searchLogResponse = apiLogs(api, param);
        return Response
                .ok(logsService.exportAsCsv(searchLogResponse))
                .header(HttpHeaders.CONTENT_DISPOSITION, format("attachment;filename=logs-%s-%s.csv", api, System.currentTimeMillis()))
                .build();
    }
}
