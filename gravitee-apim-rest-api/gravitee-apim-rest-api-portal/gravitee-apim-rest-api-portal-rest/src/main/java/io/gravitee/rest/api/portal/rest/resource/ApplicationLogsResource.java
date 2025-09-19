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
package io.gravitee.rest.api.portal.rest.resource;

import static java.lang.String.format;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.log.model.ConnectionLog;
import io.gravitee.apim.core.log.use_case.SearchApiConnectionLogDetailUseCase;
import io.gravitee.apim.core.log.use_case.SearchApiMessageLogsUseCase;
import io.gravitee.apim.core.log.use_case.SearchApplicationConnectionLogsUseCase;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import io.gravitee.rest.api.model.analytics.query.LogQuery;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.log.ApplicationRequestItem;
import io.gravitee.rest.api.model.log.LogMetadata;
import io.gravitee.rest.api.model.log.SearchLogResponse;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.LogMapper;
import io.gravitee.rest.api.portal.rest.model.Log;
import io.gravitee.rest.api.portal.rest.resource.param.LogsParam;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.resource.param.SearchApplicationLogsParam;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.LogsService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationLogsResource extends AbstractResource {

    @Inject
    private LogsService logsService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private SearchApplicationConnectionLogsUseCase searchApplicationConnectionLogsUseCase;

    @Inject
    private SearchApiConnectionLogDetailUseCase searchApiConnectionLogDetailUseCase;

    @Inject
    private SearchApiMessageLogsUseCase searchMessageLogsUseCase;

    private final LogMapper logMapper = LogMapper.INSTANCE;

    @GET
    @Deprecated
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_LOG, acls = RolePermissionAction.READ) })
    public Response applicationLogs_deprecated(
        @PathParam("applicationId") String applicationId,
        @Valid @BeanParam PaginationParam paginationParam,
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

    @POST
    @Path("_search")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_LOG, acls = RolePermissionAction.READ) })
    public Response searchApplicationLogs(
        @PathParam("applicationId") String applicationId,
        @Valid @BeanParam PaginationParam paginationParam,
        final @Valid @NotNull SearchApplicationLogsParam searchLogsParam
    ) {
        searchLogsParam.validate();

        var executionContext = GraviteeContext.getExecutionContext();

        Optional<Pageable> pageable = Optional.ofNullable(
            paginationParam.hasPagination() ? new PageableImpl(paginationParam.getPage(), paginationParam.getSize()) : null
        );

        var result = searchApplicationConnectionLogsUseCase.execute(
            new SearchApplicationConnectionLogsUseCase.Input(
                applicationId,
                executionContext.getOrganizationId(),
                executionContext.getEnvironmentId(),
                logMapper.convert(applicationId, searchLogsParam),
                pageable
            )
        );

        var metadata = getMetadataForApplicationConnectionLog(result.data(), result.total());

        // No pagination, because logsService did it already
        return createListResponse(executionContext, logMapper.convertConnectionLogs(result.data()), paginationParam, metadata, false);
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
        var executionContext = GraviteeContext.getExecutionContext();

        var connectionLog = getConnectionLogByApplicationIdAndLogIdAndTimestamp(executionContext, applicationId, logId, timestamp);

        var detail = searchApiConnectionLogDetailUseCase
            .execute(
                GraviteeContext.getExecutionContext(),
                new SearchApiConnectionLogDetailUseCase.Input(connectionLog.getApiId(), connectionLog.getRequestId())
            )
            .connectionLogDetail();

        return Response.ok(
            logMapper.convert(connectionLog, detail, getMetadataForApplicationConnectionLog(List.of(connectionLog)))
        ).build();
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
        return Response.ok(logsService.exportAsCsv(GraviteeContext.getExecutionContext(), searchLogResponse))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                format("attachment;filename=logs-%s-%s.csv", applicationId, System.currentTimeMillis())
            )
            .build();
    }

    @GET
    @Path("/{logId}/messages")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_LOG, acls = RolePermissionAction.READ) })
    public Response getApplicationLogMessagesByApplicationIdAndLogId(
        @PathParam("applicationId") String applicationId,
        @PathParam("logId") String logId,
        @QueryParam("timestamp") Long timestamp,
        @Valid @BeanParam PaginationParam paginationParam
    ) {
        var executionContext = GraviteeContext.getExecutionContext();

        var connectionLog = getConnectionLogByApplicationIdAndLogIdAndTimestamp(executionContext, applicationId, logId, timestamp);

        Optional<Pageable> pageable = Optional.ofNullable(
            paginationParam.hasPagination() ? new PageableImpl(paginationParam.getPage(), paginationParam.getSize()) : null
        );
        var result = searchMessageLogsUseCase.execute(
            GraviteeContext.getExecutionContext(),
            new SearchApiMessageLogsUseCase.Input(connectionLog.getApiId(), connectionLog.getRequestId(), pageable)
        );

        var metadata = getMetadataForApplicationConnectionLog(List.of(connectionLog), result.total());

        return createListResponse(
            GraviteeContext.getExecutionContext(),
            logMapper.convert(result.data()),
            paginationParam,
            metadata,
            false
        );
    }

    public Map<String, Map<String, Object>> getMetadataForApplicationConnectionLog(
        @NotNull List<ConnectionLog> applicationConnectionLogs,
        Long total
    ) {
        var metadata = getMetadataForApplicationConnectionLog(applicationConnectionLogs);

        metadata.put(METADATA_DATA_KEY, Map.of(METADATA_DATA_TOTAL_KEY, total));

        return metadata;
    }

    public Map<String, Map<String, Object>> getMetadataForApplicationConnectionLog(@NotNull List<ConnectionLog> applicationConnectionLogs) {
        Map<String, Map<String, Object>> metadata = new HashMap<>();

        applicationConnectionLogs.forEach(applicationConnectionLog -> {
            var apiId = applicationConnectionLog.getApiId();
            var planId = applicationConnectionLog.getPlanId();

            if (apiId != null) {
                metadata.computeIfAbsent(apiId, mapApiToMetadata(apiId, applicationConnectionLog.getApi()));
            }

            if (planId != null) {
                metadata.computeIfAbsent(planId, mapPlanToMetadata(planId, applicationConnectionLog.getPlan()));
            }
        });

        return metadata;
    }

    private ConnectionLog getConnectionLogByApplicationIdAndLogIdAndTimestamp(
        ExecutionContext executionContext,
        String applicationId,
        String logId,
        Long timestamp
    ) {
        var result = searchApplicationConnectionLogsUseCase.execute(
            new SearchApplicationConnectionLogsUseCase.Input(
                applicationId,
                executionContext.getOrganizationId(),
                executionContext.getEnvironmentId(),
                SearchLogsFilters.builder().from(timestamp).to(timestamp).requestIds(Set.of(logId)).build(),
                new PageableImpl(1, 1)
            )
        );
        if (result.data().isEmpty()) {
            throw new NotFoundException("Log [ " + logId + " ] not found.");
        }

        return result.data().getFirst();
    }

    private Function<String, Map<String, Object>> mapApiToMetadata(@NotNull String apiId, Api api) {
        return s -> {
            var metadata = new HashMap<String, Object>();

            if (isAnUnknownService(apiId)) {
                metadata.put(LogMetadata.METADATA_NAME.getValue(), LogMetadata.METADATA_UNKNOWN_API_NAME.getValue());
                metadata.put(LogMetadata.METADATA_UNKNOWN.getValue(), Boolean.TRUE.toString());
            } else if (api == null) {
                metadata.put(LogMetadata.METADATA_NAME.getValue(), LogMetadata.METADATA_DELETED_API_NAME.getValue());
                metadata.put(LogMetadata.METADATA_DELETED.getValue(), Boolean.TRUE.toString());
            } else if (isAnUnknownService(api.getId())) {
                metadata.put(LogMetadata.METADATA_NAME.getValue(), LogMetadata.METADATA_UNKNOWN_API_NAME.getValue());
                metadata.put(LogMetadata.METADATA_UNKNOWN.getValue(), Boolean.TRUE.toString());
            } else {
                metadata.put(LogMetadata.METADATA_NAME.getValue(), api.getName());
                metadata.put(LogMetadata.METADATA_VERSION.getValue(), api.getVersion());

                if (api.getDefinitionVersion() == DefinitionVersion.V4) {
                    metadata.put(LogMetadata.METADATA_API_TYPE.getValue(), api.getType().name());
                }

                if (Api.ApiLifecycleState.ARCHIVED.equals(api.getApiLifecycleState())) {
                    metadata.put(LogMetadata.METADATA_DELETED.getValue(), Boolean.TRUE.toString());
                }
            }

            return metadata;
        };
    }

    private Function<String, Map<String, Object>> mapPlanToMetadata(@NotNull String planId, Plan plan) {
        return s -> {
            var metadata = new HashMap<String, Object>();

            if (isAnUnknownService(planId)) {
                metadata.put(LogMetadata.METADATA_NAME.getValue(), LogMetadata.METADATA_UNKNOWN_PLAN_NAME.getValue());
                metadata.put(LogMetadata.METADATA_UNKNOWN.getValue(), Boolean.TRUE.toString());
            } else if (plan == null) {
                metadata.put(LogMetadata.METADATA_NAME.getValue(), LogMetadata.METADATA_DELETED_PLAN_NAME.getValue());
                metadata.put(LogMetadata.METADATA_DELETED.getValue(), Boolean.TRUE.toString());
            } else if (isAnUnknownService(plan.getId())) {
                metadata.put(LogMetadata.METADATA_NAME.getValue(), LogMetadata.METADATA_UNKNOWN_PLAN_NAME.getValue());
                metadata.put(LogMetadata.METADATA_UNKNOWN.getValue(), Boolean.TRUE.toString());
            } else {
                metadata.put(LogMetadata.METADATA_NAME.getValue(), plan.getName());
            }

            return metadata;
        };
    }

    private static boolean isAnUnknownService(String id) {
        return (
            Objects.equals(id, LogMetadata.UNKNOWN_SERVICE.getValue()) || Objects.equals(id, LogMetadata.UNKNOWN_SERVICE_MAPPED.getValue())
        );
    }
}
