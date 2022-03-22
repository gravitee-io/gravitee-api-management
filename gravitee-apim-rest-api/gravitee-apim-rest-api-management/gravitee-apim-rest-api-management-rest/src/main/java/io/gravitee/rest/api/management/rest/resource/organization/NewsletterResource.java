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

import static javax.ws.rs.core.Response.ok;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.service.NewsletterService;
import io.gravitee.rest.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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
@Tag(name = "Newsletter")
public class NewsletterResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private NewsletterService newsletterService;

    @Inject
    private UserService userService;

    @POST
    @Path("/_subscribe")
    @Operation(summary = "Subscribe to the newsletter the authenticated user")
    @ApiResponse(
        responseCode = "200",
        description = "Updated user",
        content = @Content(schema = @Schema(implementation = UserEntity.class))
    )
    @ApiResponse(responseCode = "400", description = "Invalid user profile")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response subscribeNewsletterToCurrentUser(@Valid @NotNull final String email) {
        UserEntity userEntity = userService.findById(getAuthenticatedUser());
        UpdateUserEntity user = new UpdateUserEntity(userEntity);
        user.setNewsletter(true);
        return ok(userService.update(userEntity.getId(), user, email)).build();
    }

    @GET
    @Path("/taglines")
    @Operation(summary = "Get taglines to display in the newsletter")
    @ApiResponse(
        responseCode = "200",
        description = "Retrieved taglines",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(type = "string")))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response getTaglines() {
        return ok(newsletterService.getTaglines()).build();
    }
}
