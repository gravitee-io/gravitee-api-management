/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.common.http.MediaType.APPLICATION_JSON;

import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.portal.rest.mapper.CategoryMapper;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiCategoryService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CategoryResource extends AbstractResource {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ApiCategoryService apiCategoryService;

    @Autowired
    private CategoryMapper categoryMapper;

    @GET
    @Produces(APPLICATION_JSON)
    @RequirePortalAuth
    public Response get(@PathParam("categoryId") String categoryId) {
        CategoryEntity category = categoryService.findNotHiddenById(categoryId, GraviteeContext.getCurrentEnvironment());

        var countByCategory = apiCategoryService.countApisPublishedGroupedByCategoriesForUser(getAuthenticatedUserOrNull());
        category.setTotalApis(countByCategory.applyAsLong(category.getId()));

        return Response.ok(categoryMapper.convert(category, uriInfo.getBaseUriBuilder())).build();
    }

    @GET
    @Path("picture")
    @RequirePortalAuth
    public Response picture(@Context Request request, @PathParam("categoryId") String categoryId) {
        categoryService.findNotHiddenById(categoryId, GraviteeContext.getCurrentEnvironment());
        InlinePictureEntity image = categoryService.getPicture(GraviteeContext.getCurrentEnvironment(), categoryId);
        return createPictureResponse(request, image);
    }

    @GET
    @Path("background")
    public Response background(@Context Request request, @PathParam("categoryId") String categoryId) {
        categoryService.findNotHiddenById(categoryId, GraviteeContext.getCurrentEnvironment());
        InlinePictureEntity image = categoryService.getBackground(GraviteeContext.getCurrentEnvironment(), categoryId);
        return createPictureResponse(request, image);
    }
}
