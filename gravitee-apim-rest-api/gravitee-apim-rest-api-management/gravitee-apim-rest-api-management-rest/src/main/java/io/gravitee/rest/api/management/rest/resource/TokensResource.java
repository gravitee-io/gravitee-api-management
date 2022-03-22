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
package io.gravitee.rest.api.management.rest.resource;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.NewTokenEntity;
import io.gravitee.rest.api.model.TokenEntity;
import io.gravitee.rest.api.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "User Tokens")
public class TokensResource extends AbstractResource {

    @Inject
    private TokenService tokenService;

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
    public List<TokenEntity> getTokens() {
        return tokenService.findByUser(getAuthenticatedUser()).stream().sorted(comparing(TokenEntity::getCreatedAt)).collect(toList());
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
    public TokenEntity createTokens(@Valid @NotNull final NewTokenEntity token) {
        return tokenService.create(token, getAuthenticatedUser());
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Revoke all user's personal tokens")
    @ApiResponse(responseCode = "204", description = "User's personal tokens revoked")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public void revokeAllTokens() {
        tokenService.revokeByUser(getAuthenticatedUser());
    }

    @Path("{token}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Revoke a single user's personal tokens")
    @ApiResponse(responseCode = "204", description = "User's personal token revoked")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public void revokeToken(@PathParam("token") String tokenId) {
        tokenService.revoke(tokenId);
    }
}
