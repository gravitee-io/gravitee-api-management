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

import static io.gravitee.common.http.MediaType.APPLICATION_JSON;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.exception.InvalidImageException;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.UpdateCategoryEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.security.utils.ImageUtils;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.CategoryNotFoundException;
import io.gravitee.rest.api.service.exceptions.UnauthorizedAccessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Categories")
public class CategoryResource extends AbstractCategoryResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryResource.class);

    @Inject
    private CategoryService categoryService;

    @PathParam("categoryId")
    @Parameter(name = "categoryId", required = true)
    private String categoryId;

    @GET
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Get the category", description = "User must have the PORTAL_CATEGORY[READ] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Category's definition",
        content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = CategoryEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public CategoryEntity getCategory() {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        boolean canShowCategory = hasPermission(
            executionContext,
            RolePermission.ENVIRONMENT_CATEGORY,
            executionContext.getEnvironmentId(),
            RolePermissionAction.READ
        );
        CategoryEntity category = categoryService.findById(categoryId, executionContext.getEnvironmentId());

        if (!canShowCategory && category.isHidden()) {
            throw new UnauthorizedAccessException();
        }

        // set picture
        setPictures(category, false);
        return category;
    }

    @GET
    @Path("picture")
    @Operation(
        summary = "Get the category's picture",
        description = "User must have the PORTAL_CATEGORY[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Category's picture",
        content = @Content(mediaType = "*/*", schema = @Schema(type = "string", format = "binary"))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response getCategoryPicture(@Context Request request) throws CategoryNotFoundException {
        return getImageResponse(
            GraviteeContext.getExecutionContext(),
            request,
            categoryService.getPicture(GraviteeContext.getCurrentEnvironment(), categoryId)
        );
    }

    @GET
    @Path("background")
    @Operation(summary = "Get the Category's background", description = "User must have the READ permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Category's background",
        content = @Content(mediaType = "*/*", schema = @Schema(type = "string", format = "binary"))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response getCategoryBackground(@Context Request request) throws CategoryNotFoundException {
        return getImageResponse(
            GraviteeContext.getExecutionContext(),
            request,
            categoryService.getBackground(GraviteeContext.getCurrentEnvironment(), categoryId)
        );
    }

    private Response getImageResponse(final ExecutionContext executionContext, Request request, InlinePictureEntity image) {
        boolean canShowCategory = hasPermission(
            executionContext,
            RolePermission.ENVIRONMENT_CATEGORY,
            executionContext.getEnvironmentId(),
            RolePermissionAction.READ
        );
        CategoryEntity category = categoryService.findById(categoryId, GraviteeContext.getCurrentEnvironment());

        if (!canShowCategory && category.isHidden()) {
            throw new UnauthorizedAccessException();
        }

        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);

        if (image == null || image.getContent() == null) {
            return Response.ok().build();
        }

        EntityTag etag = new EntityTag(Integer.toString(new String(image.getContent()).hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

        if (builder != null) {
            // Preconditions are not met, returning HTTP 304 'not-modified'
            return builder.cacheControl(cc).build();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(image.getContent(), 0, image.getContent().length);

        return Response.ok(baos).cacheControl(cc).tag(etag).type(image.getType()).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update the category", description = "User must have the PORTAL_CATEGORY[UPDATE] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Category successfully updated",
        content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = CategoryEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CATEGORY, acls = RolePermissionAction.UPDATE) })
    public Response updateCategory(@Valid @NotNull final UpdateCategoryEntity category) {
        try {
            ImageUtils.verify(category.getPicture());
            ImageUtils.verify(category.getBackground());
        } catch (InvalidImageException e) {
            LOGGER.warn("Invalid image format", e);
            throw new BadRequestException("Invalid image format : " + e.getMessage());
        }

        CategoryEntity categoryEntity = categoryService.update(GraviteeContext.getExecutionContext(), categoryId, category);
        setPictures(categoryEntity, false);

        return Response.ok(categoryEntity).build();
    }

    @DELETE
    @Operation(summary = "Delete the category", description = "User must have the PORTAL_CATEGORY[DELETE] permission to use this service")
    @ApiResponse(responseCode = "204", description = "Category successfully deleted")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CATEGORY, acls = RolePermissionAction.DELETE) })
    public void deleteCategory() {
        categoryService.delete(GraviteeContext.getExecutionContext(), categoryId);
    }
}
