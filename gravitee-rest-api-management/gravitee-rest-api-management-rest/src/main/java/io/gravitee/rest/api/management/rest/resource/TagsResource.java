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
import io.gravitee.rest.api.model.NewTagEntity;
import io.gravitee.rest.api.model.TagEntity;
import io.gravitee.rest.api.model.UpdateTagEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.service.TagService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Sharding Tags"})
public class TagsResource extends AbstractResource  {

    @Autowired
    private TagService tagService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<TagEntity> list()  {
        return tagService.findAll()
                .stream()
                .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()))
                .collect(Collectors.toList());
    }

    @GET
    @Path("{tag}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get an tag",
            notes = "User must have the MANAGEMENT_TAG permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Tag", response = TagEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_TAG, acls = RolePermissionAction.READ)
    })
    public TagEntity getTag(@PathParam("tag") String tag) {
        return tagService.findById(tag);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a sharding tag",
            notes = "User must have the MANAGEMENT_TAG permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "A new sharding tag", response = TagEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_TAG, acls = RolePermissionAction.CREATE)
    })
    public TagEntity create(@Valid @NotNull final NewTagEntity tag) {
        return tagService.create(tag);
    }

    @PUT
    @Path("{tag}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a sharding tag",
            notes = "User must have the MANAGEMENT_TAG permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Sharding tag", response = TagEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_TAG, acls = RolePermissionAction.UPDATE)
    })
    public TagEntity update(@Valid @NotNull final UpdateTagEntity tag) {
        return tagService.update(tag);
    }

    @Path("{tag}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_TAG, acls = RolePermissionAction.DELETE)
    })
    public void delete(@PathParam("tag") String tag) {
        tagService.delete(tag);
    }
}
