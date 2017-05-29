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
import io.gravitee.management.model.NewPageEntity;
import io.gravitee.management.model.PageEntity;
import io.gravitee.management.model.PageListItem;
import io.gravitee.management.model.UpdatePageEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.PageService;
import io.gravitee.management.service.exceptions.UnauthorizedAccessException;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Portal"})
public class PortalPagesResource extends AbstractResource {

    @Inject
    private PageService pageService;

    @GET
    @Path("/{page}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a page",
            notes = "Every users can use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Page"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public PageEntity getPage(
                @PathParam("page") String page) {
        PageEntity pageEntity = pageService.findById(page);
        if (isDisplayable(pageEntity.isPublished())) {
            return pageEntity;
        } else {
            throw new UnauthorizedAccessException();
        }
    }

    @GET
    @Path("/{page}/content")
    @ApiOperation(value = "Get the page's content",
            notes = "Every users can use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Page's content"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response getPageContent(
            @PathParam("page") String page) {
        PageEntity pageEntity = pageService.findById(page, true);
        if (isDisplayable(pageEntity.isPublished())) {
            return Response.ok(pageEntity.getContent(), pageEntity.getContentType()).build();
        } else {
            throw new UnauthorizedAccessException();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List pages",
            notes = "Every users can use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of pages", response = PageListItem.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public List<PageListItem> listPages(
            @QueryParam("homepage") Boolean homepage) {
        return pageService.findPortalPagesByHomepage(homepage).
                stream().
                filter(page -> isDisplayable(page.isPublished())).
                collect(Collectors.toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a page",
            notes = "User must be ADMIN to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Page successfully created", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.PORTAL_DOCUMENTATION, acls = RolePermissionAction.CREATE)
    })
    public Response createPage(
            @ApiParam(name = "page", required = true) @Valid @NotNull NewPageEntity newPageEntity) {
        int order = pageService.findMaxPortalPageOrder() + 1;
        newPageEntity.setOrder(order);
        newPageEntity.setLastContributor(getAuthenticatedUsername());
        PageEntity newPage = pageService.createPortalPage(newPageEntity);
        if (newPage != null) {
            return Response
                    .created(URI.create("/portal/pages/" + newPage.getId()))
                    .entity(newPage)
                    .build();
        }

        return Response.serverError().build();
    }

    @PUT
    @Path("/{page}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a page",
            notes = "User must be ADMIN to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Page successfully updated", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.PORTAL_DOCUMENTATION, acls = RolePermissionAction.UPDATE)
    })
    public PageEntity updatePage(
            @PathParam("page") String page,
            @ApiParam(name = "page", required = true) @Valid @NotNull UpdatePageEntity updatePageEntity) {
        pageService.findById(page);

        updatePageEntity.setLastContributor(getAuthenticatedUsername());
        return pageService.update(page, updatePageEntity);
    }

    @DELETE
    @Path("/{page}")
    @ApiOperation(value = "Delete a page",
            notes = "User must be ADMIN to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Page successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.PORTAL_DOCUMENTATION, acls = RolePermissionAction.DELETE)
    })
    public void deletePage(
            @PathParam("page") String page) {
        pageService.findById(page);

        pageService.delete(page);
    }

    private boolean isDisplayable(boolean isPublished) {
        return isAuthenticated() && isAdmin() || isPublished;
    }
}
