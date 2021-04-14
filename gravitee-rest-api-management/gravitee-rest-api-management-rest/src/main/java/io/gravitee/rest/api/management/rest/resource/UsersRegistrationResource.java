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

import static io.gravitee.common.http.MediaType.APPLICATION_JSON;

import io.gravitee.rest.api.model.NewExternalUserEntity;
import io.gravitee.rest.api.model.RegisterUserEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * Defines the REST resources to manage users registration.
 *
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "User Registration" })
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class UsersRegistrationResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private UserService userService;

    /**
     * Register a new user.
     * Generate a token and send it in an email to allow a user to create an account.
     */
    @POST
    @ApiOperation(value = "Register a user", notes = "User registration must be enabled")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "User successfully registered", response = UserEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Response registerUser(@Valid NewExternalUserEntity newExternalUserEntity) {
        UserEntity newUser = userService.register(newExternalUserEntity);
        if (newUser != null) {
            return Response.ok().entity(newUser).build();
        }

        return Response.serverError().build();
    }

    @POST
    @Path("/finalize")
    @ApiOperation(value = "Finalize user registration", notes = "User registration must be enabled")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "User successfully created", response = UserEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Response finalizeUserRegistration(@Valid RegisterUserEntity registerUserEntity) {
        UserEntity newUser = userService.finalizeRegistration(registerUserEntity);
        if (newUser != null) {
            return Response.ok().entity(newUser).build();
        }

        return Response.serverError().build();
    }
}
