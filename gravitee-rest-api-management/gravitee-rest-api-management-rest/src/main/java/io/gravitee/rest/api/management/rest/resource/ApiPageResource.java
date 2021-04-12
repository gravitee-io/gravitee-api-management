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
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.UpdatePageEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.exceptions.PageSystemFolderActionException;
import io.gravitee.rest.api.service.exceptions.SwaggerDescriptorException;
import io.gravitee.rest.api.service.exceptions.UnauthorizedAccessException;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API Pages"})
public class ApiPageResource extends AbstractResource {

    @Inject
    private PageService pageService;

    @Inject
    private GroupService groupService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @ApiParam(name = "api", hidden = true)
    private String api;

    @PathParam("page")
    @ApiParam(name = "page", required = true)
    private String page;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a page",
            notes = "User must have the READ permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Page"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public PageEntity getApiPage(
                @HeaderParam("Accept-Language") String acceptLang,
                @QueryParam("portal") boolean portal,
                @QueryParam("translated") boolean translated) {
        final String acceptedLocale = HttpHeadersUtil.getFirstAcceptedLocaleName(acceptLang);

        final ApiEntity apiEntity = apiService.findById(api);

        if (Visibility.PUBLIC.equals(apiEntity.getVisibility())
                || hasPermission(RolePermission.API_DOCUMENTATION, api, RolePermissionAction.READ)) {

            PageEntity pageEntity = pageService.findById(page, translated?acceptedLocale:null);

            // check if the page is used as GeneralCondition by an active Plan
            // and update the PageEntity to transfer the information to the FrontEnd
            pageEntity.setGeneralConditions(pageService.isPageUsedAsGeneralConditions(pageEntity, api));

            if (portal) {
                pageService.transformSwagger(pageEntity, api);
                if (!isAuthenticated() && pageEntity.getMetadata() != null) {
                    pageEntity.getMetadata().clear();
                }
            }
            if (isDisplayable(apiEntity, pageEntity.isPublished(), pageEntity.getExcludedGroups())) {
                if (pageEntity.getContentType() != null) {
                    String content = pageEntity.getContent();
                    try {
                        pageService.validateSafeContent(pageEntity, api);
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
    @ApiOperation(value = "Get the page's content",
            notes = "User must have the READ permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Page's content"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response getApiPageContent() {
        final PageEntity pageEntity = getApiPage(null, true, false);
        return Response.ok(pageEntity.getContent(), pageEntity.getContentType()).build();
    }

    @PUT
    @Path("/content")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Put the page's content",
            notes = "User must have the MANAGE_PAGES permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Page content successfully updated"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.UPDATE)
    })
    public String updatePageContent(@ApiParam(name = "content", required = true) @Valid @NotNull String content) {
        pageService.findById(page);

        UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setContent(content);
        PageEntity update = pageService.update(page, updatePageEntity, true);

        return update.getContent();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a page",
            notes = "User must have the MANAGE_PAGES permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Page successfully updated", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.UPDATE)
    })
    public PageEntity updateApiPage(@ApiParam(name = "page", required = true) @Valid @NotNull UpdatePageEntity updatePageEntity) {
        PageEntity existingPage = pageService.findById(page);
        if(existingPage.getType().equals(PageType.SYSTEM_FOLDER.name())) {
            throw new PageSystemFolderActionException("Update");
        }

        updatePageEntity.setLastContributor(getAuthenticatedUser());
        return pageService.update(page, updatePageEntity);
    }

    @POST
    @Path("/_fetch")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Refresh page by calling the associated fetcher",
            notes = "User must have the MANAGE_PAGES permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Page successfully refreshed", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.UPDATE)
    })
    public PageEntity fetchApiPage() {
        pageService.findById(page);
        String contributor = getAuthenticatedUser();

        return pageService.fetch(page, contributor);
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a page",
            notes = "User must have the MANAGE_PAGES permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Page successfully updated", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.UPDATE)
    })
    public PageEntity partialUpdateApiPage(
            @ApiParam(name = "page") UpdatePageEntity updatePageEntity) {
        PageEntity existingPage = pageService.findById(page);
        if(existingPage.getType().equals(PageType.SYSTEM_FOLDER.name())) {
            throw new PageSystemFolderActionException("Update");
        }

        updatePageEntity.setLastContributor(getAuthenticatedUser());
        return pageService.update(page, updatePageEntity, true);
    }

    @DELETE
    @ApiOperation(value = "Delete a page",
            notes = "User must have the MANAGE_PAGES permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Page successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.DELETE)
    })
    public void deleteApiPage() {
        PageEntity existingPage = pageService.findById(page);
        if(existingPage.getType().equals(PageType.SYSTEM_FOLDER.name())) {
            throw new PageSystemFolderActionException("Delete");
        }

        pageService.delete(page);
    }

    private boolean isDisplayable(ApiEntity api, boolean isPagePublished, List<String> excludedGroups) {
        return (isAuthenticated() && isAdmin())
                ||
                ( pageService.isDisplayable(api, isPagePublished, getAuthenticatedUserOrNull()) &&
                        groupService.isUserAuthorizedToAccessApiData(api, excludedGroups, getAuthenticatedUserOrNull()));

    }

    @Path("media")
    public ApiPageMediaResource getApiPageMediaResource() {
        return resourceContext.getResource(ApiPageMediaResource.class);
    }

}
