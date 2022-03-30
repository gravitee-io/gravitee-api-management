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

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.analytics.query.LogQuery;
import io.gravitee.rest.api.model.log.ApplicationRequest;
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
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.LogsService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationLogsResource extends AbstractResource {

    @Inject
    private LogsService logsService;

    @Inject
    private LogMapper logMapper;

    @Inject
    private ApplicationService applicationService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_LOG, acls = RolePermissionAction.READ) })
    public Response applicationLogs(
        @PathParam("applicationId") String applicationId,
        @BeanParam PaginationParam paginationParam,
        @BeanParam LogsParam logsParam
    ) {
        //Does application exists ?
        applicationService.findById(GraviteeContext.getExecutionContext(), applicationId);

        final SearchLogResponse<ApplicationRequestItem> searchLogResponse = getSearchLogResponse(applicationId, paginationParam, logsParam);

        List<Log> logs = searchLogResponse.getLogs().stream().map(logMapper::convert).collect(Collectors.toList());

        final Map<String, Object> metadataTotal = new HashMap<>();
        metadataTotal.put(METADATA_DATA_TOTAL_KEY, searchLogResponse.getTotal());

        final Map<String, Map<String, Object>> metadata = searchLogResponse.getMetadata() == null
            ? new HashMap()
            : new HashMap(searchLogResponse.getMetadata());
        metadata.put(METADATA_DATA_KEY, metadataTotal);
        //No pagination, because logsService did it already
        return createListResponse(GraviteeContext.getExecutionContext(), logs, paginationParam, metadata, false);
    }

    @SuppressWarnings("unchecked")
    protected SearchLogResponse<ApplicationRequestItem> getSearchLogResponse(
        String applicationId,
        PaginationParam paginationParam,
        LogsParam logsParam
    ) {
        logsParam.validate();

        LogQuery logQuery = new LogQuery();
        logQuery.setPage(paginationParam.getPage());
        logQuery.setSize(paginationParam.getSize());

        logQuery.setQuery(logsParam.getQuery());
        logQuery.setFrom(logsParam.getFrom());
        logQuery.setTo(logsParam.getTo());
        logQuery.setField(logsParam.getField());
        logQuery.setOrder(!"DESC".equals(logsParam.getOrder()));

        return logsService.findByApplication(GraviteeContext.getExecutionContext(), applicationId, logQuery);
    }

    @GET
    @Path("/{logId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_LOG, acls = RolePermissionAction.READ) })
    public Response applicationLog(
        @PathParam("applicationId") String applicationId,
        @PathParam("logId") String logId,
        @QueryParam("timestamp") Long timestamp
    ) {
        //Does application exists ?
        applicationService.findById(GraviteeContext.getExecutionContext(), applicationId);

        ApplicationRequest applicationLogs = logsService.findApplicationLog(GraviteeContext.getExecutionContext(), logId, timestamp);

        return Response.ok(logMapper.convert(applicationLogs)).build();
    }

    @POST
    @Path("_export")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_LOG, acls = RolePermissionAction.READ) })
    public Response exportApplicationLogsAsCSV(
        @PathParam("applicationId") String applicationId,
        @BeanParam PaginationParam paginationParam,
        @BeanParam LogsParam logsParam
    ) {
        //Does application exists ?
        applicationService.findById(GraviteeContext.getExecutionContext(), applicationId);
        final SearchLogResponse<ApplicationRequestItem> searchLogResponse = getSearchLogResponse(applicationId, paginationParam, logsParam);
        return Response
            .ok(logsService.exportAsCsv(GraviteeContext.getExecutionContext(), searchLogResponse))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                format("attachment;filename=logs-%s-%s.csv", applicationId, System.currentTimeMillis())
            )
            .build();
    }
}
