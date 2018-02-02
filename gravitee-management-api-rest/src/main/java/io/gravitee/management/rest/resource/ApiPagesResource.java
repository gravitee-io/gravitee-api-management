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
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.GroupService;
import io.gravitee.management.service.PageService;
import io.gravitee.management.service.exceptions.ForbiddenAccessException;
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
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API"})
public class ApiPagesResource extends AbstractResource {

    @Inject
    private ApiService apiService;

    @Inject
    private PageService pageService;

    @Inject
    private GroupService groupService;

    @GET
    @Path("/{page}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a page",
            notes = "User must have the READ permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Page"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public PageEntity getPage(
                @PathParam("api") String api,
                @PathParam("page") String page,
                @QueryParam("portal") boolean portal) {
        final ApiEntity apiEntity = apiService.findById(api);

        if (Visibility.PUBLIC.equals(apiEntity.getVisibility())
                || hasPermission(RolePermission.API_DOCUMENTATION, api, RolePermissionAction.READ)) {
            PageEntity pageEntity = pageService.findById(page, portal);
            if (isDisplayable(apiEntity, pageEntity.isPublished(), pageEntity.getExcludedGroups())) {
                return pageEntity;
            } else {
                throw new UnauthorizedAccessException();
            }
        }
        throw new ForbiddenAccessException();
    }

    @GET
    @Path("/{page}/content")
    @ApiOperation(value = "Get the page's content",
            notes = "User must have the READ permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Page's content"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response getPageContent(
            @PathParam("api") String api,
            @PathParam("page") String page) {
        final PageEntity pageEntity = getPage(api, page, true);
        return Response.ok(pageEntity.getContent(), pageEntity.getContentType()).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List pages",
            notes = "User must have the READ permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of pages", response = PageListItem.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public List<PageListItem> listPages(
            @PathParam("api") String api,
            @QueryParam("homepage") Boolean homepage) {
        final ApiEntity apiEntity = apiService.findById(api);
        if (Visibility.PUBLIC.equals(apiEntity.getVisibility())
                || hasPermission(RolePermission.API_DOCUMENTATION, api, RolePermissionAction.READ)) {
            final List<PageListItem> pages = pageService.findApiPagesByApiAndHomepage(api, homepage);

            return pages.stream()
                    .filter(page -> isDisplayable(apiEntity, page.isPublished(), page.getExcludedGroups()))
                    .collect(Collectors.toList());
        }
        throw new ForbiddenAccessException();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a page",
            notes = "User must have the MANAGE_PAGES permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Page successfully created", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.CREATE)
    })
    public Response createPage(
            @PathParam("api") String api,
            @ApiParam(name = "page", required = true) @Valid @NotNull NewPageEntity newPageEntity) {
        int order = pageService.findMaxApiPageOrderByApi(api) + 1;
        newPageEntity.setOrder(order);
        newPageEntity.setLastContributor(getAuthenticatedUser());
        PageEntity newPage = pageService.createApiPage(api, newPageEntity);
        if (newPage != null) {
            return Response
                    .created(URI.create("/apis/" + api + "/pages/" + newPage.getId()))
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
            notes = "User must have the MANAGE_PAGES permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Page successfully updated", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.UPDATE)
    })
    public PageEntity updatePage(
            @PathParam("api") String api,
            @PathParam("page") String page,
            @ApiParam(name = "page", required = true) @Valid @NotNull UpdatePageEntity updatePageEntity) {
        pageService.findById(page);

        updatePageEntity.setLastContributor(getAuthenticatedUser());
        return pageService.update(page, updatePageEntity);
    }

    @DELETE
    @Path("/{page}")
    @ApiOperation(value = "Delete a page",
            notes = "User must have the MANAGE_PAGES permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Page successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.DELETE)
    })
    public void deletePage(
            @PathParam("api") String api,
            @PathParam("page") String page) {
        pageService.findById(page);

        pageService.delete(page);
    }

    private boolean isDisplayable(ApiEntity api, boolean isPagePublished, List<String> excludedGroups) {
        return (isAuthenticated() && isAdmin())
                ||
                ( pageService.isDisplayable(api, isPagePublished, getAuthenticatedUserOrNull()) &&
                        groupService.isUserAuthorizedToAccessApiData(api, excludedGroups, getAuthenticatedUserOrNull()));

    }
}
