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
import io.gravitee.management.model.*;
import io.gravitee.management.model.documentation.PageQuery;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.GroupService;
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
 * @author Guillaume GILLON 
 */
@Api(tags = {"Portal"})
public class PortalPagesResource extends AbstractResource {

    @Inject
    private PageService pageService;

    @Inject
    private GroupService groupService;

    @GET
    @Path("/{page}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a page",
            notes = "Every users can use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Page"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public PageEntity getPage(
                @PathParam("page") String page,
                @QueryParam("portal") boolean portal) {
        PageEntity pageEntity = pageService.findById(page);
        if (isDisplayable(pageEntity.isPublished(), pageEntity.getExcludedGroups())) {
            if (!isAuthenticated() && pageEntity.getMetadata() != null) {
                pageEntity.getMetadata().clear();
            }
            if (portal) {
                pageService.transformWithTemplate(pageEntity, null);
            }
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
        PageEntity pageEntity = pageService.findById(page);
        pageService.transformSwagger(pageEntity);
        if (isDisplayable(pageEntity.isPublished(), pageEntity.getExcludedGroups())) {
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
            @ApiResponse(code = 200, message = "List of pages", response = PageEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public List<PageEntity> listPages(
            @QueryParam("homepage") Boolean homepage,
            @QueryParam("type") PageType type,
            @QueryParam("parent") String parent,
            @QueryParam("name") String name,
            @QueryParam("root") Boolean rootParent) {
        return pageService
                .search(new PageQuery.Builder()
                        .homepage(homepage)
                        .type(type)
                        .parent(parent)
                        .name(name)
                        .rootParent(rootParent)
                        .build())
                .stream()
                .filter(page -> isDisplayable(page.isPublished(), page.getExcludedGroups()))
                .collect(Collectors.toList());
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
        newPageEntity.setLastContributor(getAuthenticatedUser());
        PageEntity newPage = pageService.createPage(newPageEntity);
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

        updatePageEntity.setLastContributor(getAuthenticatedUser());
        return pageService.update(page, updatePageEntity);
    }

    @PATCH
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
    public PageEntity partialUpdatePage(
            @PathParam("page") String page,
            @ApiParam(name = "page", required = true) @NotNull UpdatePageEntity updatePageEntity) {
        pageService.findById(page);

        updatePageEntity.setLastContributor(getAuthenticatedUser());
        return pageService.update(page, updatePageEntity, true);
    }

    @POST
    @Path("/{page}/_fetch")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Refresh page by calling the associated fetcher",
            notes = "User must have the MANAGE_PAGES permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Page successfully refreshed", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.PORTAL_DOCUMENTATION, acls = RolePermissionAction.UPDATE)
    })
    public PageEntity fetchPage(
    @PathParam("page") String page) {
            pageService.findById(page);
            String contributor = getAuthenticatedUser();

            return pageService.fetch(page, contributor);
    }

    @POST
    @Path("/_fetch")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Refresh all pages by calling their associated fetcher",
            notes = "User must have the MANAGE_PAGES permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Pages successfully refreshed", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.PORTAL_DOCUMENTATION, acls = RolePermissionAction.UPDATE)
    })
    public Response fetchAllPages() {
        String contributor = getAuthenticatedUser();
        pageService.search(new PageQuery.Builder().build())
                .stream()
                .filter(pageListItem ->
                        pageListItem.getSource() != null)
                .forEach(pageListItem ->
                        pageService.fetch(pageListItem.getId(), contributor));

        return Response.noContent().build();
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

    @POST
    @Path("/_import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Import pages",
            notes = "User must be ADMIN to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Page successfully created", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.PORTAL_DOCUMENTATION, acls = RolePermissionAction.CREATE)
    })
    public List<PageEntity> importFiles(
            @ApiParam(name = "page", required = true) @Valid @NotNull ImportPageEntity importPageEntity) {
        importPageEntity.setLastContributor(getAuthenticatedUser());
        return pageService.importFiles(importPageEntity);
    }

    @PUT
    @Path("/_import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Import pages",
            notes = "User must be ADMIN to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Page successfully updated", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.PORTAL_DOCUMENTATION, acls = RolePermissionAction.CREATE)
    })
    public List<PageEntity> updateImportFiles(
            @ApiParam(name = "page", required = true) @Valid @NotNull ImportPageEntity importPageEntity) {
        importPageEntity.setLastContributor(getAuthenticatedUser());
        return pageService.importFiles(importPageEntity);
    }

    private boolean isDisplayable(boolean isPagePublished, List<String> excludedGroups) {
        return (isAuthenticated() && isAdmin()) ||
                (isPagePublished && groupService.isUserAuthorizedToAccessPortalData(excludedGroups, getAuthenticatedUserOrNull()));
    }
}
