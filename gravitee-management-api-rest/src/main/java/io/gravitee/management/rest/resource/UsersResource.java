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
import io.gravitee.management.model.*;
import io.gravitee.management.service.UserService;
import io.swagger.annotations.Api;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.*;
import java.io.ByteArrayOutputStream;
import java.net.URI;

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
public class UsersResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private UserService userService;

    /**
     * Register a new user.
     * Generate a token and send it in an email to allow a user to create an account.
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
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
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
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

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public UserEntity getUser(@PathParam("id") String userId) {
        UserEntity user = userService.findById(userId);

        // Delete password for security reason
        user.setPassword(null);
        user.setPicture(null);

        return user;
    }

    @GET
    @Path("{id}/avatar")
    public Response getUserAvatar(@PathParam("id") String id, @Context Request request) {
        PictureEntity picture = userService.getPicture(id);

        if (picture == null) {
            throw new NotFoundException();
        }

        if (picture instanceof UrlPictureEntity) {
            return Response.temporaryRedirect(URI.create(((UrlPictureEntity)picture).getUrl())).build();
        }

        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);

        InlinePictureEntity image = (InlinePictureEntity) picture;

        EntityTag etag = new EntityTag(Integer.toString(new String(image.getContent()).hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

        if (builder != null) {
            // Preconditions are not met, returning HTTP 304 'not-modified'
            return builder
                    .cacheControl(cc)
                    .build();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(image.getContent(), 0, image.getContent().length);

        return Response
                .ok()
                .entity(baos)
                .cacheControl(cc)
                .tag(etag)
                .type(image.getType())
                .build();
    }
}
