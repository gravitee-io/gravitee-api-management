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

import io.gravitee.common.http.MediaType;
import io.gravitee.management.idp.api.authentication.UserDetails;
import io.gravitee.management.model.UpdateUserEntity;
import io.gravitee.management.model.permissions.Role;
import io.gravitee.management.security.JWTCookieGenerator;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.exceptions.ForbiddenAccessException;
import io.gravitee.management.service.exceptions.UserNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Azize Elamrani (azize at gravitee.io)
 * @author GraviteeSource Team
 */
@Path("/user")
@Api(tags = {"User"})
public class UserResource extends AbstractResource {

    private static Logger LOG = LoggerFactory.getLogger(UserResource.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JWTCookieGenerator jwtCookieGenerator;

    @Context
    private HttpServletResponse response;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the authenticated user")
    public Response getCurrentUser() {
        final Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            final UserDetails details = ((UserDetails) principal);
            final String username = details.getUsername();
            try {
                userService.findByName(username);
            } catch (final UserNotFoundException unfe) {
                LOG.info("User '{}' no longer exists.", username, unfe);
                return logout();
            }
            List<GrantedAuthority> authorities = new ArrayList<>(details.getAuthorities());
            authorities.add(new SimpleGrantedAuthority(Role.API_CONSUMER.name()));
            return Response.ok(
                    new UserDetails(details.getUsername(), details.getPassword(), authorities),
                    MediaType.APPLICATION_JSON).build();
        }
        return Response.ok().build();
    }

    @PUT
    @Path("/{username}")
    @ApiOperation(value = "Update user")
    public Response updateCurrentUser(@PathParam("username") final String username, @Valid @NotNull final UpdateUserEntity user) {
        if (!username.equals(getAuthenticatedUsername())) {
            throw new ForbiddenAccessException();
        }

        return Response.ok(userService.update(user)).build();
    }

    @GET
    @Path("/{username}/picture")
    @ApiOperation(value = "Get user's picture")
    public Response getCurrentUserPicture(@PathParam("username") final String username) {
        return Response.ok(userService.findByName(username).getPicture()).build();
    }

    @POST
    @Path("/login")
    @ApiOperation(value = "Login")
    public Response login() {
        return Response.ok().build();
    }

    @POST
    @Path("/logout")
    @ApiOperation(value = "Logout")
    public Response logout() {
        response.addCookie(jwtCookieGenerator.generate(null));
        return Response.ok().build();
    }
}
