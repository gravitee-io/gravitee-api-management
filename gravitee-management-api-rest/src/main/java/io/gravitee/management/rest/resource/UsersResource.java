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
import io.gravitee.management.model.NewUserEntity;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.model.providers.User;
import io.gravitee.management.service.IdentityService;
import io.gravitee.management.service.UserService;
import io.swagger.annotations.Api;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.*;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Defines the REST resources to manage Users.
 *
 * @author David BRASSELY (david at graviteesource.com)
 */
@Path("/users")
@Api(tags = {"User"})
public class UsersResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private UserService userService;

    @Inject
    private IdentityService identityService;
    
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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<User> listUsers(@QueryParam("query") String query) {
        if (query != null && ! query.trim().isEmpty()) {
            return identityService.search(query);
        } else {
            return userService.findAll()
                    .stream()
                    .map(userEntity -> {
                        User user = new User();
                        user.setEmail(userEntity.getEmail());
                        user.setLastname(userEntity.getLastname());
                        user.setFirstname(userEntity.getFirstname());
                        user.setId(userEntity.getUsername());
                        return user;
                    })
                    .sorted((o1, o2) -> CASE_INSENSITIVE_ORDER.compare(o1.getLastname(), o2.getLastname()))
                    .collect(Collectors.toList());
        }
    }

    @GET
    @Path("{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public UserEntity getUser(@PathParam("username") String username) {
        UserEntity user = userService.findByName(username);

        // Delete password for security reason
        user.setPassword(null);
        user.setPicture(null);

        return user;
    }

    @GET
    @Path("{username}/picture")
    public Response getUserPicture(@PathParam("username") String username, @Context Request request) {
        UserEntity user = userService.findByName(username);

        if (user.getPicture() == null) {
            throw new NotFoundException();
        }

        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);

        String picture = user.getPicture();
        String [] parts = picture.split("base64,");
        String type = parts[0].split("data:")[1];
        type = type.substring(0, type.length() - 1);

        EntityTag etag = new EntityTag(Integer.toString(picture.hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

        if (builder != null) {
            // Preconditions are not met, returning HTTP 304 'not-modified'
            return builder
                    .cacheControl(cc)
                    .build();
        }

        byte[] content = Base64.getDecoder().decode(parts[1]);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(content, 0, content.length);

        return Response
                .ok()
                .entity(baos)
                .cacheControl(cc)
                .tag(etag)
                .type(type)
                .build();
    }

    public static final Comparator<String> CASE_INSENSITIVE_ORDER
            = new CaseInsensitiveComparator();

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
