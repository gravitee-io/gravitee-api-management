/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.exception.InvalidImageException;
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.security.utils.ImageUtils;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.UploadUnauthorized;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Tag(name = "Portal Media")
public class PortalMediaResource extends AbstractResource {

    @Inject
    private MediaService mediaService;

    @Inject
    private PageService pageService;

    @POST
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.CREATE) })
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("text/plain")
    @Operation(
        summary = "Create a media for the portal",
        description = "User must have the PORTAL_DOCUMENTATION[CREATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Media successfully created",
        content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response uploadPortalMedia(
        @FormDataParam("file") InputStream uploadedInputStream,
        @FormDataParam("file") FormDataContentDisposition fileDetail,
        @FormDataParam("file") final FormDataBodyPart body
    ) throws IOException {
        String mediaId;

        if (fileDetail.getSize() > this.mediaService.getMediaMaxSize(GraviteeContext.getExecutionContext())) {
            throw new UploadUnauthorized("Max size achieved " + fileDetail.getSize());
        } else {
            MediaEntity mediaEntity = new MediaEntity();
            mediaEntity.setSize(fileDetail.getSize());
            mediaEntity.setType(body.getMediaType().getType());
            mediaEntity.setSubType(body.getMediaType().getSubtype());
            mediaEntity.setData(IOUtils.toByteArray(uploadedInputStream));
            mediaEntity.setFileName(fileDetail.getFileName());
            try {
                ImageUtils.verify(body.getMediaType().getType(), body.getMediaType().getSubtype(), mediaEntity.getData());
            } catch (InvalidImageException e) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid image format").build();
            }

            mediaId = mediaService.savePortalMedia(GraviteeContext.getExecutionContext(), mediaEntity);
        }

        return Response.status(200).entity(mediaId).build();
    }

    @GET
    @Path("/{hash}")
    @Operation(summary = "Retrieve a media")
    @ApiResponse(
        responseCode = "200",
        description = "A media",
        content = @Content(mediaType = "*/*", schema = @Schema(type = "string", format = "binary"))
    )
    @ApiResponse(responseCode = "404", description = "Not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response getPortalMedia(@Context Request request, @PathParam("hash") String hash) {
        MediaEntity mediaEntity = mediaService.findByHash(GraviteeContext.getExecutionContext(), hash);

        if (mediaEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);

        EntityTag etag = new EntityTag(hash);
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

        if (builder != null) {
            // Preconditions are not met, returning HTTP 304 'not-modified'
            return builder.cacheControl(cc).build();
        }

        return Response.ok(mediaEntity.getData()).type(mediaEntity.getMimeType()).cacheControl(cc).tag(etag).build();
    }

    @DELETE
    @Path("/{hash}")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.DELETE) })
    @Operation(
        summary = "Delete a portal media",
        description = "User must have the PORTAL_DOCUMENTATION[DELETE] permission to use this service"
    )
    @ApiResponse(responseCode = "204", description = "Media successfully deleted")
    @ApiResponse(responseCode = "400", description = "Media is attached to pages and cannot be deleted")
    @ApiResponse(responseCode = "404", description = "Media not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response deletePortalMedia(@PathParam("hash") String hash) {
        MediaEntity mediaEntity = mediaService.findByHash(GraviteeContext.getExecutionContext(), hash);

        if (mediaEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        boolean isAttached = pageService.isMediaUsedInPages(GraviteeContext.getExecutionContext(), hash);

        if (isAttached) {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity("Media is attached to pages and cannot be deleted. Please detach it from all pages first.")
                .build();
        }

        mediaService.deletePortalMediaByHash(GraviteeContext.getExecutionContext(), hash);

        return Response.noContent().build();
    }
}
