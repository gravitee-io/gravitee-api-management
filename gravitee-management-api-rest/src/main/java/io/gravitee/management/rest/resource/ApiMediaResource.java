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
import io.gravitee.management.model.MediaEntity;
import io.gravitee.management.model.PageEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.MediaService;
import io.gravitee.management.service.exceptions.UploadUnauthorized;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * @author Guillaume Gillon
 */
@Api(tags = {"API"})
public class ApiMediaResource extends AbstractResource {
    @Inject
    private MediaService mediaService;

    @POST
    @ApiOperation(value = "Create a picture for an API",
            notes = "User must have the API_DOCUMENTATION permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Page successfully created", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.CREATE)
    })
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("text/plain")
    public Response uploadImage(
            @PathParam("api") String api,
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail,
            @FormDataParam("file") final FormDataBodyPart body
    ) throws IOException {
        final String mediaId;
        if (!body.getMediaType().getType().equals("image")) {
            throw new UploadUnauthorized("File format unauthorized " + body.getMediaType().getType()+"/"+body.getMediaType().getSubtype());
        } else if (fileDetail.getSize() > this.mediaService.getMediaMaxSize()) {
            throw new UploadUnauthorized("Max size achieved " + fileDetail.getSize());
        } else {
            checkImageContent(IOUtils.toString(uploadedInputStream, Charset.defaultCharset()));
            mediaId = mediaService.saveApiMedia(api, new MediaEntity(
                    uploadedInputStream,
                    body.getMediaType().getType(),
                    body.getMediaType().getSubtype(),
                    fileDetail.getFileName(),
                    fileDetail.getSize()
            ));
        }

        return Response.status(200).entity(mediaId).build();
    }

    @GET
    @Path("/{hash}")
    public Response getImage(
            @Context Request request,
            @PathParam("api") String api,
            @PathParam("hash") String hash) {

        MediaEntity mediaEntity = mediaService.findby(hash, api);

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
            return builder
                    .cacheControl(cc)
                    .build();
        }


        return Response
                .ok(mediaEntity.getData())
                .type(mediaEntity.getMimeType())
                .cacheControl(cc)
                .tag(etag)
                .build();
    }
}
