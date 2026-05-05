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
package io.gravitee.rest.api.management.v2.rest.resource.api.log;

import static io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo.computePaginationInfo;

import io.gravitee.apim.core.log.use_case.NativeApiLogReadUseCase;
import io.gravitee.apim.core.log.use_case.NativeApiLogSearchUseCase;
import io.gravitee.apim.core.log.use_case.NativeApiLogSummaryUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.NativeApiLogsMapper;
import io.gravitee.rest.api.management.v2.rest.model.NativeApiLog;
import io.gravitee.rest.api.management.v2.rest.model.NativeApiLogsResponse;
import io.gravitee.rest.api.management.v2.rest.model.NativeApiLogsSummary;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.api.log.param.SearchNativeLogsParam;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;

public class NativeApiLogsResource extends AbstractResource {

    @PathParam("apiId")
    private String apiId;

    @Inject
    private NativeApiLogSummaryUseCase summaryUseCase;

    @Inject
    private NativeApiLogReadUseCase readUseCase;

    @Inject
    private NativeApiLogSearchUseCase searchUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_NATIVE_LOG, acls = RolePermissionAction.READ) })
    public NativeApiLogsResponse getFilteredLogs(
        @BeanParam @Valid SearchNativeLogsParam searchParam,
        @BeanParam @Valid PaginationParam paginationParam
    ) {
        var output = searchUseCase.execute(
            new NativeApiLogSearchUseCase.Input(
                GraviteeContext.getExecutionContext(),
                apiId,
                searchParam.getFrom(),
                searchParam.getTo(),
                searchParam.getApplicationIds(),
                searchParam.getPlanIds(),
                searchParam.getConnectionStatuses(),
                paginationParam.getPage(),
                paginationParam.getPerPage()
            )
        );
        var data = NativeApiLogsMapper.INSTANCE.mapList(output.response().logs());
        return new NativeApiLogsResponse()
            .data(data)
            .pagination(computePaginationInfo(output.response().total(), data.size(), paginationParam))
            .links(computePaginationLinks(output.response().total(), paginationParam));
    }

    @GET
    @Path("/summary")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_NATIVE_LOG, acls = RolePermissionAction.READ) })
    public NativeApiLogsSummary summarize(@QueryParam("from") @NotNull @Min(0) Long from, @QueryParam("to") @NotNull @Min(0) Long to) {
        var output = summaryUseCase.execute(
            new NativeApiLogSummaryUseCase.Input(
                GraviteeContext.getExecutionContext(),
                apiId,
                Instant.ofEpochMilli(from),
                Instant.ofEpochMilli(to)
            )
        );
        return NativeApiLogsMapper.INSTANCE.mapSummary(output);
    }

    @GET
    @Path("/{requestId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_NATIVE_ANALYTICS, acls = RolePermissionAction.READ) })
    public NativeApiLog getLog(
        @QueryParam("from") @NotNull @Min(0) Long from,
        @QueryParam("to") @NotNull @Min(0) Long to,
        @PathParam("requestId") String requestId
    ) {
        return readUseCase
            .execute(new NativeApiLogReadUseCase.Input(GraviteeContext.getExecutionContext(), apiId, requestId, from, to))
            .nativeApiLog()
            .map(NativeApiLogsMapper.INSTANCE::map)
            .orElseThrow(() -> new NotFoundException("No native log found for api: " + apiId + " and requestId: " + requestId));
    }
}
