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
package io.gravitee.gamma.rest.resources.guardian;

import io.gravitee.common.http.MediaType;
import io.gravitee.gamma.rest.core.guardian.use_case.EnableApiGuardianUseCase;
import io.gravitee.gamma.rest.core.guardian.use_case.ListApiGuardiansUseCase;
import io.gravitee.gamma.rest.core.guardian.use_case.ReorderApiGuardiansUseCase;
import io.gravitee.gamma.rest.resources.CurrentAuditInfo;
import io.gravitee.gamma.rest.resources.guardian.dto.ApiGuardiansDto;
import io.gravitee.gamma.rest.resources.guardian.dto.EnableApiGuardianRequestDto;
import io.gravitee.gamma.rest.resources.guardian.dto.ReorderApiGuardiansRequestDto;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

/**
 * The Guardians enabled on an API, under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/apis/{apiId}/guardians}.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code GET} — the enabled Guardians, request ones before response ones, each phase in execution order.</li>
 *   <li>{@code POST} — enables a deployed Guardian after those of its phase and answers the enabled Guardians. The
 *       API runs it once redeployed. {@code 400} when the Guardian is not deployed or does not support the API's
 *       type, {@code 409} when it is already enabled.</li>
 *   <li>{@code PUT /_order} — sets the order in which the Guardians of one phase run, from the whole list of that
 *       phase, and answers the enabled Guardians. {@code 400} unless the list holds each Guardian of the phase
 *       exactly once.</li>
 * </ul>
 * Every endpoint answers {@code 404} when the API is unknown <em>or belongs to another environment</em>.
 *
 * <h2>Authorization</h2>
 * Reading takes {@code API_DEFINITION[READ]}. Changing takes {@code API_DEFINITION[UPDATE]} and
 * {@code ENVIRONMENT_GUARDIAN[READ]}: {@code @Permissions} grants on any one of the permissions it lists, so the
 * second is checked by the endpoint.
 *
 * @author GraviteeSource Team
 */
@Produces(MediaType.APPLICATION_JSON)
public class ApiGuardiansResource {

    @Inject
    private ListApiGuardiansUseCase listApiGuardiansUseCase;

    @Inject
    private EnableApiGuardianUseCase enableApiGuardianUseCase;

    @Inject
    private ReorderApiGuardiansUseCase reorderApiGuardiansUseCase;

    @Inject
    private PermissionService permissionService;

    @GET
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = { RolePermissionAction.READ }) })
    public ApiGuardiansDto list(@PathParam("apiId") String apiId) {
        var output = listApiGuardiansUseCase.execute(
            new ListApiGuardiansUseCase.Input(GraviteeContext.getExecutionContext().getEnvironmentId(), apiId)
        );
        return ApiGuardiansDto.from(output.guardians());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = { RolePermissionAction.UPDATE }) })
    public ApiGuardiansDto enable(@PathParam("apiId") String apiId, @Valid @NotNull EnableApiGuardianRequestDto request) {
        requireGuardianRead();
        var output = enableApiGuardianUseCase.execute(
            new EnableApiGuardianUseCase.Input(CurrentAuditInfo.of(), apiId, request.guardianId())
        );
        return ApiGuardiansDto.from(output.guardians());
    }

    @PUT
    @Path("/_order")
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = { RolePermissionAction.UPDATE }) })
    public ApiGuardiansDto reorder(@PathParam("apiId") String apiId, @Valid @NotNull ReorderApiGuardiansRequestDto request) {
        requireGuardianRead();
        var output = reorderApiGuardiansUseCase.execute(
            new ReorderApiGuardiansUseCase.Input(CurrentAuditInfo.of(), apiId, request.phase(), request.guardianIds())
        );
        return ApiGuardiansDto.from(output.guardians());
    }

    private void requireGuardianRead() {
        var executionContext = GraviteeContext.getExecutionContext();
        if (
            !permissionService.hasPermission(
                executionContext,
                RolePermission.ENVIRONMENT_GUARDIAN,
                executionContext.getEnvironmentId(),
                RolePermissionAction.READ
            )
        ) {
            throw new ForbiddenAccessException();
        }
    }
}
