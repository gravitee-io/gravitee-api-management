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

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import io.gravitee.common.http.MediaType;
import javax.ws.rs.core.Response;

import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.NewPageEntity;
import io.gravitee.management.model.PageEntity;
import io.gravitee.management.model.PageListItem;
import io.gravitee.management.model.UpdatePageEntity;
import io.gravitee.management.model.Visibility;
import io.gravitee.management.model.permissions.ApiPermission;
import io.gravitee.management.rest.security.ApiPermissionsRequired;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.PageService;
import io.gravitee.management.model.MembershipType;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiPagesResource extends AbstractResource {

    @Inject
    private ApiService apiService;

    @Inject
    private PageService pageService;

    @PathParam("api")
    private String api;

    @GET
    @Path("/{page}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiPermissionsRequired(ApiPermission.READ)
    public PageEntity get(@PathParam("page") String page) {
        return pageService.findById(page);
    }

    @GET
    @Path("/{page}/content")
    @ApiPermissionsRequired(ApiPermission.READ)
    public Response getContent(@PathParam("page") String page) {
        PageEntity pageEntity = pageService.findById(page, true);
        return Response.ok(pageEntity.getContent(), pageEntity.getContentType()).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiPermissionsRequired(ApiPermission.READ)
    public List<PageListItem> pages() {
        final ApiEntity apiEntity = apiService.findById(api);

        final List<PageListItem> pages = pageService.findByApi(api);

        final List<PageListItem> filteredPages = pages.stream()
            .filter(page -> {
                if (isAuthenticated()) {
                    MemberEntity member = apiService.getMember(apiEntity.getId(), getAuthenticatedUsername());
                    if (member != null) {
                        return !MembershipType.USER.equals(member.getType());
                    }
                }

                if (apiEntity.getVisibility() == Visibility.PUBLIC) {
                    return page.isPublished();
                } else {
                    return false;
                }
            })
            .collect(Collectors.toList());

        return filteredPages;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiPermissionsRequired(ApiPermission.MANAGE_PAGES)
    public Response create(@Valid @NotNull NewPageEntity newPageEntity) {
        int order = pageService.findMaxPageOrderByApi(api) + 1;
        newPageEntity.setOrder(order);
        newPageEntity.setLastContributor(getAuthenticatedUsername());
        PageEntity newPage = pageService.create(api, newPageEntity);
        if (newPage != null) {
            return Response
                    .created(URI.create("/apis/" + api + "/pages/" + newPage.getId()))
                    .entity(newPage)
                    .build();
        }

        return Response.serverError().build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{page}")
    @ApiPermissionsRequired(ApiPermission.MANAGE_PAGES)
    public PageEntity update(@PathParam("page") String page, @Valid @NotNull UpdatePageEntity updatePageEntity) {
        pageService.findById(page);

        updatePageEntity.setLastContributor(getAuthenticatedUsername());
        return pageService.update(page, updatePageEntity);
    }

    @DELETE
    @Path("/{page}")
    @ApiPermissionsRequired(ApiPermission.MANAGE_PAGES)
    public void deletePage(@PathParam("page") String page) {
        pageService.findById(page);

        pageService.delete(page);
    }
}
