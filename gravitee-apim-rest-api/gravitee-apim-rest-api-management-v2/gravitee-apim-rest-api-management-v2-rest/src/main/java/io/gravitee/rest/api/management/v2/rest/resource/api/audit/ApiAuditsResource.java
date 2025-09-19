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
package io.gravitee.rest.api.management.v2.rest.resource.api.audit;

import static io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo.computePaginationInfo;

import io.gravitee.apim.core.api.model.ApiAuditQueryFilters;
import io.gravitee.apim.core.audit.query_service.AuditEventQueryService;
import io.gravitee.apim.core.audit.use_case.SearchApiAuditUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiAuditMapper;
import io.gravitee.rest.api.management.v2.rest.model.AuditEventsResponse;
import io.gravitee.rest.api.management.v2.rest.model.AuditsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.api.audit.param.SearchApiAuditsParam;
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
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class ApiAuditsResource extends AbstractResource {

    @PathParam("apiId")
    private String apiId;

    @Inject
    private SearchApiAuditUseCase searchApiAuditUseCase;

    @Inject
    private AuditEventQueryService auditEventQueryService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_AUDIT, acls = { RolePermissionAction.READ }) })
    public AuditsResponse getApiLogs(@BeanParam @Valid PaginationParam paginationParam, @BeanParam @Valid SearchApiAuditsParam params) {
        var input = buildInput(paginationParam, params);

        var output = searchApiAuditUseCase.execute(input);

        return AuditsResponse.builder()
            .data(ApiAuditMapper.INSTANCE.map(output.data(), output.metadata()))
            .pagination(computePaginationInfo(output.total(), output.data().size(), paginationParam))
            .links(computePaginationLinks(output.total(), paginationParam))
            .build();
    }

    @GET
    @Path("/events")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_AUDIT, acls = { RolePermissionAction.READ }) })
    public AuditEventsResponse listAllApiAuditEvent() {
        return AuditEventsResponse.builder().data(auditEventQueryService.listAllApiAuditEvents()).build();
    }

    @NotNull
    private SearchApiAuditUseCase.Input buildInput(PaginationParam paginationParam, SearchApiAuditsParam params) {
        var executionContext = GraviteeContext.getExecutionContext();
        var query = new ApiAuditQueryFilters(
            apiId,
            executionContext.getOrganizationId(),
            executionContext.getEnvironmentId(),
            Optional.ofNullable(params.getFrom()),
            Optional.ofNullable(params.getTo()),
            params.getEvents()
        );
        return new SearchApiAuditUseCase.Input(query, new PageableImpl(paginationParam.getPage(), paginationParam.getPerPage()));
    }
}
