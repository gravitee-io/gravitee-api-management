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
package io.gravitee.gamma.rest.resources.credential;

import io.gravitee.apim.core.credential.model.CredentialActor;
import io.gravitee.apim.core.credential.use_case.DeleteCredentialUseCase;
import io.gravitee.apim.core.credential.use_case.GetCredentialUseCase;
import io.gravitee.apim.core.credential.use_case.ListCredentialConsumersUseCase;
import io.gravitee.apim.core.credential.use_case.RotateCredentialUseCase;
import io.gravitee.apim.core.credential.use_case.UpdateCredentialUseCase;
import io.gravitee.gamma.rest.resources.credential.dto.CredentialConsumersResponseDto;
import io.gravitee.gamma.rest.resources.credential.dto.CredentialDto;
import io.gravitee.gamma.rest.resources.credential.dto.RotateCredentialRequestDto;
import io.gravitee.gamma.rest.resources.credential.dto.UpdateCredentialRequestDto;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PermissionService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * One credential. Reading needs only the credential permission; changing, rotating or deleting it also needs to be its
 * owner or an administrator, and the secret permission when Gravitee stores its values.
 */
@Produces(MediaType.APPLICATION_JSON)
public class CredentialResource {

    @Context
    private SecurityContext securityContext;

    @Inject
    private PermissionService permissionService;

    @Inject
    private MembershipService membershipService;

    @Inject
    private GetCredentialUseCase getCredentialUseCase;

    @Inject
    private UpdateCredentialUseCase updateCredentialUseCase;

    @Inject
    private RotateCredentialUseCase rotateCredentialUseCase;

    @Inject
    private DeleteCredentialUseCase deleteCredentialUseCase;

    @Inject
    private ListCredentialConsumersUseCase listCredentialConsumersUseCase;

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CREDENTIAL, acls = { RolePermissionAction.READ }) })
    public CredentialDto getCredential(@PathParam("credentialId") String credentialId) {
        var actor = actor();
        var output = getCredentialUseCase.execute(new GetCredentialUseCase.Input(actor, credentialId));
        return CredentialDto.from(output.credential(), actor, output.ownerName());
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CREDENTIAL, acls = { RolePermissionAction.UPDATE }) })
    public CredentialDto updateCredential(@PathParam("credentialId") String credentialId, @NotNull UpdateCredentialRequestDto request) {
        var actor = actor();
        var output = updateCredentialUseCase.execute(new UpdateCredentialUseCase.Input(actor, credentialId, request.toCommand()));
        return CredentialDto.from(output.credential(), actor);
    }

    @POST
    @Path("/_rotate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CREDENTIAL, acls = { RolePermissionAction.UPDATE }) })
    public CredentialDto rotateCredential(
        @PathParam("credentialId") String credentialId,
        @Valid @NotNull RotateCredentialRequestDto request
    ) {
        var actor = actor();
        var output = rotateCredentialUseCase.execute(new RotateCredentialUseCase.Input(actor, credentialId, request.values()));
        return CredentialDto.from(output.credential(), actor);
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CREDENTIAL, acls = { RolePermissionAction.DELETE }) })
    public Response deleteCredential(@PathParam("credentialId") String credentialId) {
        deleteCredentialUseCase.execute(new DeleteCredentialUseCase.Input(actor(), credentialId));
        return Response.noContent().build();
    }

    @GET
    @Path("/consumers")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CREDENTIAL, acls = { RolePermissionAction.READ }) })
    public CredentialConsumersResponseDto listCredentialConsumers(@PathParam("credentialId") String credentialId) {
        var output = listCredentialConsumersUseCase.execute(new ListCredentialConsumersUseCase.Input(actor(), credentialId));
        return new CredentialConsumersResponseDto(output.consumers());
    }

    private CredentialActor actor() {
        return CurrentCredentialActor.of(securityContext, permissionService, membershipService);
    }
}
