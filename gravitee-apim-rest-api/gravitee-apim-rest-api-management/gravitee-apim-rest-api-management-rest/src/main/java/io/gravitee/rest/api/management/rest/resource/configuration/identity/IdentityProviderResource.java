/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.rest.resource.configuration.identity;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.UpdateIdentityProviderEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Configuration")
@Tag(name = "Identity Providers")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IdentityProviderResource extends AbstractResource {

    @Autowired
    private IdentityProviderService identityProviderService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get an identity provider",
        description = "User must have the ORGANIZATION_IDENTITY_PROVIDER[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "An identity provider",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IdentityProviderEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_IDENTITY_PROVIDER, acls = RolePermissionAction.READ))
    public IdentityProviderEntity getIdentityProvider(@PathParam("identityProvider") String identityProvider) {
        return identityProviderService.findById(identityProvider);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update an identity provider",
        description = "User must have the ORGANIZATION_IDENTITY_PROVIDER[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Updated identity provider",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IdentityProviderEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_IDENTITY_PROVIDER, acls = RolePermissionAction.UPDATE))
    public IdentityProviderEntity updateIdentityProvider(
        @PathParam("identityProvider") String identityProvider,
        @Parameter(
            name = "identityProviderEntity",
            required = true
        ) @Valid @NotNull final UpdateIdentityProviderEntity updatedIdentityProvider
    ) {
        return identityProviderService.update(GraviteeContext.getExecutionContext(), identityProvider, updatedIdentityProvider);
    }

    @DELETE
    @Operation(
        summary = "Delete an identity provider",
        description = "User must have the ORGANIZATION_IDENTITY_PROVIDER[DELETE] permission to use this service"
    )
    @ApiResponse(responseCode = "204", description = "Identity provider successfully deleted")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_IDENTITY_PROVIDER, acls = RolePermissionAction.DELETE) })
    public Response deleteIdentityProvider(@PathParam("identityProvider") String identityProvider) {
        identityProviderService.delete(GraviteeContext.getExecutionContext(), identityProvider);
        return Response.noContent().build();
    }
}
