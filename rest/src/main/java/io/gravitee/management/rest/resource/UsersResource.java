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

import io.gravitee.definition.jackson.model.NewUserEntity;
import io.gravitee.definition.jackson.model.UserEntity;
import io.gravitee.management.service.UserService;

import java.net.URI;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Defines the REST resources to manage Users.
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Path("/users")
public class UsersResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private UserService userService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * Create a new user.
     * @param newUserEntity
     * @return
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUser(@Valid NewUserEntity newUserEntity) {
    	// encode password
    	if(passwordEncoder == null) {
    		passwordEncoder = NoOpPasswordEncoder.getInstance();
    	}
    	newUserEntity.setPassword(passwordEncoder.encode(newUserEntity.getPassword()));
        
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
    public UserResource getUserResource() {
        return resourceContext.getResource(UserResource.class);
    }
}