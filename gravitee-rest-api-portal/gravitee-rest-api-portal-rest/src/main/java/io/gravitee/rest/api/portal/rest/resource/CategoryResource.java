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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.mapper.CategoryMapper;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import io.gravitee.rest.api.service.CategoryService;

import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import java.util.Set;

import static io.gravitee.common.http.MediaType.APPLICATION_JSON;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CategoryResource extends AbstractResource {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryMapper categoryMapper;

    @GET
    @Produces(APPLICATION_JSON)
    @RequirePortalAuth
    public Response get(@PathParam("categoryId") String categoryId) {
        CategoryEntity category = categoryService.findNotHiddenById(categoryId);

        // FIXME: retrieve all the apis of the user can be heavy because it involves a lot of data fetching. Find a way to just retrieve only necessary data.
        Set<ApiEntity> apis = apiService.findPublishedByUser(getAuthenticatedUserOrNull());
        category.setTotalApis(categoryService.getTotalApisByCategory(apis, category));

        return Response
                .ok(categoryMapper.convert(category, uriInfo.getBaseUriBuilder()))
                .build();
    }

    @GET
    @Path("picture")
    @RequirePortalAuth
    public Response picture(@Context Request request, @PathParam("categoryId") String categoryId) {
        categoryService.findNotHiddenById(categoryId);
        InlinePictureEntity image = categoryService.getPicture(categoryId);
        return createPictureResponse(request, image);
    }

    @GET
    @Path("background")
    public Response background(@Context Request request, @PathParam("categoryId") String categoryId) {
        categoryService.findNotHiddenById(categoryId);
        InlinePictureEntity image = categoryService.getBackground(categoryId);
        return createPictureResponse(request, image);
    }
}
