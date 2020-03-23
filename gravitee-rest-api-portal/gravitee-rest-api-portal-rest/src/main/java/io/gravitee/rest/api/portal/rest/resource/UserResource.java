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
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.PortalConfigEntity.Management;
import io.gravitee.rest.api.portal.rest.mapper.UserMapper;
import io.gravitee.rest.api.portal.rest.model.User;
import io.gravitee.rest.api.portal.rest.model.UserConfig;
import io.gravitee.rest.api.portal.rest.model.UserInput;
import io.gravitee.rest.api.security.cookies.JWTCookieGenerator;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.UnauthorizedAccessException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.net.URI;

import static io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper.userURL;
import static javax.ws.rs.core.Response.status;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;
    @Inject
    private ConfigService configService;
    @Inject
    private UserService userService;
    @Inject
    private UserMapper userMapper;
    @Context
    private HttpServletResponse response;
    @Autowired
    private JWTCookieGenerator jwtCookieGenerator;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentUser() {
        final String authenticatedUser = getAuthenticatedUser();
        try {
            UserEntity userEntity = userService.findById(authenticatedUser);
            User currentUser = userMapper.convert(userEntity);

            boolean withManagement = (authenticatedUser != null && permissionService.hasManagementRights(authenticatedUser));
            if (withManagement) {
                Management managementConfig = this.configService.getPortalConfig().getManagement();
                if (managementConfig != null && managementConfig.getUrl() != null) {
                    UserConfig userConfig = new UserConfig();
                    userConfig.setManagementUrl(managementConfig.getUrl());
                    currentUser.setConfig(userConfig);
                }
            }

            currentUser.setLinks(userMapper.computeUserLinks(userURL(uriInfo.getBaseUriBuilder()), userEntity.getUpdatedAt()));
            return Response
                    .ok(currentUser)
                    .build();
        } catch (final UserNotFoundException unfe) {
            response.addCookie(jwtCookieGenerator.generate(null));
            return status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateCurrentUser(@Valid @NotNull(message = "Input must not be null.") UserInput user) {
        if(!getAuthenticatedUser().equals(user.getId())) {
            throw new UnauthorizedAccessException();
        }
        userService.findById(getAuthenticatedUser());

        UpdateUserEntity updateUserEntity = new UpdateUserEntity();
        updateUserEntity.setPicture(checkAndScaleImage(user.getAvatar()));

        UserEntity updatedUser = userService.update(user.getId(), updateUserEntity);

        final User currentUser = userMapper.convert(updatedUser);
        currentUser.setLinks(userMapper.computeUserLinks(userURL(uriInfo.getBaseUriBuilder()), updatedUser.getUpdatedAt()));
        return Response.ok(currentUser).build();
    }

    @GET
    @Path("avatar")
    public Response getCurrentUserAvatar(@Context Request request) {
        String userId = userService.findById(getAuthenticatedUser()).getId();
        PictureEntity picture = userService.getPicture(userId);

        if (picture == null) {
            return Response.ok().build();
        }

        if (picture instanceof UrlPictureEntity) {
            return Response.temporaryRedirect(URI.create(((UrlPictureEntity) picture).getUrl())).build();
        }

        return createPictureResponse(request, (InlinePictureEntity) picture);
    }

    @Path("notifications")
    public UserNotificationsResource getUserNotificationsResource() {
        return resourceContext.getResource(UserNotificationsResource.class);
    }
}
