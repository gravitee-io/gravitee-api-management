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
package io.gravitee.rest.api.management.rest.resource.configuration.application.registration;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.configuration.application.registration.ClientRegistrationProviderEntity;
import io.gravitee.rest.api.model.configuration.application.registration.UpdateClientRegistrationProviderEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.configuration.application.ClientRegistrationService;
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
@Tag(name = "Client Registration Providers")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ClientRegistrationProviderResource extends AbstractResource {

    @Autowired
    private ClientRegistrationService clientRegistrationService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get a client registration provider",
        description = "User must have the PORTAL_CLIENT_REGISTRATION_PROVIDER[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "A client registration provider",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = ClientRegistrationProviderEntity.class)
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(@Permission(value = RolePermission.ENVIRONMENT_CLIENT_REGISTRATION_PROVIDER, acls = RolePermissionAction.READ))
    public ClientRegistrationProviderEntity getClientRegistrationProvider(
        @PathParam("clientRegistrationProvider") String clientRegistrationProvider
    ) {
        return clientRegistrationService.findById(clientRegistrationProvider);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update a client registration provider",
        description = "User must have the PORTAL_CLIENT_REGISTRATION_PROVIDER[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Updated client registration provider",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = ClientRegistrationProviderEntity.class)
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(@Permission(value = RolePermission.ENVIRONMENT_CLIENT_REGISTRATION_PROVIDER, acls = RolePermissionAction.UPDATE))
    public ClientRegistrationProviderEntity updateClientRegistrationProvider(
        @PathParam("clientRegistrationProvider") String clientRegistrationProvider,
        @Parameter(
            name = "clientRegistrationProvider",
            required = true
        ) @Valid @NotNull final UpdateClientRegistrationProviderEntity updatedClientRegistrationProvider
    ) {
        return clientRegistrationService.update(clientRegistrationProvider, updatedClientRegistrationProvider);
    }

    @DELETE
    @Operation(
        summary = "Delete a client registration provider",
        description = "User must have the PORTAL_CLIENT_REGISTRATION_PROVIDER[DELETE] permission to use this service"
    )
    @ApiResponse(responseCode = "204", description = "Client registration provider successfully deleted")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLIENT_REGISTRATION_PROVIDER, acls = RolePermissionAction.DELETE) })
    public Response deleteClientRegistrationProvider(@PathParam("clientRegistrationProvider") String clientRegistrationProvider) {
        clientRegistrationService.delete(clientRegistrationProvider);
        return Response.noContent().build();
    }
}
