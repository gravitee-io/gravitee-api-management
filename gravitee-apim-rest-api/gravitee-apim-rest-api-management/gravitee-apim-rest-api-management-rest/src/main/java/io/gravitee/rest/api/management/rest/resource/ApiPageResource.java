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

import static java.util.Collections.singletonList;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.management.rest.utils.HttpHeadersUtil;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.UpdatePageEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.AccessControlService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "API Pages")
public class ApiPageResource extends AbstractResource {

    @Inject
    private PageService pageService;

    @Inject
    private AccessControlService accessControlService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @Parameter(name = "api", hidden = true)
    private String api;

    @PathParam("page")
    @Parameter(name = "page", required = true)
    private String page;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a page", description = "User must have the READ permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Page",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PageEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public PageEntity getApiPage(
        @HeaderParam("Accept-Language") String acceptLang,
        @QueryParam("portal") boolean portal,
        @QueryParam("translated") boolean translated
    ) {
        final String acceptedLocale = HttpHeadersUtil.getFirstAcceptedLocaleName(acceptLang);

        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final ApiEntity apiEntity = apiService.findById(executionContext, api);

        if (
            Visibility.PUBLIC.equals(apiEntity.getVisibility()) ||
            hasPermission(executionContext, RolePermission.API_DOCUMENTATION, api, RolePermissionAction.READ)
        ) {
            PageEntity pageEntity = pageService.findById(page, translated ? acceptedLocale : null);

            // check if the page is used as GeneralCondition by an active Plan
            // and update the PageEntity to transfer the information to the FrontEnd
            pageEntity.setGeneralConditions(pageService.isPageUsedAsGeneralConditions(executionContext, pageEntity, api));

            if (portal) {
                pageService.transformSwagger(executionContext, pageEntity, api);
                if (!isAuthenticated() && pageEntity.getMetadata() != null) {
                    pageEntity.getMetadata().clear();
                }
            }
            if (isDisplayable(apiEntity, pageEntity)) {
                if (pageEntity.getContentType() != null) {
                    String content = pageEntity.getContent();
                    try {
                        pageService.validateSafeContent(executionContext, pageEntity, api);
                    } catch (SwaggerDescriptorException contentException) {
                        pageEntity.setMessages(singletonList(contentException.getMessage()));
                    } finally {
                        pageEntity.setContent(content);
                    }
                }
                return pageEntity;
            } else {
                throw new UnauthorizedAccessException();
            }
        }
        throw new ForbiddenAccessException();
    }

    @GET
    @Path("/content")
    @Operation(summary = "Get the page's content", description = "User must have the READ permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Page's content",
        content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(type = "string"))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response getApiPageContent() {
        final PageEntity pageEntity = getApiPage(null, true, false);
        return Response.ok(pageEntity.getContent(), pageEntity.getContentType()).build();
    }

    @PUT
    @Path("/content")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Put the page's content", description = "User must have the MANAGE_PAGES permission to use this service")
    @ApiResponse(responseCode = "201", description = "Page content successfully updated")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.UPDATE) })
    public String updatePageContent(@Parameter(name = "content", required = true) @Valid @NotNull String content) {
        pageService.findById(page);

        UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setContent(content);
        PageEntity update = pageService.update(GraviteeContext.getExecutionContext(), page, updatePageEntity, true);

        return update.getContent();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update a page", description = "User must have the MANAGE_PAGES permission to use this service")
    @ApiResponse(
        responseCode = "201",
        description = "Page successfully updated",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PageEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.UPDATE) })
    public PageEntity updateApiPage(@Parameter(name = "page", required = true) @Valid @NotNull UpdatePageEntity updatePageEntity) {
        PageEntity existingPage = pageService.findById(page);
        if (existingPage.getType().equals(PageType.SYSTEM_FOLDER.name())) {
            throw new PageSystemFolderActionException("Update");
        } else if (existingPage.getType().equals(PageType.MARKDOWN_TEMPLATE.name())) {
            throw new PageMarkdownTemplateActionException("Update");
        }

        updatePageEntity.setLastContributor(getAuthenticatedUser());
        return pageService.update(GraviteeContext.getExecutionContext(), page, updatePageEntity);
    }

    @POST
    @Path("/_fetch")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Refresh page by calling the associated fetcher",
        description = "User must have the MANAGE_PAGES permission to use this service"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Page successfully refreshed",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PageEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.UPDATE) })
    public PageEntity fetchApiPage() {
        pageService.findById(page);
        String contributor = getAuthenticatedUser();

        return pageService.fetch(GraviteeContext.getExecutionContext(), page, contributor);
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update a page", description = "User must have the MANAGE_PAGES permission to use this service")
    @ApiResponse(
        responseCode = "201",
        description = "Page successfully updated",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PageEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.UPDATE) })
    public PageEntity partialUpdateApiPage(@Parameter(name = "page") UpdatePageEntity updatePageEntity) {
        PageEntity existingPage = pageService.findById(page);
        if (existingPage.getType().equals(PageType.SYSTEM_FOLDER.name())) {
            throw new PageSystemFolderActionException("Update");
        } else if (existingPage.getType().equals(PageType.MARKDOWN_TEMPLATE.name())) {
            throw new PageMarkdownTemplateActionException("Update");
        }

        updatePageEntity.setLastContributor(getAuthenticatedUser());
        return pageService.update(GraviteeContext.getExecutionContext(), page, updatePageEntity, true);
    }

    @DELETE
    @Operation(summary = "Delete a page", description = "User must have the MANAGE_PAGES permission to use this service")
    @ApiResponse(responseCode = "204", description = "Page successfully deleted")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.DELETE) })
    public void deleteApiPage() {
        PageEntity existingPage = pageService.findById(page);
        if (existingPage.getType().equals(PageType.SYSTEM_FOLDER.name())) {
            throw new PageSystemFolderActionException("Delete");
        } else if (existingPage.getType().equals(PageType.MARKDOWN_TEMPLATE.name())) {
            throw new PageMarkdownTemplateActionException("Delete");
        }

        pageService.delete(GraviteeContext.getExecutionContext(), page);
    }

    private boolean isDisplayable(ApiEntity api, PageEntity pageEntity) {
        return (
            (isAuthenticated() && isAdmin()) ||
            accessControlService.canAccessPageFromConsole(GraviteeContext.getExecutionContext(), api, pageEntity)
        );
    }

    @Path("media")
    public ApiPageMediaResource getApiPageMediaResource() {
        return resourceContext.getResource(ApiPageMediaResource.class);
    }
}
