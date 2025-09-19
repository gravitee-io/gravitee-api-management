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

import io.gravitee.apim.core.log.use_case.SearchApiConnectionLogDetailUseCase;
import io.gravitee.apim.core.log.use_case.SearchApiMessageLogsUseCase;
import io.gravitee.apim.core.log.use_case.SearchApiV4ConnectionLogsUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiLogsMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiMessageLogsMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiLogResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiLogsResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiMessageLogsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.api.log.param.SearchLogsParam;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

public class ApiLogsResource extends AbstractResource {

    @PathParam("apiId")
    private String apiId;

    @Inject
    private SearchApiV4ConnectionLogsUseCase searchConnectionLogsUsecase;

    @Inject
    private SearchApiMessageLogsUseCase searchApiMessageLogsUseCase;

    @Inject
    private SearchApiConnectionLogDetailUseCase searchConnectionLogUsecase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_LOG, acls = { RolePermissionAction.READ }) })
    public ApiLogsResponse getApiLogs(@BeanParam @Valid PaginationParam paginationParam, @BeanParam @Valid SearchLogsParam logsParam) {
        var request = new SearchApiV4ConnectionLogsUseCase.Input(
            apiId,
            ApiLogsMapper.INSTANCE.toSearchLogsFilters(logsParam),
            new PageableImpl(paginationParam.getPage(), paginationParam.getPerPage())
        );

        var response = searchConnectionLogsUsecase.execute(GraviteeContext.getExecutionContext(), request);

        return ApiLogsResponse.builder()
            .data(ApiLogsMapper.INSTANCE.mapToList(response.data()))
            .pagination(computePaginationInfo(response.total(), response.data().size(), paginationParam))
            .links(computePaginationLinks(response.total(), paginationParam))
            .build();
    }

    @Path("/{requestId}/messages")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_LOG, acls = { RolePermissionAction.READ }) })
    public ApiMessageLogsResponse getApiMessageLogs(
        @BeanParam @Valid PaginationParam paginationParam,
        @PathParam("requestId") String requestId
    ) {
        var request = new SearchApiMessageLogsUseCase.Input(
            apiId,
            requestId,
            new PageableImpl(paginationParam.getPage(), paginationParam.getPerPage())
        );

        var response = searchApiMessageLogsUseCase.execute(GraviteeContext.getExecutionContext(), request);

        return ApiMessageLogsResponse.builder()
            .data(ApiMessageLogsMapper.INSTANCE.mapToList(response.data()))
            .pagination(computePaginationInfo(response.total(), response.data().size(), paginationParam))
            .links(computePaginationLinks(response.total(), paginationParam))
            .build();
    }

    @Path("/{requestId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_LOG, acls = { RolePermissionAction.READ }) })
    public ApiLogResponse getApiLog(@PathParam("requestId") String requestId) {
        var request = new SearchApiConnectionLogDetailUseCase.Input(apiId, requestId);

        return searchConnectionLogUsecase
            .execute(GraviteeContext.getExecutionContext(), request)
            .connectionLogDetail()
            .map(ApiLogsMapper.INSTANCE::map)
            .orElseThrow(() -> new NotFoundException("No log found for api: " + apiId + " and requestId: " + requestId));
    }
}
