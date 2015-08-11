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
package io.gravitee.management.api.resource;

import io.gravitee.management.api.model.NewUserEntity;
import io.gravitee.management.api.model.UserEntity;
import io.gravitee.management.api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * Defines the REST resources to manage Users.
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
@Path("/users")
public class UsersResource {

    @Context
    private ResourceContext resourceContext;

    @Autowired
    private UserService userService;

    /**
     * Create a new user.
     * @param newUserEntity
     * @return
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUser(@Valid NewUserEntity newUserEntity) {
        UserEntity newUser = userService.create(newUserEntity);
        if (newUser != null) {
            return Response
                    .created(URI.create("/users/" + newUser.getUsername()))
                    .entity(newUser)
                    .build();
        }

        return Response.serverError().build();
    }

    @Path("{username}")
    public UserResource getUserResource(@PathParam("username") String username) {
        UserResource userResource = resourceContext.getResource(UserResource.class);
        userResource.setUsername(username);

        return userResource;
    }
}