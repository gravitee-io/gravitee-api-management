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
package io.gravitee.rest.api.portal.rest.resource;

import static java.lang.String.format;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.analytics.query.LogQuery;
import io.gravitee.rest.api.model.log.ApplicationRequestItem;
import io.gravitee.rest.api.model.log.SearchLogResponse;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.LogMapper;
import io.gravitee.rest.api.portal.rest.model.Log;
import io.gravitee.rest.api.portal.rest.resource.param.LogsParam;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.security.Permission;
import io.gravitee.rest.api.portal.rest.security.Permissions;
import io.gravitee.rest.api.service.LogsService;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationLogsResource extends AbstractResource {

    @Inject
    private LogsService logsService;

    @Inject
    private LogMapper logMapper;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_LOG, acls = RolePermissionAction.READ)
    })
    public Response applicationLogs(
            @PathParam("applicationId") String applicationId,
            @BeanParam PaginationParam paginationParam,
            @BeanParam LogsParam logsParam) {
        final SearchLogResponse<ApplicationRequestItem> searchLogResponse = getSearchLogResponse(applicationId,
                paginationParam, logsParam);
        
        List<Log> logs = searchLogResponse.getLogs().stream()
                .map(logMapper::convert)
                .collect(Collectors.toList());
        
        //No pagination, because logsService did it already
        return createListResponse(logs, paginationParam, searchLogResponse.getMetadata(), false);
    }

    protected SearchLogResponse<ApplicationRequestItem> getSearchLogResponse(String applicationId,
            PaginationParam paginationParam, LogsParam logsParam) {
        logsParam.validate();

        LogQuery logQuery = new LogQuery();
        logQuery.setPage(paginationParam.getPage());
        logQuery.setSize(paginationParam.getSize());
        
        logQuery.setQuery(logsParam.getQuery());
        logQuery.setFrom(logsParam.getFrom());
        logQuery.setTo(logsParam.getTo());
        logQuery.setField(logsParam.getField());
        logQuery.setOrder(!"DESC".equals(logsParam.getOrder()));

        return logsService.findByApplication(applicationId, logQuery);
    }

    @GET
    @Path("/{logId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_LOG, acls = RolePermissionAction.READ)
    })
    public Response applicationLog(
            @PathParam("applicationId") String applicationId,
            @PathParam("logId") String logId,
            @QueryParam("timestamp") Long timestamp) {
        
        return Response
                .ok(logMapper.convert(logsService.findApplicationLog(logId, timestamp)))
                .build();
    }

    @POST
    @Path("_export")
    @Produces(MediaType.TEXT_PLAIN)
    @Permissions({
        @Permission(value = RolePermission.APPLICATION_LOG, acls = RolePermissionAction.READ)
    })
    public Response exportApplicationLogsAsCSV(
            @PathParam("applicationId") String applicationId,
            @BeanParam PaginationParam paginationParam,
            @BeanParam LogsParam logsParam) {
        final SearchLogResponse searchLogResponse = getSearchLogResponse(applicationId, paginationParam, logsParam);
        return Response
                .ok(logsService.exportAsCsv(searchLogResponse))
                .header(HttpHeaders.CONTENT_DISPOSITION, format("attachment;filename=logs-%s-%s.csv", applicationId, System.currentTimeMillis()))
                .build();
    }

}
