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
package io.gravitee.rest.api.management.v2.rest.resource.credential;

import io.gravitee.apim.core.credential.model.CredentialActor;
import io.gravitee.apim.core.credential.use_case.DeleteCredentialUseCase;
import io.gravitee.apim.core.credential.use_case.GetCredentialUseCase;
import io.gravitee.apim.core.credential.use_case.ListCredentialConsumersUseCase;
import io.gravitee.apim.core.credential.use_case.RotateCredentialUseCase;
import io.gravitee.apim.core.credential.use_case.UpdateCredentialUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.CredentialMapper;
import io.gravitee.rest.api.management.v2.rest.model.Credential;
import io.gravitee.rest.api.management.v2.rest.model.CredentialConsumersResponse;
import io.gravitee.rest.api.management.v2.rest.model.RotateCredential;
import io.gravitee.rest.api.management.v2.rest.model.UpdateCredential;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
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
import jakarta.ws.rs.core.Response;

/**
 * One credential. Reading needs only the credential permission; changing, rotating or deleting it also needs to be its
 * owner or an administrator, and the secret permission when Gravitee stores its values.
 */
public class CredentialResource extends AbstractResource {

    @PathParam("credentialId")
    private String credentialId;

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
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CREDENTIAL, acls = { RolePermissionAction.READ }) })
    public Credential getCredential() {
        var output = getCredentialUseCase.execute(new GetCredentialUseCase.Input(actor(), credentialId));
        return CredentialMapper.INSTANCE.map(output.credential());
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CREDENTIAL, acls = { RolePermissionAction.UPDATE }) })
    public Credential updateCredential(@Valid @NotNull final UpdateCredential updateCredential) {
        var output = updateCredentialUseCase.execute(
            new UpdateCredentialUseCase.Input(actor(), credentialId, CredentialMapper.INSTANCE.map(updateCredential))
        );
        return CredentialMapper.INSTANCE.map(output.credential());
    }

    @POST
    @Path("/_rotate")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CREDENTIAL, acls = { RolePermissionAction.UPDATE }) })
    public Credential rotateCredential(@Valid @NotNull final RotateCredential rotateCredential) {
        var output = rotateCredentialUseCase.execute(
            new RotateCredentialUseCase.Input(actor(), credentialId, rotateCredential.getValues())
        );
        return CredentialMapper.INSTANCE.map(output.credential());
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CREDENTIAL, acls = { RolePermissionAction.DELETE }) })
    public Response deleteCredential() {
        deleteCredentialUseCase.execute(new DeleteCredentialUseCase.Input(actor(), credentialId));
        return Response.noContent().build();
    }

    @GET
    @Path("/consumers")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CREDENTIAL, acls = { RolePermissionAction.READ }) })
    public CredentialConsumersResponse listCredentialConsumers() {
        var output = listCredentialConsumersUseCase.execute(new ListCredentialConsumersUseCase.Input(actor(), credentialId));
        return new CredentialConsumersResponse().data(CredentialMapper.INSTANCE.mapConsumers(output.consumers()));
    }

    private CredentialActor actor() {
        return CurrentCredentialActor.of(securityContext, permissionService);
    }
}
