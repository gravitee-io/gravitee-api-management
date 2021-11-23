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
package io.gravitee.rest.api.management.rest.resource.organization;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.NewTokenEntity;
import io.gravitee.rest.api.model.TokenEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.TokenService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "User Tokens" })
public class UserTokensResource extends AbstractResource {

    @Inject
    private TokenService tokenService;

    @PathParam("userId")
    @ApiParam(name = "userId", required = true)
    private String userId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List user's personal tokens")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "User's personal tokens"),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS, acls = { RolePermissionAction.READ }))
    public List<TokenEntity> getTokens() {
        return tokenService.findByUser(userId).stream().sorted(comparing(TokenEntity::getCreatedAt)).collect(toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a personal token")
    @ApiResponses(
        {
            @ApiResponse(code = 201, message = "A new personal token", response = TokenEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS, acls = { RolePermissionAction.READ, RolePermissionAction.CREATE }))
    public Response createToken(@Valid @NotNull final NewTokenEntity token) {
        return Response.status(Response.Status.CREATED).entity(tokenService.create(token, userId)).build();
    }

    @Path("{token}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Revoke a single user's personal tokens")
    @ApiResponses(
        {
            @ApiResponse(code = 204, message = "User's personal token revoked"),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS, acls = { RolePermissionAction.READ, RolePermissionAction.DELETE }))
    public void revokeToken(@PathParam("token") String tokenId) {
        tokenService.revoke(tokenId);
    }
}
