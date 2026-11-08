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
package io.gravitee.rest.api.management.v2.rest.resource.secret;

import io.gravitee.apim.core.secret.model.SecretValue;
import io.gravitee.apim.core.secret.use_case.DeleteSecretUseCase;
import io.gravitee.apim.core.secret.use_case.GetSecretUseCase;
import io.gravitee.apim.core.secret.use_case.UpdateSecretUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.SecretMapper;
import io.gravitee.rest.api.management.v2.rest.model.Secret;
import io.gravitee.rest.api.management.v2.rest.model.UpdateSecret;
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

public class SecretResource extends AbstractResource {

    @Inject
    private GetSecretUseCase getSecretUseCase;

    @Inject
    private UpdateSecretUseCase updateSecretUseCase;

    @Inject
    private DeleteSecretUseCase deleteSecretUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SECRET, acls = { RolePermissionAction.READ }) })
    public Secret getSecret(@PathParam("secretId") String secretId) {
        var executionContext = GraviteeContext.getExecutionContext();
        var output = getSecretUseCase.execute(
            new GetSecretUseCase.Input(secretId, executionContext.getOrganizationId(), executionContext.getEnvironmentId())
        );
        return SecretMapper.INSTANCE.map(output.secret());
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SECRET, acls = { RolePermissionAction.UPDATE }) })
    public Secret updateSecret(@PathParam("secretId") String secretId, @Valid @NotNull final UpdateSecret updateSecret) {
        var output = updateSecretUseCase.execute(
            new UpdateSecretUseCase.Input(secretId, new SecretValue(updateSecret.getValue()), getAuditInfo())
        );
        return SecretMapper.INSTANCE.map(output.secret());
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SECRET, acls = { RolePermissionAction.DELETE }) })
    public Response deleteSecret(@PathParam("secretId") String secretId) {
        deleteSecretUseCase.execute(new DeleteSecretUseCase.Input(secretId, getAuditInfo()));
        return Response.noContent().build();
    }
}
