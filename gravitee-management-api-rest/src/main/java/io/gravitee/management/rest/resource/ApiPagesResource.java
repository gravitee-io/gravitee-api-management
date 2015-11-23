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

import io.gravitee.management.model.NewPageEntity;
import io.gravitee.management.model.PageEntity;
import io.gravitee.management.model.UpdatePageEntity;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.PageService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Optional;

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
    @Produces(MediaType.APPLICATION_JSON)
    public List<PageEntity> pages() {
        // Check that the API exists
        apiService.findById(api);

        return pageService.findByApi(api);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPage(NewPageEntity newPageEntity) {
        int order = pageService.findMaxPageOrderByApi(newPageEntity.getApiName()) + 1;
        newPageEntity.setOrder(order);
        newPageEntity.setLastContributor(getAuthenticatedUser());
        PageEntity newPage = pageService.createPage(newPageEntity);
        if (newPage != null) {
            return Response
                    .created(URI.create("/apis/" + api + "/pages/" + newPage.getId()))
                    .entity(newPage)
                    .build();
        }

        return Response.serverError().build();
    }

    @GET
    @Path("/page}/content")
    @Produces(MediaType.APPLICATION_JSON)
    public String contentPage(@PathParam("page") String page) {
        Optional<PageEntity> optPage = pageService.findById(page);
        if (optPage != null && optPage.isPresent()) {
            return optPage.get().getContent();
        }
        return Response.status(400).toString();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{page}")
    public PageEntity update(@PathParam("page") String page, UpdatePageEntity updatePageEntity) {
        updatePageEntity.setLastContributor(getAuthenticatedUser());
        return pageService.updatePage(page, updatePageEntity);
    }

    @DELETE
    @Path("/{page}")
    public void deletePage(@PathParam("page") String page) {
        pageService.deletePage(page);
    }
}
