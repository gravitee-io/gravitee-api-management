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
package io.gravitee.rest.api.management.v2.rest.resource.api.metrics;

import static io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo.computePaginationInfo;

import io.gravitee.apim.core.metrics.use_case.SearchApiMessageMetricsUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiMessageMetricsMapper;
import io.gravitee.rest.api.management.v2.rest.model.MessageMetricsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.api.metrics.param.SearchMessageMetricsParam;
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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiMessageMetricsResource extends AbstractResource {

    @PathParam("apiId")
    private String apiId;

    @Inject
    private SearchApiMessageMetricsUseCase searchApiMessageLogsUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_LOG, acls = { RolePermissionAction.READ }) })
    public MessageMetricsResponse getApiMessageLogs(
        @BeanParam @Valid PaginationParam paginationParam,
        @BeanParam @Valid SearchMessageMetricsParam searchMessageMetricsParam
    ) {
        var input = new SearchApiMessageMetricsUseCase.Input(
            apiId,
            ApiMessageMetricsMapper.INSTANCE.map(searchMessageMetricsParam),
            new PageableImpl(paginationParam.getPage(), paginationParam.getPerPage())
        );

        var output = searchApiMessageLogsUseCase.execute(GraviteeContext.getExecutionContext(), input);

        return new MessageMetricsResponse()
            .data(ApiMessageMetricsMapper.INSTANCE.map(output.data()))
            .pagination(computePaginationInfo(output.total(), output.data().size(), paginationParam))
            .links(computePaginationLinks(output.total(), paginationParam));
    }
}
