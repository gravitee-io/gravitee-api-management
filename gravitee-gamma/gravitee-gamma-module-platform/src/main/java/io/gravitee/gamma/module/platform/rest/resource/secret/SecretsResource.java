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
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
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
        @QueryParam("page") @DefaultValue("1") int page,
        @QueryParam("perPage") @DefaultValue("10") int perPage
    ) {
        var context = environmentContext();
        var secrets = listSecretsUseCase
            .execute(new ListSecretsUseCase.Input(context.getOrganizationId(), context.getEnvironmentId(), page, perPage))
            .secrets();
        long pageCount = (secrets.getTotalElements() + perPage - 1) / perPage;
        return new SecretsPageResponse(
            secrets.getContent().stream().map(SecretResponse::from).toList(),
            new Pagination(page, perPage, secrets.getPageElements(), pageCount, secrets.getTotalElements())
        );
    }

    @POST
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_SECRET, acls = RolePermissionAction.CREATE) })
    public Response create(@NotNull CreateSecretRequest request) {
        var context = environmentContext();
        var secret = createSecretUseCase
            .execute(
                CreateSecretUseCase.Input.builder()
                    .id(UuidString.generateRandom())
                    .organizationId(context.getOrganizationId())
                    .environmentId(context.getEnvironmentId())
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
    public SecretResponse get(@PathParam("secretId") String secretId) {
        var context = environmentContext();
        return SecretResponse.from(
            getSecretUseCase.execute(new GetSecretUseCase.Input(context.getOrganizationId(), context.getEnvironmentId(), secretId)).secret()
        );
    }

    @PUT
    @Path("/{secretId}")
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_SECRET, acls = RolePermissionAction.UPDATE) })
    public SecretResponse rotate(@PathParam("secretId") String secretId, @NotNull RotateSecretRequest request) {
        var context = environmentContext();
        var input = new UpdateSecretUseCase.Input(
            context.getOrganizationId(),
            context.getEnvironmentId(),
            secretId,
            new SecretValue(request.value()),
            currentActor()
        );
        return SecretResponse.from(updateSecretUseCase.execute(input).secret());
    }

    @DELETE
    @Path("/{secretId}")
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_SECRET, acls = RolePermissionAction.DELETE) })
    public Response delete(@PathParam("secretId") String secretId) {
        var context = environmentContext();
        deleteSecretUseCase.execute(
            new DeleteSecretUseCase.Input(context.getOrganizationId(), context.getEnvironmentId(), secretId, currentActor())
        );
        return Response.noContent().build();
    }

    // The context holds the canonical ids; the path may name the environment by its hrid.
    private static ExecutionContext environmentContext() {
        var context = GraviteeContext.getExecutionContext();
        if (!context.hasEnvironmentId()) {
            throw new NotFoundException();
        }
        return context;
    }

    private static SecretActor currentActor() {
        var user = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return new SecretActor(user.getUsername(), user.getSource(), user.getSourceId());
    }
}
