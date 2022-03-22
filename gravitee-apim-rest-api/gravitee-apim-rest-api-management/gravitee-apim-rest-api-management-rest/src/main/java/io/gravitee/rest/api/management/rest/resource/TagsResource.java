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

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.NewTagEntity;
import io.gravitee.rest.api.model.TagEntity;
import io.gravitee.rest.api.model.TagReferenceType;
import io.gravitee.rest.api.model.UpdateTagEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Sharding Tags")
public class TagsResource extends AbstractResource {

    @Inject
    private TagService tagService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List sharding tags")
    @ApiResponse(
        responseCode = "200",
        description = "List of sharding tags",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = TagEntity.class)))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public List<TagEntity> getTags() {
        return tagService
            .findByReference(GraviteeContext.getCurrentOrganization(), TagReferenceType.ORGANIZATION)
            .stream()
            .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()))
            .collect(Collectors.toList());
    }

    @GET
    @Path("{tag}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a sharding tag", description = "User must have the MANAGEMENT_TAG[READ] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Tag",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TagEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_TAG, acls = RolePermissionAction.READ),
            @Permission(value = RolePermission.ENVIRONMENT_TAG, acls = RolePermissionAction.READ),
        }
    )
    public TagEntity getTag(@PathParam("tag") String tag) {
        return tagService.findByIdAndReference(tag, GraviteeContext.getCurrentOrganization(), TagReferenceType.ORGANIZATION);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a sharding tag", description = "User must have the MANAGEMENT_TAG[CREATE] permission to use this service")
    @ApiResponse(
        responseCode = "201",
        description = "A new sharding tag",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TagEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_TAG, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.ORGANIZATION_TAG, acls = RolePermissionAction.CREATE),
        }
    )
    public TagEntity createTag(@Valid @NotNull final NewTagEntity tag) {
        return tagService.create(tag, GraviteeContext.getCurrentOrganization(), TagReferenceType.ORGANIZATION);
    }

    @PUT
    @Path("{tag}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update an existing sharding tag",
        description = "User must have the MANAGEMENT_TAG[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Sharding tag",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TagEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_TAG, acls = RolePermissionAction.UPDATE),
            @Permission(value = RolePermission.ORGANIZATION_TAG, acls = RolePermissionAction.UPDATE),
        }
    )
    public TagEntity updateTag(@PathParam("tag") String tagId, @Valid @NotNull final UpdateTagEntity tag) {
        return tagService.update(tag, GraviteeContext.getCurrentOrganization(), TagReferenceType.ORGANIZATION);
    }

    @Path("{tag}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Delete an existing sharding tag",
        description = "User must have the MANAGEMENT_TAG[DELETE] permission to use this service"
    )
    @ApiResponse(responseCode = "204", description = "Sharding tag successfully deleted")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_TAG, acls = RolePermissionAction.DELETE),
            @Permission(value = RolePermission.ORGANIZATION_TAG, acls = RolePermissionAction.DELETE),
        }
    )
    public void deleteTag(@PathParam("tag") String tag) {
        tagService.delete(tag, GraviteeContext.getCurrentOrganization(), TagReferenceType.ORGANIZATION);
    }
}
