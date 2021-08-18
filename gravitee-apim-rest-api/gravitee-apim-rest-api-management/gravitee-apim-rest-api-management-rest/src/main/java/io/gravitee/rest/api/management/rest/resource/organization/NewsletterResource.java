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
package io.gravitee.rest.api.management.rest.resource.organization;

import static io.gravitee.common.http.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.ok;

import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.NewsletterService;
import io.gravitee.rest.api.service.UserService;
import io.swagger.annotations.*;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.*;

/**
 * Defines the REST resources to manage Newsletter.
 *
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Newsletter" })
public class NewsletterResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private NewsletterService newsletterService;

    @Inject
    private UserService userService;

    @POST
    @Path("/_subscribe")
    @ApiOperation(value = "Subscribe to the newsletter the authenticated user")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Updated user", response = UserEntity.class),
            @ApiResponse(code = 400, message = "Invalid user profile"),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Response subscribeNewsletterToCurrentUser(@Valid @NotNull final String email) {
        UserEntity userEntity = userService.findById(getAuthenticatedUser());
        UpdateUserEntity user = new UpdateUserEntity(userEntity);
        user.setNewsletter(true);
        return ok(userService.update(userEntity.getId(), user, email)).build();
    }

    @GET
    @Path("/taglines")
    @ApiOperation(value = "Get taglines to display in the newsletter")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Retrieved taglines", response = List.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Response getTaglines() {
        return ok(newsletterService.getTaglines()).build();
    }
}
