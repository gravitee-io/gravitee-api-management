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
package io.gravitee.rest.api.management.rest.resource.configuration.application.registration;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.configuration.application.registration.AuthenticationStrategyEntity;
import io.gravitee.rest.api.model.configuration.application.registration.UpdateAuthenticationStrategyEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.application.AuthenticationStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import org.springframework.beans.factory.annotation.Autowired;

@Tag(name = "Authentication Strategies")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthenticationStrategyResource extends AbstractResource {

    @Autowired
    private AuthenticationStrategyService authenticationStrategyService;

    @PathParam("authenticationStrategy")
    @Parameter(name = "authenticationStrategy", required = true)
    private String authenticationStrategyId;

    @GET
    @Permissions(@Permission(value = RolePermission.ENVIRONMENT_CLIENT_REGISTRATION_PROVIDER, acls = RolePermissionAction.READ))
    @Operation(
        summary = "Get an authentication strategy",
        description = "User must have the ENVIRONMENT_CLIENT_REGISTRATION_PROVIDER[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Authentication strategy",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = AuthenticationStrategyEntity.class)
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public AuthenticationStrategyEntity getAuthenticationStrategy() {
        return authenticationStrategyService.findById(
            GraviteeContext.getExecutionContext().getEnvironmentId(),
            authenticationStrategyId
        );
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLIENT_REGISTRATION_PROVIDER, acls = RolePermissionAction.UPDATE) })
    @Operation(
        summary = "Update an authentication strategy",
        description = "User must have the ENVIRONMENT_CLIENT_REGISTRATION_PROVIDER[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Updated authentication strategy",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = AuthenticationStrategyEntity.class)
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public AuthenticationStrategyEntity updateAuthenticationStrategy(
        @Parameter(name = "strategy", required = true) @Valid @NotNull UpdateAuthenticationStrategyEntity updateStrategy
    ) {
        return authenticationStrategyService.update(
            GraviteeContext.getExecutionContext(),
            authenticationStrategyId,
            updateStrategy
        );
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLIENT_REGISTRATION_PROVIDER, acls = RolePermissionAction.DELETE) })
    @Operation(
        summary = "Delete an authentication strategy",
        description = "User must have the ENVIRONMENT_CLIENT_REGISTRATION_PROVIDER[DELETE] permission to use this service"
    )
    @ApiResponse(responseCode = "204", description = "Authentication strategy successfully deleted")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public void deleteAuthenticationStrategy() {
        authenticationStrategyService.delete(GraviteeContext.getExecutionContext(), authenticationStrategyId);
    }
}
