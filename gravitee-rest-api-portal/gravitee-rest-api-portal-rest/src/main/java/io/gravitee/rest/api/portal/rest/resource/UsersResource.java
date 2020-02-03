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

import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.portal.rest.mapper.UserMapper;
import io.gravitee.rest.api.portal.rest.model.FinalizeRegistrationInput;
import io.gravitee.rest.api.portal.rest.model.RegisterUserInput;
import io.gravitee.rest.api.portal.rest.model.User;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.security.Permission;
import io.gravitee.rest.api.portal.rest.security.Permissions;
import io.gravitee.rest.api.service.UserService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UsersResource extends AbstractResource {

    @Inject
    private UserMapper userMapper;

    @Inject
    private UserService userService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.MANAGEMENT_USERS, acls = READ) })
    public Response getUsers(@BeanParam PaginationParam paginationParam) {
        UserCriteria criteria = new UserCriteria.Builder().build();
        List<User> users = userService
                .search(criteria, new PageableImpl(paginationParam.getPage(), paginationParam.getSize())).getContent()
                .stream().map(userMapper::convert).collect(Collectors.toList());

        // No pagination, because userService did it already
        return createListResponse(users, paginationParam, false);
    }

    /**
     * Register a new user. Generate a token and send it in an email to allow a user
     * to create an account.
     */
    @POST
    @Path("/registration")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerUser(@Valid @NotNull(message = "Input must not be null.") RegisterUserInput registerUserInput) {
        UserEntity newUser = userService.register(userMapper.convert(registerUserInput), registerUserInput.getConfirmationPageUrl());
        if (newUser != null) {
            return Response.ok().entity(userMapper.convert(newUser)).build();
        }

        return Response.serverError().build();
    }

    @POST
    @Path("/registration/_finalize")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response finalizeRegistration(@Valid @NotNull(message = "Input must not be null.") FinalizeRegistrationInput finalizeRegistrationInput) {
        UserEntity newUser = userService.finalizeRegistration(userMapper.convert(finalizeRegistrationInput));
        if (newUser != null) {
            return Response.ok().entity(userMapper.convert(newUser)).build();
        }

        return Response.serverError().build();
    }
}
