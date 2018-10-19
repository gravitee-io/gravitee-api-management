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
package io.gravitee.management.rest.resource;

import io.gravitee.common.data.domain.Page;
import io.gravitee.management.model.NewExternalUserEntity;
import io.gravitee.management.model.RegisterUserEntity;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.rest.model.Pageable;
import io.gravitee.management.rest.model.PagedResult;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.UserService;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static io.gravitee.common.http.MediaType.APPLICATION_JSON;
import static io.gravitee.management.model.permissions.RolePermissionAction.READ;

/**
 * Defines the REST resources to manage Users.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/users")
@Api(tags = {"User"})
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class UsersResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private UserService userService;

    @GET
    @Permissions(@Permission(value = RolePermission.MANAGEMENT_USERS, acls = READ))
    @ApiOperation(
            value = "Search for API using the search engine",
            notes = "User must have the MANAGEMENT_USERS[READ] permission to use this service"
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "List users matching the query criteria", response = UserEntity.class, responseContainer = "PagedResult"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public PagedResult<UserEntity> findAll(
            @ApiParam(name = "q")
            @QueryParam("q") String query,
            @Valid @BeanParam Pageable pageable) {
        Page<UserEntity> users = userService.search(query, pageable.toPageable());
        return new PagedResult<>(users, pageable.getSize());
    }

    /**
     * Register a new user.
     * Generate a token and send it in an email to allow a user to create an account.
     */
    @POST
    @Path("/register")
    public Response registerUser(@Valid NewExternalUserEntity newExternalUserEntity) {
        UserEntity newUser = userService.register(newExternalUserEntity);
        if (newUser != null) {
            return Response
                    .ok()
                    .entity(newUser)
                    .build();
        }

        return Response.serverError().build();
    }
    
    @POST
    public Response createUser(@Valid RegisterUserEntity registerUserEntity) {
        UserEntity newUser = userService.create(registerUserEntity);
        if (newUser != null) {
            return Response
                    .ok()
                    .entity(newUser)
                    .build();
        }

        return Response.serverError().build();
    }

    @Path("{id}")
    public UserResource getUserResource() {
        return resourceContext.getResource(UserResource.class);
    }
}
