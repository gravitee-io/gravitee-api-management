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
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.exceptions.UploadUnauthorized;
import io.swagger.annotations.*;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 */
@Api(tags = {"API Media"})
public class ApiPageMediaResource extends AbstractResource {
    @Inject
    private MediaService mediaService;

    @Inject
    private PageService pageService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @ApiParam(name = "api", hidden = true)
    private String api;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("page")
    @ApiParam(name = "page", hidden = true)
    private String page;

    @POST
    @ApiOperation(value = "Attach a media to an API page ",
            notes = "User must have the API_DOCUMENTATION[UPDATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Media successfully added", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.UPDATE)
    })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response attachApiPageMedia(
            @Context final HttpServletRequest request,
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail,
            @FormDataParam("file") final FormDataBodyPart body,
            @FormDataParam("fileName") final String fileName
    ) throws IOException {
        final String mediaId;

        if (request.getContentLength() > this.mediaService.getMediaMaxSize()) {
            throw new UploadUnauthorized("Max size is " + this.mediaService.getMediaMaxSize() + "bytes. Actual size is " + request.getContentLength() + "bytes.");
        }
        final String originalFileName = fileDetail.getFileName();
        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setSize(fileDetail.getSize());
        mediaEntity.setType(body.getMediaType().getType());
        mediaEntity.setSubType(body.getMediaType().getSubtype());
        mediaEntity.setData(IOUtils.toByteArray(uploadedInputStream));
        mediaEntity.setFileName(originalFileName);

        mediaId = mediaService.saveApiMedia(api, mediaEntity);
        pageService.attachMedia(page, mediaId, fileName == null ? originalFileName : fileName);

        //remove data before sending entity
        mediaEntity.setData(null);
        return Response.ok(mediaEntity).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieve all media for an API page",
            notes = "User must have the API_DOCUMENTATION[READ] permission to use this service")
    @Permissions({
            @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.READ)
    })
    public Response getApiPageMedia() {
        final PageEntity currentPage = pageService.findById(page);
        List<MediaEntity> pageMedia = mediaService.findAllWithoutContent(currentPage.getAttachedMedia(), api);

        if (pageMedia != null && !pageMedia.isEmpty()) {
            return Response
                    .ok(pageMedia)
                    .build();
        }
        return Response.noContent().build();
    }
}
