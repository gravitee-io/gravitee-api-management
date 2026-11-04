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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.guardian.use_case.CreateGuardianUseCase;
import io.gravitee.apim.core.guardian.use_case.SearchGuardianUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.GuardianMapper;
import io.gravitee.rest.api.management.v2.rest.model.CreateGuardian;
import io.gravitee.rest.api.management.v2.rest.model.GuardiansResponse;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import lombok.CustomLog;

/**
 * @author GraviteeSource Team
 */
@CustomLog
public class GuardiansResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private CreateGuardianUseCase createGuardianUseCase;

    @Inject
    private SearchGuardianUseCase searchGuardianUseCase;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDIAN, acls = { RolePermissionAction.CREATE }) })
    public Response createGuardian(@Valid @NotNull final CreateGuardian createGuardian) {
        var output = createGuardianUseCase.execute(
            new CreateGuardianUseCase.Input(GuardianMapper.INSTANCE.map(createGuardian), auditInfo())
        );

        return Response.created(this.getLocationHeader(output.guardian().getId()))
            .entity(GuardianMapper.INSTANCE.map(output.guardian()))
            .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDIAN, acls = { RolePermissionAction.READ }) })
    public GuardiansResponse searchGuardians(
        @QueryParam("q") String q,
        @BeanParam @Valid PaginationParam paginationParam,
        @QueryParam("sortBy") String sortBy
    ) {
        var executionContext = GraviteeContext.getExecutionContext();
        var result = searchGuardianUseCase
            .execute(new SearchGuardianUseCase.Input(executionContext.getEnvironmentId(), q, paginationParam.toPageable(), sortBy))
            .guardians();

        return new GuardiansResponse()
            .data(GuardianMapper.INSTANCE.map(result.getContent()))
            .pagination(PaginationInfo.computePaginationInfo(result.getTotalElements(), result.getContent().size(), paginationParam))
            .links(computePaginationLinks(result.getTotalElements(), paginationParam));
    }

    @Path("{guardianId}")
    public GuardianResource getGuardianResource() {
        return resourceContext.getResource(GuardianResource.class);
    }

    private AuditInfo auditInfo() {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();
        return AuditInfo.builder()
            .organizationId(executionContext.getOrganizationId())
            .environmentId(executionContext.getEnvironmentId())
            .actor(
                AuditActor.builder()
                    .userId(userDetails.getUsername())
                    .userSource(userDetails.getSource())
                    .userSourceId(userDetails.getSourceId())
                    .build()
            )
            .build();
    }
}
