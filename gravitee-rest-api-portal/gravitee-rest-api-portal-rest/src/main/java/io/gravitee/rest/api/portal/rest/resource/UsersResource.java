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
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.PictureEntity;
import io.gravitee.rest.api.model.UrlPictureEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.portal.rest.mapper.UserMapper;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.security.Permission;
import io.gravitee.rest.api.portal.rest.security.Permissions;
import io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper;
import io.gravitee.rest.api.service.IdentityService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.AbstractManagementException;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import java.net.URI;
import java.util.Comparator;
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

    @Inject
    private IdentityService identityService;
    
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

    @POST
    @Path("_reset_password")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response resetUserPassword(@NotNull(message = "Input must not be null.") @Valid ResetUserPasswordInput resetUserPasswordInput) {
        try {
            userService.resetPasswordFromSourceId(resetUserPasswordInput.getUsername(), resetUserPasswordInput.getResetPageUrl());
        } catch (AbstractManagementException e) {
            LOGGER.warn("Problem while resetting a password : {}", e.getMessage());
        }
        return Response.noContent().build();
    }

    @POST
    @Path("_search")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_USERS, acls = READ) })
    public Response getUsers(@QueryParam("q") String query, @BeanParam PaginationParam paginationParam) {
        String q = query;
        if (q == null) {
            q = "*";
        }
        List<User> users = identityService.search(q).stream()
                .map(searchableUser -> userMapper.convert(searchableUser)
                        .links(userMapper.computeUserLinks(PortalApiLinkHelper.usersURL(uriInfo.getBaseUriBuilder(), searchableUser.getId()), null))
                )
                .sorted((o1, o2) -> CASE_INSENSITIVE_ORDER.compare(o1.getLastName(), o2.getLastName()))
                .collect(Collectors.toList());

        // No pagination, because userService did it already
        return createListResponse(users, paginationParam, false);
    }

    @GET
    @Path("/{userId}/avatar")
    public Response getUserAvatar(@Context Request request, @PathParam("userId") String userId) {
        PictureEntity picture = userService.getPicture(userId);

        if (picture == null) {
            return Response.ok().build();
        }

        if (picture instanceof UrlPictureEntity) {
            return Response.temporaryRedirect(URI.create(((UrlPictureEntity) picture).getUrl())).build();
        }

        return createPictureResponse(request, (InlinePictureEntity) picture);
    }
    
    private static final Comparator<String> CASE_INSENSITIVE_ORDER = new CaseInsensitiveComparator();

    private static class CaseInsensitiveComparator
            implements Comparator<String>, java.io.Serializable {
        // use serialVersionUID from JDK 1.2.2 for interoperability
        private static final long serialVersionUID = 8575799808933029326L;

        public int compare(String s1, String s2) {
            if (s1 == null) return 1;
            if (s2 == null) return -1;

            int n1 = s1.length();
            int n2 = s2.length();
            int min = Math.min(n1, n2);
            for (int i = 0; i < min; i++) {
                char c1 = s1.charAt(i);
                char c2 = s2.charAt(i);
                if (c1 != c2) {
                    c1 = Character.toUpperCase(c1);
                    c2 = Character.toUpperCase(c2);
                    if (c1 != c2) {
                        c1 = Character.toLowerCase(c1);
                        c2 = Character.toLowerCase(c2);
                        if (c1 != c2) {
                            // No overflow because of numeric promotion
                            return c1 - c2;
                        }
                    }
                }
            }
            return n1 - n2;
        }

        /** Replaces the de-serialized object. */
        private Object readResolve() { return CASE_INSENSITIVE_ORDER; }
    }
}
