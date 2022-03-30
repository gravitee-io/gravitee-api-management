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
package io.gravitee.rest.api.management.rest.resource;

import static java.util.stream.Collectors.toList;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.NewTopApiEntity;
import io.gravitee.rest.api.model.TopApiEntity;
import io.gravitee.rest.api.model.UpdateTopApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.TopApiService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.UriBuilder;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Top APIs")
public class TopApisResource extends AbstractResource {

    @Inject
    private TopApiService topApiService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List of top APIs", description = "User must have the PORTAL_TOP_APIS[READ] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "List of top APIs",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = TopApiEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_TOP_APIS, acls = RolePermissionAction.READ) })
    public List<TopApiEntity> getTopApis() {
        return topApiService.findAll(GraviteeContext.getExecutionContext()).stream().peek(addPictureUrl()).collect(toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a top API", description = "User must have the PORTAL_TOP_APIS[CREATE] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "List of top APIs",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = TopApiEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_TOP_APIS, acls = RolePermissionAction.CREATE) })
    public List<TopApiEntity> createTopApi(@Valid @NotNull final NewTopApiEntity topApi) {
        return topApiService.create(GraviteeContext.getExecutionContext(), topApi).stream().peek(addPictureUrl()).collect(toList());
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update a top API", description = "User must have the PORTAL_TOP_APIS[UPDATE] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "List of top APIs",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = TopApiEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_TOP_APIS, acls = RolePermissionAction.UPDATE) })
    public List<TopApiEntity> updateTopApi(@Valid @NotNull final List<UpdateTopApiEntity> topApis) {
        return topApiService.update(GraviteeContext.getExecutionContext(), topApis).stream().peek(addPictureUrl()).collect(toList());
    }

    @Path("{topAPI}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Delete an existing top API",
        description = "User must have the PORTAL_TOP_APIS[DELETE] permission to use this service"
    )
    @ApiResponse(responseCode = "204", description = "Top API successfully deleted")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_TOP_APIS, acls = RolePermissionAction.DELETE) })
    public void deleteTopApi(@PathParam("topAPI") String topAPI) {
        topApiService.delete(GraviteeContext.getExecutionContext(), topAPI);
    }

    private Consumer<TopApiEntity> addPictureUrl() {
        return topApiEntity -> {
            final UriBuilder ub = uriInfo.getBaseUriBuilder();
            final UriBuilder uriBuilder = ub
                .path("organizations")
                .path(GraviteeContext.getCurrentOrganization())
                .path("environments")
                .path(GraviteeContext.getCurrentEnvironment())
                .path("apis")
                .path(topApiEntity.getApi())
                .path("picture");
            topApiEntity.setPictureUrl(uriBuilder.build().toString());
        };
    }
}
