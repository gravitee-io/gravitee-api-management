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
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.NewIdentityProviderEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.management.rest.model.configuration.identity.IdentityProviderListItem;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import io.swagger.annotations.*;

import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Configuration", "Identity Providers"})
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IdentityProvidersResource extends AbstractResource {

    @Autowired
    private IdentityProviderService identityProviderService;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Permissions(@Permission(value = RolePermission.ENVIRONMENT_IDENTITY_PROVIDER, acls = RolePermissionAction.READ))
    @ApiOperation(value = "Get the list of identity providers",
            notes = "User must have the PORTAL_IDENTITY_PROVIDER[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List identity providers", response = IdentityProviderListItem.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public List<IdentityProviderListItem> listIdentityProviders() {
        return identityProviderService.findAll().stream().map(identityProvider -> {
            IdentityProviderListItem item = new IdentityProviderListItem();
            item.setId(identityProvider.getId());
            item.setName(identityProvider.getName());
            item.setDescription(identityProvider.getDescription());
            item.setEnabled(identityProvider.isEnabled());
            item.setType(identityProvider.getType());
            item.setCreatedAt(identityProvider.getCreatedAt());
            item.setUpdatedAt(identityProvider.getUpdatedAt());
            return item;
        }).collect(Collectors.toList());
    }

    @POST
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_IDENTITY_PROVIDER, acls = RolePermissionAction.CREATE)
    })
    @ApiOperation(value = "Create an identity provider",
            notes = "User must have the PORTAL_IDENTITY_PROVIDER[CREATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Identity provider successfully created", response = IdentityProviderEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response createIdentityProvider(
            @ApiParam(name = "identity-provider", required = true) @Valid @NotNull NewIdentityProviderEntity newIdentityProviderEntity) {
        IdentityProviderEntity newIdentityProvider = identityProviderService.create(newIdentityProviderEntity);

        if (newIdentityProvider != null) {
            return Response
                    .created(URI.create("/configuration/identities/" + newIdentityProvider.getId()))
                    .entity(newIdentityProvider)
                    .build();
        }

        return Response.serverError().build();
    }

    @Path("{identityProvider}")
    public IdentityProviderResource getIdentityProviderResource() {
        return resourceContext.getResource(IdentityProviderResource.class);
    }
}
