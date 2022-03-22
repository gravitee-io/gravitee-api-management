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
import io.gravitee.rest.api.management.rest.model.configuration.identity.IdentityProviderListItem;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.NewIdentityProviderEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
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
public class IdentityProvidersResource extends AbstractResource {

    @Autowired
    private IdentityProviderService identityProviderService;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_IDENTITY_PROVIDER, acls = RolePermissionAction.READ))
    @Operation(
        summary = "Get the list of identity providers",
        description = "User must have the ORGANIZATION_IDENTITY_PROVIDER[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "List identity providers",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = IdentityProviderListItem.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public List<IdentityProviderListItem> getIdentityProviders() {
        return identityProviderService
            .findAll()
            .stream()
            .map(
                identityProvider -> {
                    IdentityProviderListItem item = new IdentityProviderListItem();
                    item.setId(identityProvider.getId());
                    item.setName(identityProvider.getName());
                    item.setDescription(identityProvider.getDescription());
                    item.setEnabled(identityProvider.isEnabled());
                    item.setType(identityProvider.getType());
                    item.setCreatedAt(identityProvider.getCreatedAt());
                    item.setUpdatedAt(identityProvider.getUpdatedAt());
                    item.setSync(identityProvider.isSyncMappings());
                    return item;
                }
            )
            .collect(Collectors.toList());
    }

    @POST
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_IDENTITY_PROVIDER, acls = RolePermissionAction.CREATE) })
    @Operation(
        summary = "Create an identity provider",
        description = "User must have the ORGANIZATION_IDENTITY_PROVIDER[CREATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Identity provider successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IdentityProviderEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response createIdentityProvider(
        @Parameter(name = "identity-provider", required = true) @Valid @NotNull NewIdentityProviderEntity newIdentityProviderEntity
    ) {
        IdentityProviderEntity newIdentityProvider = identityProviderService.create(newIdentityProviderEntity);

        if (newIdentityProvider != null) {
            return Response.created(this.getLocationHeader(newIdentityProvider.getId())).entity(newIdentityProvider).build();
        }

        return Response.serverError().build();
    }

    @Path("{identityProvider}")
    public IdentityProviderResource getIdentityProviderResource() {
        return resourceContext.getResource(IdentityProviderResource.class);
    }
}
