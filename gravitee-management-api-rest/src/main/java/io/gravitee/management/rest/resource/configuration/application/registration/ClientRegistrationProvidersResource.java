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
package io.gravitee.management.rest.resource.configuration.application.registration;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.configuration.application.registration.ClientRegistrationProviderEntity;
import io.gravitee.management.model.configuration.application.registration.InitialAccessTokenType;
import io.gravitee.management.model.configuration.application.registration.NewClientRegistrationProviderEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.model.configuration.application.ClientRegistrationProviderListItem;
import io.gravitee.management.rest.resource.AbstractResource;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.configuration.application.ClientRegistrationService;
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
@Api(tags = {"Client Registration Providers"})
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ClientRegistrationProvidersResource extends AbstractResource {

    @Autowired
    private ClientRegistrationService clientRegistrationService;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Permissions(@Permission(value = RolePermission.PORTAL_CLIENT_REGISTRATION_PROVIDER, acls = RolePermissionAction.READ))
    @ApiOperation(value = "Get the list of client registration providers",
            notes = "User must have the PORTAL_CLIENT_REGISTRATION_PROVIDER[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List client registration providers",
                    response = ClientRegistrationProviderListItem.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public List<ClientRegistrationProviderListItem> listClientRegistrationProviders() {
        return clientRegistrationService.findAll().stream().map(clientRegistrationProvider -> {
            ClientRegistrationProviderListItem item = new ClientRegistrationProviderListItem();
            item.setId(clientRegistrationProvider.getId());
            item.setName(clientRegistrationProvider.getName());
            item.setDescription(clientRegistrationProvider.getDescription());
            item.setCreatedAt(clientRegistrationProvider.getCreatedAt());
            item.setUpdatedAt(clientRegistrationProvider.getUpdatedAt());
            return item;
        }).collect(Collectors.toList());
    }

    @POST
    @Permissions({
            @Permission(value = RolePermission.PORTAL_CLIENT_REGISTRATION_PROVIDER, acls = RolePermissionAction.CREATE)
    })
    @ApiOperation(value = "Create a client registration provider",
            notes = "User must have the PORTAL_CLIENT_REGISTRATION_PROVIDER[CREATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Client registration provider provider successfully created",
                    response = ClientRegistrationProviderEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response createClientRegistrationProvider(
            @ApiParam(name = "identity-provider", required = true) @Valid @NotNull NewClientRegistrationProviderEntity newClientRegistrationProviderEntity) {
        if (newClientRegistrationProviderEntity.getInitialAccessTokenType() == InitialAccessTokenType.CLIENT_CREDENTIALS) {
            if (newClientRegistrationProviderEntity.getClientId() == null || newClientRegistrationProviderEntity.getClientSecret() == null) {
                throw new IllegalArgumentException("Client credentials are missing");
            }
        } else {
            if (newClientRegistrationProviderEntity.getInitialAccessToken() == null) {
                throw new IllegalArgumentException("Access token is missing");
            }
        }

        ClientRegistrationProviderEntity newClientRegistrationProvider = clientRegistrationService.create(newClientRegistrationProviderEntity);

        if (newClientRegistrationProvider != null) {
            return Response
                    .created(URI.create("/configuration/application/registration/providers/" + newClientRegistrationProvider.getId()))
                    .entity(newClientRegistrationProvider)
                    .build();
        }

        return Response.serverError().build();
    }

    @Path("{clientRegistrationProvider}")
    public ClientRegistrationProviderResource getClientRegistrationProviderResource() {
        return resourceContext.getResource(ClientRegistrationProviderResource.class);
    }
}
