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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.configuration.application.registration.AuthenticationStrategyEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.application.AuthenticationStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Portal endpoint for listing available authentication strategies.
 * Developers use this to select which strategy to use when creating applications.
 */
@Tag(name = "Authentication Strategies")
@Produces(MediaType.APPLICATION_JSON)
public class AuthenticationStrategiesResource extends AbstractResource {

    @Autowired
    private AuthenticationStrategyService authenticationStrategyService;

    @GET
    @Operation(summary = "List available authentication strategies for the current environment")
    @ApiResponse(
        responseCode = "200",
        description = "List of available authentication strategies",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = AuthenticationStrategyEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Set<AuthenticationStrategyEntity> getAuthenticationStrategies() {
        return authenticationStrategyService.findAll(GraviteeContext.getExecutionContext());
    }

    @GET
    @Path("{strategyId}")
    @Operation(summary = "Get a specific authentication strategy")
    @ApiResponse(
        responseCode = "200",
        description = "Authentication strategy details",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = AuthenticationStrategyEntity.class)
        )
    )
    @ApiResponse(responseCode = "404", description = "Authentication strategy not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public AuthenticationStrategyEntity getAuthenticationStrategy(@PathParam("strategyId") String strategyId) {
        return authenticationStrategyService.findById(
            GraviteeContext.getExecutionContext().getEnvironmentId(),
            strategyId
        );
    }
}
