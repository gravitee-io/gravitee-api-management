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
import io.gravitee.management.model.NewTopApiEntity;
import io.gravitee.management.model.TagEntity;
import io.gravitee.management.model.TopApiEntity;
import io.gravitee.management.model.UpdateTopApiEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.TopApiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Top APIs"})
public class TopApisResource extends AbstractResource  {

    @Context
    private UriInfo uriInfo;

    @Autowired
    private TopApiService topApiService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List of top APIs",
            notes = "User must have the PORTAL_TOP_APIS[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of top APIs", response = TopApiEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.PORTAL_TOP_APIS, acls = RolePermissionAction.READ)
    })
    public List<TopApiEntity> list()  {
        return topApiService.findAll().stream()
                .peek(addPictureUrl())
                .collect(toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a top API",
            notes = "User must have the PORTAL_TOP_APIS[CREATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of top APIs", response = TopApiEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.PORTAL_TOP_APIS, acls = RolePermissionAction.CREATE)
    })
    public List<TopApiEntity> create(@Valid @NotNull final NewTopApiEntity topApi) {
        return topApiService.create(topApi).stream()
                .peek(addPictureUrl())
                .collect(toList());
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a top API",
            notes = "User must have the PORTAL_TOP_APIS[UPDATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of top APIs", response = TopApiEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.PORTAL_TOP_APIS, acls = RolePermissionAction.UPDATE)
    })
    public List<TopApiEntity> update(@Valid @NotNull final List<UpdateTopApiEntity> topApis) {
        return topApiService.update(topApis).stream()
                .peek(addPictureUrl())
                .collect(toList());
    }

    @Path("{topAPI}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete an existing top API",
            notes = "User must have the PORTAL_TOP_APIS[DELETE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Top API successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.PORTAL_TOP_APIS, acls = RolePermissionAction.DELETE)
    })
    public void delete(@PathParam("topAPI") String topAPI) {
        topApiService.delete(topAPI);
    }

    private Consumer<TopApiEntity> addPictureUrl() {
        return topApiEntity -> {
            final UriBuilder ub = uriInfo.getBaseUriBuilder();
            final UriBuilder uriBuilder = ub.path("apis").path(topApiEntity.getApi()).path("picture");
            topApiEntity.setPictureUrl(uriBuilder.build().toString());
        };
    }
}
