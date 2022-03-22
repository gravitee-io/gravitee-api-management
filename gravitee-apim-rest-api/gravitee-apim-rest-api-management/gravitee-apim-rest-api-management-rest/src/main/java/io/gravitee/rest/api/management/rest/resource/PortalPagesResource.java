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
import io.gravitee.rest.api.service.AccessControlService;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PageSystemFolderActionException;
import io.gravitee.rest.api.service.exceptions.UnauthorizedAccessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 * @author Guillaume GILLON
 */
@Tag(name = "Portal Pages")
public class PortalPagesResource extends AbstractResource {

    @Inject
    private PageService pageService;

    @Inject
    private AccessControlService accessControlService;

    @Inject
    private ConfigService configService;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Path("/{page}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a page", description = "Every users can use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Page",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PageEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public PageEntity getPortalPage(
        @HeaderParam("Accept-Language") String acceptLang,
        @PathParam("page") String page,
        @QueryParam("portal") boolean portal,
        @QueryParam("translated") boolean translated
    ) {
        final String acceptedLocale = HttpHeadersUtil.getFirstAcceptedLocaleName(acceptLang);
        PageEntity pageEntity = pageService.findById(page, translated ? acceptedLocale : null);
        if (isDisplayable(pageEntity)) {
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
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Get the page's content", description = "Every users can use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Page's content",
        content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(type = "string"))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response getPortalPageContent(@PathParam("page") String page) {
        PageEntity pageEntity = pageService.findById(page);
        if (isDisplayable(pageEntity)) {
            pageService.transformSwagger(pageEntity);
            return Response.ok(pageEntity.getContent(), pageEntity.getContentType()).build();
        } else {
            throw new UnauthorizedAccessException();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List pages", description = "Every users can use this service")
    @ApiResponse(
        responseCode = "200",
        description = "List of pages",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = PageEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public List<PageEntity> getPortalPages(
        @HeaderParam("Accept-Language") String acceptLang,
        @QueryParam("homepage") Boolean homepage,
        @QueryParam("published") Boolean published,
        @QueryParam("type") PageType type,
        @QueryParam("parent") String parent,
        @QueryParam("name") String name,
        @QueryParam("root") Boolean rootParent,
        @QueryParam("translated") boolean translated
    ) {
        final String acceptedLocale = HttpHeadersUtil.getFirstAcceptedLocaleName(acceptLang);
        return pageService
            .search(
                new PageQuery.Builder()
                    .homepage(homepage)
                    .published(published)
                    .type(type)
                    .parent(parent)
                    .name(name)
                    .rootParent(rootParent)
                    .build(),
                translated ? acceptedLocale : null,
                GraviteeContext.getCurrentEnvironment()
            )
            .stream()
            .filter(this::isDisplayable)
            .collect(Collectors.toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Create a page",
        description = "User must have the ENVIRONMENT_DOCUMENTATION[CREATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Page successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PageEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.CREATE) })
    public Response createPortalPage(@Parameter(name = "page", required = true) @Valid @NotNull NewPageEntity newPageEntity) {
        if (newPageEntity.getType().equals(PageType.SYSTEM_FOLDER)) {
            throw new PageSystemFolderActionException("Create");
        }
        int order = pageService.findMaxPortalPageOrder(GraviteeContext.getCurrentEnvironment()) + 1;
        newPageEntity.setOrder(order);
        newPageEntity.setLastContributor(getAuthenticatedUser());
        PageEntity newPage = pageService.createPage(newPageEntity, GraviteeContext.getCurrentEnvironment());
        if (newPage != null) {
            return Response.created(this.getLocationHeader(newPage.getId())).entity(newPage).build();
        }

        return Response.serverError().build();
    }

    @PUT
    @Path("/{page}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update a page",
        description = "User must have the ENVIRONMENT_DOCUMENTATION[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Page successfully updated",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PageEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.UPDATE) })
    public PageEntity updatePortalPage(
        @PathParam("page") String page,
        @Parameter(name = "page", required = true) @Valid @NotNull UpdatePageEntity updatePageEntity
    ) {
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
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(
        summary = "Update a page content",
        description = "User must have the PORTAL_DOCUMENTATION[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Page content successfully updated",
        content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(type = "string"))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.UPDATE) })
    public String updatePageContent(
        @PathParam("page") String page,
        @Parameter(name = "content", required = true) @Valid @NotNull String content
    ) {
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
    @Operation(
        summary = "Update a page",
        description = "User must have the ENVIRONMENT_DOCUMENTATION[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Page successfully updated",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PageEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.UPDATE) })
    public PageEntity partialUpdatePortalPage(
        @PathParam("page") String page,
        @Parameter(name = "page", required = true) @NotNull UpdatePageEntity updatePageEntity
    ) {
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
    @Operation(
        summary = "Refresh page by calling the associated fetcher",
        description = "User must have the ENVIRONMENT_DOCUMENTATION[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Page successfully refreshed",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PageEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.UPDATE) })
    public PageEntity fetchPortalPage(@PathParam("page") String page) {
        pageService.findById(page);
        String contributor = getAuthenticatedUser();

        return pageService.fetch(page, contributor);
    }

    @POST
    @Path("/_fetch")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Refresh all pages by calling their associated fetcher",
        description = "User must have the ENVIRONMENT_DOCUMENTATION[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Pages successfully refreshed",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PageEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.UPDATE) })
    public Response fetchAllPortalPages() {
        String contributor = getAuthenticatedUser();
        pageService.fetchAll(new PageQuery.Builder().build(), contributor, GraviteeContext.getCurrentEnvironment());
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{page}")
    @Operation(
        summary = "Delete a page",
        description = "User must have the ENVIRONMENT_DOCUMENTATION[DELETE] permission to use this service"
    )
    @ApiResponse(responseCode = "204", description = "Page successfully deleted")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.DELETE) })
    public void deletePortalPage(@PathParam("page") String page) {
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
    @Operation(
        summary = "Import pages",
        description = "User must have the ENVIRONMENT_DOCUMENTATION[CREATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Page successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PageEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.CREATE) })
    public List<PageEntity> importPortalPageFromFiles(
        @Parameter(name = "page", required = true) @Valid @NotNull ImportPageEntity importPageEntity
    ) {
        importPageEntity.setLastContributor(getAuthenticatedUser());
        return pageService.importFiles(importPageEntity, GraviteeContext.getCurrentEnvironment());
    }

    @PUT
    @Path("/_import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Import pages",
        description = "User must have the ENVIRONMENT_DOCUMENTATION[CREATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Page successfully updated",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PageEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.CREATE) })
    public List<PageEntity> updateImportedPortalPageFromFiles(
        @Parameter(name = "page", required = true) @Valid @NotNull ImportPageEntity importPageEntity
    ) {
        importPageEntity.setLastContributor(getAuthenticatedUser());
        return pageService.importFiles(importPageEntity, GraviteeContext.getCurrentEnvironment());
    }

    private boolean isDisplayable(PageEntity pageEntity) {
        if (!isAuthenticated() && configService.portalLoginForced(GraviteeContext.getCurrentEnvironment())) {
            // if portal requires login, this endpoint should hide the api pages even PUBLIC ones
            return false;
        } else if (isAuthenticated() && isAdmin()) {
            // if user is org admin
            return true;
        } else {
            return accessControlService.canAccessPageFromPortal(GraviteeContext.getCurrentEnvironment(), pageEntity);
        }
    }

    @Path("/{page}/media")
    public PortalPageMediaResource getPortalPageMediaResource() {
        return resourceContext.getResource(PortalPageMediaResource.class);
    }
}
