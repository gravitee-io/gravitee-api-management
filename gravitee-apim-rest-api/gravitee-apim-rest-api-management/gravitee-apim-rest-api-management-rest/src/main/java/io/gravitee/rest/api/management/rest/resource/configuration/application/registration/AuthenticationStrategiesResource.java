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
import io.gravitee.rest.api.model.configuration.application.registration.NewAuthenticationStrategyEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.application.AuthenticationStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;

@Tag(name = "Authentication Strategies")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthenticationStrategiesResource extends AbstractResource {

    @Autowired
    private AuthenticationStrategyService authenticationStrategyService;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Permissions(@Permission(value = RolePermission.ENVIRONMENT_CLIENT_REGISTRATION_PROVIDER, acls = RolePermissionAction.READ))
    @Operation(
        summary = "Get the list of authentication strategies",
        description = "User must have the ENVIRONMENT_CLIENT_REGISTRATION_PROVIDER[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "List authentication strategies",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = AuthenticationStrategyEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Set<AuthenticationStrategyEntity> getAuthenticationStrategies() {
        return authenticationStrategyService.findAll(GraviteeContext.getExecutionContext());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLIENT_REGISTRATION_PROVIDER, acls = RolePermissionAction.CREATE) })
    @Operation(
        summary = "Create an authentication strategy",
        description = "User must have the ENVIRONMENT_CLIENT_REGISTRATION_PROVIDER[CREATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Authentication strategy successfully created",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = AuthenticationStrategyEntity.class)
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response createAuthenticationStrategy(
        @Parameter(name = "strategy", required = true) @Valid @NotNull NewAuthenticationStrategyEntity newStrategy
    ) {
        AuthenticationStrategyEntity created = authenticationStrategyService.create(
            GraviteeContext.getExecutionContext(),
            newStrategy
        );

        if (created != null) {
            return Response.created(this.getLocationHeader(created.getId()))
                .entity(created)
                .build();
        }

        return Response.serverError().build();
    }

    @Path("{authenticationStrategy}")
    public AuthenticationStrategyResource getAuthenticationStrategyResource() {
        return resourceContext.getResource(AuthenticationStrategyResource.class);
    }
}
