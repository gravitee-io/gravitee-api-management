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
package io.gravitee.rest.api.portal.rest.resource;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.UpdateUserEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.portal.rest.mapper.UserMapper;
import io.gravitee.rest.api.portal.rest.model.User;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.UnauthorizedAccessException;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private UserService userService;

    @Inject
    private UserMapper userMapper;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentUser() {
        final String authenticatedUser = getAuthenticatedUser();
        UserEntity userEntity = userService.findById(authenticatedUser);

        return Response.ok(userMapper.convert(userEntity)).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateCurrentUser(@Valid User user) {
        if(!getAuthenticatedUser().equals(user.getId())) {
            throw new UnauthorizedAccessException();
        }

        UpdateUserEntity updateUserEntity = new UpdateUserEntity();
        updateUserEntity.setEmail(user.getEmail());
        updateUserEntity.setFirstname(user.getFirstName());
        updateUserEntity.setLastname(user.getLastName());
        updateUserEntity.setPicture(checkAndScaleImage(user.getAvatar()));

        UserEntity updatedUser = userService.update(user.getId(), updateUserEntity);

        return Response
                .ok(userMapper.convert(updatedUser))
                .build();

    }
}
