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
package io.gravitee.rest.api.management.rest.resource.organization;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.model.Token;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.NewTokenEntity;
import io.gravitee.rest.api.model.TokenEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.TokenService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.exceptions.TokenNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "User Tokens")
public class UserTokensResource extends AbstractResource {

    @Inject
    private TokenService tokenService;

    @Inject
    private UserService userService;

    @PathParam("userId")
    @Parameter(name = "userId", required = true, hidden = true)
    private String userId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List user's personal tokens")
    @ApiResponse(
        responseCode = "200",
        description = "User's personal tokens",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = TokenEntity.class))
        )
    )
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS_TOKEN, acls = { RolePermissionAction.READ }))
    public List<TokenEntity> getUserTokens() {
        // Check that user belongs to current organization
        userService.findById(GraviteeContext.getExecutionContext(), userId);

        return tokenService.findByUser(userId).stream().sorted(comparing(TokenEntity::getCreatedAt)).collect(toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a personal token")
    @ApiResponse(
        responseCode = "201",
        description = "A new personal token",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TokenEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS_TOKEN, acls = { RolePermissionAction.CREATE }))
    public Response createToken(@Valid @NotNull final NewTokenEntity token) {
        // Check that user belongs to current organization
        userService.findById(GraviteeContext.getExecutionContext(), userId);

        return Response
            .status(Response.Status.CREATED)
            .entity(tokenService.create(GraviteeContext.getExecutionContext(), token, userId))
            .build();
    }

    @Path("{token}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Revoke a single user's personal tokens")
    @ApiResponse(responseCode = "204", description = "User's personal token revoked")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS_TOKEN, acls = { RolePermissionAction.DELETE }))
    public void revokeToken(@PathParam("token") String tokenId) {
        // Check that user belongs to current organization
        userService.findById(GraviteeContext.getExecutionContext(), userId);

        // Check that token exists and belongs to user
        if (!tokenService.tokenExistsForUser(tokenId, userId)) {
            throw new TokenNotFoundException(tokenId);
        }

        tokenService.revoke(GraviteeContext.getExecutionContext(), tokenId);
    }
}
