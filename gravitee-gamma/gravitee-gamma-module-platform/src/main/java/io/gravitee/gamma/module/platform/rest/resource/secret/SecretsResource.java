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
package io.gravitee.gamma.module.platform.rest.resource.secret;

import io.gravitee.gamma.module.platform.core.secret.model.SecretActor;
import io.gravitee.gamma.module.platform.core.secret.model.SecretValue;
import io.gravitee.gamma.module.platform.core.secret.use_case.CreateSecretUseCase;
import io.gravitee.gamma.module.platform.core.secret.use_case.DeleteSecretUseCase;
import io.gravitee.gamma.module.platform.core.secret.use_case.GetSecretUseCase;
import io.gravitee.gamma.module.platform.core.secret.use_case.ListSecretsUseCase;
import io.gravitee.gamma.module.platform.core.secret.use_case.UpdateSecretUseCase;
import io.gravitee.gamma.module.platform.rest.resource.dto.secret.SecretDtos.CreateSecretRequest;
import io.gravitee.gamma.module.platform.rest.resource.dto.secret.SecretDtos.Pagination;
import io.gravitee.gamma.module.platform.rest.resource.dto.secret.SecretDtos.RotateSecretRequest;
import io.gravitee.gamma.module.platform.rest.resource.dto.secret.SecretDtos.SecretResponse;
import io.gravitee.gamma.module.platform.rest.resource.dto.secret.SecretDtos.SecretsPageResponse;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.UuidString;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.security.core.context.SecurityContextHolder;

/** Serves only under {@code /environments/{envId}/modules/platform}: a secret belongs to an environment. */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SecretsResource {

    @Inject
    private CreateSecretUseCase createSecretUseCase;

    @Inject
    private UpdateSecretUseCase updateSecretUseCase;

    @Inject
    private DeleteSecretUseCase deleteSecretUseCase;

    @Inject
    private GetSecretUseCase getSecretUseCase;

    @Inject
    private ListSecretsUseCase listSecretsUseCase;

    @GET
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_SECRET, acls = RolePermissionAction.READ) })
    public SecretsPageResponse list(
        @PathParam("orgId") String orgId,
        @PathParam("envId") String envId,
        @QueryParam("page") @DefaultValue("1") int page,
        @QueryParam("perPage") @DefaultValue("10") int perPage
    ) {
        var secrets = listSecretsUseCase.execute(new ListSecretsUseCase.Input(orgId, requireEnvironment(envId), page, perPage)).secrets();
        long pageCount = (secrets.getTotalElements() + perPage - 1) / perPage;
        return new SecretsPageResponse(
            secrets.getContent().stream().map(SecretResponse::from).toList(),
            new Pagination(page, perPage, secrets.getPageElements(), pageCount, secrets.getTotalElements())
        );
    }

    @POST
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_SECRET, acls = RolePermissionAction.CREATE) })
    public Response create(@PathParam("orgId") String orgId, @PathParam("envId") String envId, @NotNull CreateSecretRequest request) {
        var secret = createSecretUseCase
            .execute(
                CreateSecretUseCase.Input.builder()
                    .id(UuidString.generateRandom())
                    .organizationId(orgId)
                    .environmentId(requireEnvironment(envId))
                    .key(request.key())
                    .value(new SecretValue(request.value()))
                    .actor(currentActor())
                    .build()
            )
            .secret();
        return Response.status(Response.Status.CREATED).entity(SecretResponse.from(secret)).build();
    }

    @GET
    @Path("/{secretId}")
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_SECRET, acls = RolePermissionAction.READ) })
    public SecretResponse get(@PathParam("orgId") String orgId, @PathParam("envId") String envId, @PathParam("secretId") String secretId) {
        return SecretResponse.from(
            getSecretUseCase.execute(new GetSecretUseCase.Input(orgId, requireEnvironment(envId), secretId)).secret()
        );
    }

    @PUT
    @Path("/{secretId}")
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_SECRET, acls = RolePermissionAction.UPDATE) })
    public SecretResponse rotate(
        @PathParam("orgId") String orgId,
        @PathParam("envId") String envId,
        @PathParam("secretId") String secretId,
        @NotNull RotateSecretRequest request
    ) {
        var input = new UpdateSecretUseCase.Input(
            orgId,
            requireEnvironment(envId),
            secretId,
            new SecretValue(request.value()),
            currentActor()
        );
        return SecretResponse.from(updateSecretUseCase.execute(input).secret());
    }

    @DELETE
    @Path("/{secretId}")
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_SECRET, acls = RolePermissionAction.DELETE) })
    public Response delete(@PathParam("orgId") String orgId, @PathParam("envId") String envId, @PathParam("secretId") String secretId) {
        deleteSecretUseCase.execute(new DeleteSecretUseCase.Input(orgId, requireEnvironment(envId), secretId, currentActor()));
        return Response.noContent().build();
    }

    private static String requireEnvironment(String envId) {
        if (envId == null) {
            throw new NotFoundException();
        }
        return envId;
    }

    private static SecretActor currentActor() {
        var user = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return new SecretActor(user.getUsername(), user.getSource(), user.getSourceId());
    }
}
