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
import io.gravitee.management.model.permissions.ApiPermission;
import io.gravitee.management.rest.security.ApiPermissionsRequired;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.PageService;
import io.gravitee.management.service.exceptions.UnauthorizedAccessException;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.Page;
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
    private MembershipService membershipService;

    @Inject
    private PageService pageService;

    @GET
    @Path("/{page}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiPermissionsRequired(ApiPermission.READ)
    @ApiOperation(value = "Get a page",
            notes = "User must have the READ permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Page"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public PageEntity getPage(
                @PathParam("api") String api,
                @PathParam("page") String page) {
        PageEntity pageEntity = pageService.findById(page);
        final ApiEntity apiEntity = apiService.findById(api);
        if (isDisplayable(apiEntity, pageEntity.isPublished())) {
            return pageEntity;
        } else {
            throw new UnauthorizedAccessException();
        }
    }

    @GET
    @Path("/{page}/content")
    @ApiPermissionsRequired(ApiPermission.READ)
    @ApiOperation(value = "Get the page's content",
            notes = "User must have the READ permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Page's content"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response getPageContent(
            @PathParam("api") String api,
            @PathParam("page") String page) {
        PageEntity pageEntity = pageService.findById(page, true);
        final ApiEntity apiEntity = apiService.findById(api);
        if (isDisplayable(apiEntity, pageEntity.isPublished())) {
            return Response.ok(pageEntity.getContent(), pageEntity.getContentType()).build();
        } else {
            throw new UnauthorizedAccessException();
        }
    }

    @GET
    @ApiPermissionsRequired(ApiPermission.READ)
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

        final List<PageListItem> pages = pageService.findApiPagesByApiAndHomepage(api, homepage);

        return pages.stream()
            .filter(page -> this.isDisplayable(apiEntity, page.isPublished()))
            .collect(Collectors.toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiPermissionsRequired(ApiPermission.MANAGE_PAGES)
    @ApiOperation(value = "Create a page",
            notes = "User must have the MANAGE_PAGES permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Page successfully created", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response createPage(
            @PathParam("api") String api,
            @ApiParam(name = "page", required = true) @Valid @NotNull NewPageEntity newPageEntity) {
        int order = pageService.findMaxApiPageOrderByApi(api) + 1;
        newPageEntity.setOrder(order);
        newPageEntity.setLastContributor(getAuthenticatedUsername());
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
    @ApiPermissionsRequired(ApiPermission.MANAGE_PAGES)
    @ApiOperation(value = "Update a page",
            notes = "User must have the MANAGE_PAGES permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Page successfully updated", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public PageEntity updatePage(
            @PathParam("api") String api,
            @PathParam("page") String page,
            @ApiParam(name = "page", required = true) @Valid @NotNull UpdatePageEntity updatePageEntity) {
        pageService.findById(page);

        updatePageEntity.setLastContributor(getAuthenticatedUsername());
        return pageService.update(page, updatePageEntity);
    }

    @DELETE
    @Path("/{page}")
    @ApiPermissionsRequired(ApiPermission.MANAGE_PAGES)
    @ApiOperation(value = "Delete a page",
            notes = "User must have the MANAGE_PAGES permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Page successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public void deletePage(
            @PathParam("api") String api,
            @PathParam("page") String page) {
        pageService.findById(page);

        pageService.delete(page);
    }

    private boolean isDisplayable(ApiEntity apiEntity, boolean isPublished) {
        if (isAuthenticated()) {
            MemberEntity member = membershipService.getMember(MembershipReferenceType.API, apiEntity.getId(), getAuthenticatedUsername());
            if (member == null && apiEntity.getGroup() != null && apiEntity.getGroup().getId() != null) {
                member = membershipService.getMember(MembershipReferenceType.API_GROUP, apiEntity.getGroup().getId(), getAuthenticatedUsername());
            }
            if (member != null) {
                return (MembershipType.USER == member.getType() && isPublished) ||
                        MembershipType.USER != member.getType();
            }
        }

        return apiEntity.getVisibility() == Visibility.PUBLIC && isPublished;
    }
}
