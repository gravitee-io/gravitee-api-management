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
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.exceptions.PageSystemFolderActionException;
import io.swagger.annotations.*;
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
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Guillaume GILLON (guillaume.gillon@outlook.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "API Pages" })
public class ApiPagesResource extends AbstractResource {

    @Inject
    private ApiService apiService;

    @Inject
    private PageService pageService;

    @Inject
    private GroupService groupService;

    @Context
    private ResourceContext resourceContext;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @ApiParam(name = "api", hidden = true)
    private String api;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List pages", notes = "User must have the READ permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "List of pages", response = PageEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public List<PageEntity> getApiPages(
        @HeaderParam("Accept-Language") String acceptLang,
        @QueryParam("homepage") Boolean homepage,
        @QueryParam("type") PageType type,
        @QueryParam("parent") String parent,
        @QueryParam("name") String name,
        @QueryParam("root") Boolean rootParent,
        @QueryParam("translated") boolean translated
    ) {
        final String acceptedLocale = HttpHeadersUtil.getFirstAcceptedLocaleName(acceptLang);

        final ApiEntity apiEntity = apiService.findById(api);
        if (
            Visibility.PUBLIC.equals(apiEntity.getVisibility()) ||
            hasPermission(RolePermission.API_DOCUMENTATION, api, RolePermissionAction.READ)
        ) {
            return pageService
                .search(
                    new PageQuery.Builder().api(api).homepage(homepage).type(type).parent(parent).name(name).rootParent(rootParent).build(),
                    translated ? acceptedLocale : null
                )
                .stream()
                .filter(page -> isDisplayable(apiEntity, page.isPublished(), page.getExcludedGroups()))
                .collect(Collectors.toList());
        }
        throw new ForbiddenAccessException();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a page", notes = "User must have the MANAGE_PAGES permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 201, message = "Page successfully created", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.CREATE) })
    public Response createApiPage(@ApiParam(name = "page", required = true) @Valid @NotNull NewPageEntity newPageEntity) {
        if (newPageEntity.getType().equals(PageType.SYSTEM_FOLDER)) {
            throw new PageSystemFolderActionException("Create");
        }
        int order = pageService.findMaxApiPageOrderByApi(api) + 1;
        newPageEntity.setOrder(order);
        newPageEntity.setLastContributor(getAuthenticatedUser());
        PageEntity newPage = pageService.createPage(api, newPageEntity);
        if (newPage != null) {
            return Response.created(this.getLocationHeader(newPage.getId())).entity(newPage).build();
        }

        return Response.serverError().build();
    }

    @POST
    @Path("/_fetch")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Refresh all pages by calling their associated fetcher",
        notes = "User must have the MANAGE_PAGES permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 201, message = "Pages successfully refreshed", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.UPDATE) })
    public Response fetchAllApiPages() {
        String contributor = getAuthenticatedUser();
        pageService.fetchAll(new PageQuery.Builder().api(api).build(), contributor);
        return Response.noContent().build();
    }

    @Path("{page}")
    public ApiPageResource getApiPageResource() {
        return resourceContext.getResource(ApiPageResource.class);
    }

    @POST
    @Path("/_import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Import pages", notes = "User must be ADMIN to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 201, message = "Page successfully created", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.CREATE) })
    public List<PageEntity> importApiPageFiles(@ApiParam(name = "page", required = true) @Valid @NotNull ImportPageEntity pageEntity) {
        pageEntity.setLastContributor(getAuthenticatedUser());
        return pageService.importFiles(api, pageEntity);
    }

    @PUT
    @Path("/_import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Import pages", notes = "User must be ADMIN to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 201, message = "Page successfully updated", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.CREATE) })
    public List<PageEntity> updateApiPageImportFiles(
        @ApiParam(name = "page", required = true) @Valid @NotNull ImportPageEntity pageEntity
    ) {
        pageEntity.setLastContributor(getAuthenticatedUser());
        return pageService.importFiles(api, pageEntity);
    }

    private boolean isDisplayable(ApiEntity api, boolean isPagePublished, List<String> excludedGroups) {
        return (
            (isAuthenticated() && isAdmin()) ||
            (
                pageService.isDisplayable(api, isPagePublished, getAuthenticatedUserOrNull()) &&
                groupService.isUserAuthorizedToAccessApiData(api, excludedGroups, getAuthenticatedUserOrNull())
            )
        );
    }
}
