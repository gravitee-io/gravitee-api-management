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
import io.gravitee.apim.core.guardian.use_case.DeleteGuardianUseCase;
import io.gravitee.apim.core.guardian.use_case.GetGuardianUseCase;
import io.gravitee.apim.core.guardian.use_case.UpdateGuardianUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.GuardianMapper;
import io.gravitee.rest.api.management.v2.rest.model.UpdateGuardian;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import lombok.CustomLog;

/**
 * @author GraviteeSource Team
 */
@CustomLog
public class GuardianResource extends AbstractResource {

    @PathParam("guardianId")
    String guardianId;

    @Inject
    private GetGuardianUseCase getGuardianUseCase;

    @Inject
    private UpdateGuardianUseCase updateGuardianUseCase;

    @Inject
    private DeleteGuardianUseCase deleteGuardianUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDIAN, acls = { RolePermissionAction.READ }) })
    public Response getGuardian() {
        var executionContext = GraviteeContext.getExecutionContext();
        var output = getGuardianUseCase.execute(new GetGuardianUseCase.Input(executionContext.getEnvironmentId(), guardianId));

        return Response.ok(GuardianMapper.INSTANCE.map(output.guardian())).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDIAN, acls = { RolePermissionAction.UPDATE }) })
    public Response updateGuardian(@Valid @NotNull final UpdateGuardian updateGuardian) {
        var output = updateGuardianUseCase.execute(
            new UpdateGuardianUseCase.Input(guardianId, GuardianMapper.INSTANCE.map(updateGuardian), auditInfo())
        );

        return Response.ok(GuardianMapper.INSTANCE.map(output.guardian())).build();
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDIAN, acls = { RolePermissionAction.DELETE }) })
    public Response deleteGuardian() {
        deleteGuardianUseCase.execute(new DeleteGuardianUseCase.Input(guardianId, auditInfo()));

        return Response.noContent().build();
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
