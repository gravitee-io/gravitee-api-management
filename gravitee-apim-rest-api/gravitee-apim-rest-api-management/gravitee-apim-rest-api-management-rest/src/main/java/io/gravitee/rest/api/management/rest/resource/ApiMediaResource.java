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
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.security.utils.ImageUtils;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.UploadUnauthorized;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

/**
 * @author Guillaume Gillon
 * @author GraviteeSource Team
 */
@Tag(name = "API Media")
public class ApiMediaResource extends AbstractResource {

    @Inject
    private MediaService mediaService;

    @PathParam("api")
    @Parameter(name = "api", required = true, description = "The ID of the API")
    private String api;

    @POST
    @Operation(
        summary = "Create a media for an API",
        description = "User must have the API_DOCUMENTATION[CREATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Media successfully created",
        content = @Content(mediaType = "text/plain", schema = @Schema(implementation = PageEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.CREATE) })
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("text/plain")
    public Response uploadApiMediaImage(
        @FormDataParam("file") InputStream uploadedInputStream,
        @FormDataParam("file") FormDataContentDisposition fileDetail,
        @FormDataParam("file") final FormDataBodyPart body
    ) throws IOException {
        final String mediaId;

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

            mediaId = mediaService.saveApiMedia(GraviteeContext.getExecutionContext(), api, mediaEntity);
        }

        return Response.status(200).entity(mediaId).build();
    }

    @GET
    @Path("/{hash}")
    @Operation(summary = "Retrieve a media for an API")
    public Response getApiMediaImage(@Context Request request, @PathParam("hash") String hash) {
        MediaEntity mediaEntity = mediaService.findByHashAndApiId(hash, api);

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
    @Operation(
        summary = "Delete media matching hash and API id",
        description = "User must have the API_DOCUMENTATION[UPDATE] permission to use this endpoint"
    )
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.UPDATE) })
    @ApiResponse(responseCode = "204", description = "Media successfully deleted")
    @ApiResponse(responseCode = "404", description = "Media not found")
    @ApiResponse(responseCode = "403", description = "Unauthorized user")
    @ApiResponse(responseCode = "500", description = "Unexpected error")
    public Response deleteApiMedia(
        @Context Request request,
        @PathParam("hash") @ApiParam(name = "hash", value = "The MD5 sum of the media") String hash
    ) {
        mediaService.deleteByHashAndApi(hash, api);
        return Response.noContent().build();
    }
}
