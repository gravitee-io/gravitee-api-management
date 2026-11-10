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
package io.gravitee.gamma.module.platform.rest.resource.credential;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.gamma.module.platform.core.credential.use_case.CreateCredentialUseCase;
import io.gravitee.gamma.module.platform.core.credential.use_case.SearchCredentialsUseCase;
import io.gravitee.gamma.module.platform.rest.resource.dto.credential.CredentialDtos.CreateCredentialRequest;
import io.gravitee.gamma.module.platform.rest.resource.dto.credential.CredentialDtos.CredentialsResponse;
import io.gravitee.gamma.module.platform.rest.resource.mapper.credential.CredentialDtoMapper;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.UuidString;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

/**
 * The credentials of the environment, at {@code /organizations/{orgId}/environments/{envId}/modules/platform/credentials}.
 * The organization-level mount has no environment, so the environment permission denies it.
 */
@Produces(MediaType.APPLICATION_JSON)
public class CredentialsResource {

    private static final int MAX_PER_PAGE = 100;

    @Context
    private ResourceContext resourceContext;

    @Context
    private SecurityContext securityContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private PermissionService permissionService;

    @Inject
    private SearchCredentialsUseCase searchCredentialsUseCase;

    @Inject
    private CreateCredentialUseCase createCredentialUseCase;

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CREDENTIAL, acls = { RolePermissionAction.READ }) })
    public CredentialsResponse list(
        @QueryParam("q") String q,
        @QueryParam("page") @DefaultValue("1") int page,
        @QueryParam("perPage") @DefaultValue("10") int perPage
    ) {
        if (page < 1 || perPage < 1 || perPage > MAX_PER_PAGE) {
            throw new ValidationDomainException("page must be 1 or more, and perPage between 1 and " + MAX_PER_PAGE + ".");
        }
        var query = q == null || q.isBlank() ? null : q.trim();
        var output = searchCredentialsUseCase.execute(
            new SearchCredentialsUseCase.Input(CurrentCredentialActor.of(securityContext, permissionService), query, page - 1, perPage)
        );
        return CredentialDtoMapper.toResponse(output.page(), page, perPage);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CREDENTIAL, acls = { RolePermissionAction.CREATE }) })
    public Response create(@Valid @NotNull CreateCredentialRequest request) {
        var credential = createCredentialUseCase
            .execute(
                new CreateCredentialUseCase.Input(
                    UuidString.generateRandom(),
                    CurrentCredentialActor.of(securityContext, permissionService),
                    CredentialDtoMapper.toCommand(request)
                )
            )
            .credential();
        return Response.created(uriInfo.getAbsolutePathBuilder().path(credential.id()).build())
            .entity(CredentialDtoMapper.toResponse(credential))
            .build();
    }

    @Path("/{credentialId}")
    public CredentialResource credential() {
        return resourceContext.getResource(CredentialResource.class);
    }
}
