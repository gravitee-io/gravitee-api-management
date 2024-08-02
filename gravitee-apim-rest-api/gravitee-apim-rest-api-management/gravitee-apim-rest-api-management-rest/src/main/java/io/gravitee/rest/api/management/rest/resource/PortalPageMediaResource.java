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
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import io.gravitee.rest.api.service.exceptions.UploadUnauthorized;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 */
@Tag(name = "Portal Media")
public class PortalPageMediaResource extends AbstractResource {

    @Inject
    private MediaService mediaService;

    @Inject
    private PageService pageService;

    @PathParam("page")
    @Parameter(name = "page", required = true)
    private String page;

    @POST
    @Operation(
        summary = "Attach a media to a portal page ",
        description = "User must have the ENVIRONMENT_DOCUMENTATION[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Media successfully added",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MediaEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.UPDATE) })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response attachPortalPageMedia(
        @Context final HttpServletRequest request,
        @FormDataParam("file") InputStream uploadedInputStream,
        @FormDataParam("file") FormDataContentDisposition fileDetail,
        @FormDataParam("file") final FormDataBodyPart body,
        @FormDataParam("fileName") String fileName
    ) throws IOException {
        final PageEntity currentPage = pageService.findById(page);
        if (
            !currentPage.getReferenceType().equalsIgnoreCase(PageReferenceType.ENVIRONMENT.name()) ||
            !currentPage.getReferenceId().equalsIgnoreCase(GraviteeContext.getCurrentEnvironment())
        ) {
            throw new PageNotFoundException(page);
        }

        final String mediaId;

        if (request.getContentLength() > this.mediaService.getMediaMaxSize(GraviteeContext.getExecutionContext())) {
            throw new UploadUnauthorized(
                "Max size is " +
                this.mediaService.getMediaMaxSize(GraviteeContext.getExecutionContext()) +
                "bytes. Actual size is " +
                request.getContentLength() +
                "bytes."
            );
        }
        final String originalFileName = fileDetail.getFileName();
        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setSize(fileDetail.getSize());
        mediaEntity.setType(body.getMediaType().getType());
        mediaEntity.setSubType(body.getMediaType().getSubtype());
        mediaEntity.setData(IOUtils.toByteArray(uploadedInputStream));
        mediaEntity.setFileName(originalFileName);

        mediaId = mediaService.savePortalMedia(GraviteeContext.getExecutionContext(), mediaEntity);
        pageService
            .attachMedia(page, mediaId, fileName == null ? originalFileName : fileName)
            .ifPresent(pageEntity ->
                pageEntity
                    .getAttachedMedia()
                    .stream()
                    .filter(media -> media.getMediaHash().equals(mediaId))
                    .findFirst()
                    .ifPresent(createdMedia -> {
                        mediaEntity.setUploadDate(createdMedia.getAttachedAt());
                        mediaEntity.setHash(createdMedia.getMediaHash());
                    })
            );

        //remove data before sending entity
        mediaEntity.setData(null);
        URI location = URI.create(uriInfo.getPath());
        return Response.created(location).entity(mediaEntity).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Retrieve all media for a Portal page",
        description = "User must have the ENVIRONMENT_DOCUMENTATION[READ] permission to use this service"
    )
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.READ) })
    public Response getPortalPageMedia() {
        final PageEntity currentPage = pageService.findById(page);
        if (
            !currentPage.getReferenceType().equalsIgnoreCase(PageReferenceType.ENVIRONMENT.name()) ||
            !currentPage.getReferenceId().equalsIgnoreCase(GraviteeContext.getCurrentEnvironment())
        ) {
            throw new PageNotFoundException(page);
        }

        List<MediaEntity> pageMedia = mediaService.findAllWithoutContent(
            GraviteeContext.getExecutionContext(),
            currentPage.getAttachedMedia()
        );

        if (pageMedia != null && !pageMedia.isEmpty()) {
            return Response.ok(pageMedia).build();
        }
        return Response.noContent().build();
    }
}
