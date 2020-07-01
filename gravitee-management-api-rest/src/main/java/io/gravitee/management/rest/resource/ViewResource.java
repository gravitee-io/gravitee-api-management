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
import io.gravitee.management.model.InlinePictureEntity;
import io.gravitee.management.model.UpdateViewEntity;
import io.gravitee.management.model.ViewEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.exception.InvalidImageException;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.rest.utils.ImageUtils;
import io.gravitee.management.service.ViewService;
import io.gravitee.management.service.exceptions.UnauthorizedAccessException;
import io.gravitee.management.service.exceptions.ViewNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.ByteArrayOutputStream;

import static io.gravitee.common.http.MediaType.APPLICATION_JSON;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Views"})
public class ViewResource extends AbstractViewResource {

    @Autowired
    private ViewService viewService;

    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get the view",
            notes = "User must have the PORTAL_VIEW[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "View's definition", response = ViewEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public ViewEntity get(@PathParam("id") String viewId) {
        boolean canShowView = hasPermission(RolePermission.PORTAL_VIEW, RolePermissionAction.READ);
        ViewEntity view = viewService.findById(viewId);

        if (!canShowView && view.isHidden()) {
            throw new UnauthorizedAccessException();
        }

        // set picture
        setPicture(view, false);
        return view;
    }

    @GET
    @Path("picture")
    @ApiOperation(value = "Get the view's picture",
            notes = "User must have the PORTAL_VIEW[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "View's picture"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response picture(
            @Context Request request,
            @PathParam("id") String viewId) throws ViewNotFoundException {
        boolean canShowView = hasPermission(RolePermission.PORTAL_VIEW, RolePermissionAction.READ);
        ViewEntity view = viewService.findById(viewId);

        if (!canShowView && view.isHidden()) {
            throw new UnauthorizedAccessException();
        }

        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);

        InlinePictureEntity image = viewService.getPicture(viewId);

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
                .ok(baos)
                .cacheControl(cc)
                .tag(etag)
                .type(image.getType())
                .build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Update the view",
            notes = "User must have the PORTAL_VIEW[UPDATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "View successfully updated", response = ViewEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.PORTAL_VIEW, acls = RolePermissionAction.UPDATE)
    })
    public Response update(@PathParam("id") String viewId, @Valid @NotNull final UpdateViewEntity view) {
        try {
            ImageUtils.verify(view.getPicture());
        } catch (InvalidImageException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid image format").build();
        }

        ViewEntity viewEntity = viewService.update(viewId, view);
        setPicture(viewEntity, false);

        return Response.ok(viewEntity).build();
    }

    @DELETE
    @ApiOperation(
            value = "Delete the view",
            notes = "User must have the PORTAL_VIEW[DELETE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "View successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.PORTAL_VIEW, acls = RolePermissionAction.DELETE)
    })
    public void delete(@PathParam("id") String id) {
        viewService.delete(id);
    }

}
