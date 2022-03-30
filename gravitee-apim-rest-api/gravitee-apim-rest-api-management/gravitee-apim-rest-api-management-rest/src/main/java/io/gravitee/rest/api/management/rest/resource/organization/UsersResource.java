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

import static io.gravitee.common.http.MediaType.APPLICATION_JSON;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.management.rest.model.Pageable;
import io.gravitee.rest.api.management.rest.model.wrapper.UserPageResult;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.NewPreRegisterUserEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.validator.ValidNewPreRegisterUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * Defines the REST resources to manage Users.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Users")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class UsersResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private UserService userService;

    @GET
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS, acls = READ))
    @Operation(
        summary = "Search for users using the search engine",
        description = "User must have the ORGANIZATION_USERS[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "List users matching the query criteria",
        content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = UserPageResult.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public UserPageResult getAllUsers(@Parameter(name = "q") @QueryParam("q") String query, @Valid @BeanParam Pageable pageable) {
        Page<UserEntity> users = userService.search(GraviteeContext.getExecutionContext(), query, pageable.toPageable());
        return new UserPageResult(users, pageable.getSize());
    }

    @POST
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS, acls = CREATE))
    @Operation(summary = "Create a user", description = "User must have the ORGANIZATION_USERS[CREATE] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "List users matching the query criteria",
        content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = UserEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response createUser(@ValidNewPreRegisterUser NewPreRegisterUserEntity newPreRegisterUserEntity) {
        UserEntity newUser = userService.create(GraviteeContext.getExecutionContext(), newPreRegisterUserEntity);
        if (newUser != null) {
            return Response.ok().entity(newUser).build();
        }

        return Response.serverError().build();
    }

    @Path("{userId}")
    public UserResource getUserResource() {
        return resourceContext.getResource(UserResource.class);
    }

    @Path("registration")
    public UsersRegistrationResource getUsersRegistrationResource() {
        return resourceContext.getResource(UsersRegistrationResource.class);
    }
}
