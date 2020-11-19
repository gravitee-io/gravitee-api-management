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
import io.gravitee.rest.api.management.rest.utils.HttpHeadersUtil;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.exceptions.PageSystemFolderActionException;
import io.gravitee.rest.api.service.exceptions.UnauthorizedAccessException;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 * @author Guillaume GILLON
 */
@Api(tags = {"Portal Pages"})
public class PortalPagesResource extends AbstractResource {

    @Inject
    private PageService pageService;

    @Inject
    private GroupService groupService;

    @Inject
    private ConfigService configService;

    @GET
    @Path("/{page}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a page",
            notes = "Every users can use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Page"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public PageEntity getPortalPage(
                @HeaderParam("Accept-Language") String acceptLang,
                @PathParam("page") String page,
                @QueryParam("portal") boolean portal,
                @QueryParam("translated") boolean translated) {
        final String acceptedLocale = HttpHeadersUtil.getFirstAcceptedLocaleName(acceptLang);
        PageEntity pageEntity = pageService.findById(page, translated ? acceptedLocale : null);
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
    public Response getPortalPageContent(
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
    public List<PageEntity> getPortalPages(
            @HeaderParam("Accept-Language") String acceptLang,
            @QueryParam("homepage") Boolean homepage,
            @QueryParam("type") PageType type,
            @QueryParam("parent") String parent,
            @QueryParam("name") String name,
            @QueryParam("root") Boolean rootParent,
            @QueryParam("translated") boolean translated) {
        final String acceptedLocale = HttpHeadersUtil.getFirstAcceptedLocaleName(acceptLang);
        return pageService
                .search(new PageQuery.Builder()
                        .homepage(homepage)
                        .type(type)
                        .parent(parent)
                        .name(name)
                        .rootParent(rootParent)
                        .build()
                        , translated?acceptedLocale:null)
                .stream()
                .filter(page -> isDisplayable(page.isPublished(), page.getExcludedGroups()))
                .collect(Collectors.toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a page",
            notes = "User must have the PORTAL_DOCUMENTATION[CREATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Page successfully created", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.CREATE)
    })
    public Response createPortalPage(
            @ApiParam(name = "page", required = true) @Valid @NotNull NewPageEntity newPageEntity) {
        if (newPageEntity.getType().equals(PageType.SYSTEM_FOLDER)) {
            throw new PageSystemFolderActionException("Create");
        }
        int order = pageService.findMaxPortalPageOrder() + 1;
        newPageEntity.setOrder(order);
        newPageEntity.setLastContributor(getAuthenticatedUser());
        PageEntity newPage = pageService.createPage(newPageEntity);
        if (newPage != null) {
            return Response
                    .created(this.getLocationHeader(newPage.getId()))
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
            notes = "User must have the PORTAL_DOCUMENTATION[UPDATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Page successfully updated", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.UPDATE)
    })
    public PageEntity updatePortalPage(
            @PathParam("page") String page,
            @ApiParam(name = "page", required = true) @Valid @NotNull UpdatePageEntity updatePageEntity) {
        PageEntity existingPage = pageService.findById(page);
        if (existingPage.getType().equals(PageType.SYSTEM_FOLDER.name())) {
            throw new PageSystemFolderActionException("Update");
        }
        updatePageEntity.setLastContributor(getAuthenticatedUser());
        return pageService.update(page, updatePageEntity);
    }

    @PUT
    @Path("/{page}/content")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a page content",
            notes = "User must have the PORTAL_DOCUMENTATION[UPDATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Page content successfully updated"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.UPDATE)
    })
    public String updatePageContent(
            @PathParam("page") String page,
            @ApiParam(name = "content", required = true) @Valid @NotNull String content) {
        pageService.findById(page);

        UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setContent(content);
        PageEntity update = pageService.update(page, updatePageEntity, true);

        return update.getContent();
    }

    @PATCH
    @Path("/{page}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a page",
            notes = "User must have the PORTAL_DOCUMENTATION[UPDATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Page successfully updated", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.UPDATE)
    })
    public PageEntity partialUpdatePortalPage(
            @PathParam("page") String page,
            @ApiParam(name = "page", required = true) @NotNull UpdatePageEntity updatePageEntity) {
        PageEntity existingPage = pageService.findById(page);
        if (existingPage.getType().equals(PageType.SYSTEM_FOLDER.name())) {
            throw new PageSystemFolderActionException("Update");
        }
        updatePageEntity.setLastContributor(getAuthenticatedUser());
        return pageService.update(page, updatePageEntity, true);
    }

    @POST
    @Path("/{page}/_fetch")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Refresh page by calling the associated fetcher",
            notes = "User must have the PORTAL_DOCUMENTATION[UPDATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Page successfully refreshed", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.UPDATE)
    })
    public PageEntity fetchPortalPage(@PathParam("page") String page) {
        pageService.findById(page);
        String contributor = getAuthenticatedUser();

        return pageService.fetch(page, contributor);
    }

    @POST
    @Path("/_fetch")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Refresh all pages by calling their associated fetcher",
            notes = "User must have the PORTAL_DOCUMENTATION[UPDATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Pages successfully refreshed", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.UPDATE)
    })
    public Response fetchAllPortalPages() {
        String contributor = getAuthenticatedUser();
        pageService.fetchAll(new PageQuery.Builder().build(), contributor);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{page}")
    @ApiOperation(value = "Delete a page",
            notes = "User must have the PORTAL_DOCUMENTATION[DELETE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Page successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.DELETE)
    })
    public void deletePortalPage(
            @PathParam("page") String page) {
        PageEntity existingPage = pageService.findById(page);
        if (existingPage.getType().equals(PageType.SYSTEM_FOLDER.name())) {
            throw new PageSystemFolderActionException("Delete");
        }
        pageService.delete(page);
    }

    @POST
    @Path("/_import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Import pages",
            notes = "User must have the PORTAL_DOCUMENTATION[CREATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Page successfully created", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.CREATE)
    })
    public List<PageEntity> importPortalPageFromFiles(
            @ApiParam(name = "page", required = true) @Valid @NotNull ImportPageEntity importPageEntity) {
        importPageEntity.setLastContributor(getAuthenticatedUser());
        return pageService.importFiles(importPageEntity);
    }

    @PUT
    @Path("/_import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Import pages",
            notes = "User must have the PORTAL_DOCUMENTATION[CREATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Page successfully updated", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.CREATE)
    })
    public List<PageEntity> updateImportedPortalPageFromFiles(
            @ApiParam(name = "page", required = true) @Valid @NotNull ImportPageEntity importPageEntity) {
        importPageEntity.setLastContributor(getAuthenticatedUser());
        return pageService.importFiles(importPageEntity);
    }

    private boolean isDisplayable(boolean isPagePublished, List<String> excludedGroups) {
        if (!isAuthenticated() && configService.portalLoginForced())  {
            // if portal requires login, this endpoint should hide the api pages even PUBLIC ones
            return false;
        } else {
            return (isAuthenticated() && hasPermission(RolePermission.ENVIRONMENT_DOCUMENTATION, RolePermissionAction.UPDATE, RolePermissionAction.CREATE, RolePermissionAction.DELETE)) ||
                    (isPagePublished && groupService.isUserAuthorizedToAccessPortalData(excludedGroups, getAuthenticatedUserOrNull()));
        }
    }
}
