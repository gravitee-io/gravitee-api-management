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

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.portal.rest.mapper.CategoryMapper;
import io.gravitee.rest.api.portal.rest.model.Category;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiCategoryService;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CategoriesResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ApiCategoryService apiCategoryService;

    @Autowired
    private CategoryMapper categoryMapper;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getCategories(@BeanParam PaginationParam paginationParam) {
        var countByCategory = apiCategoryService.countApisPublishedGroupedByCategoriesForUser(getAuthenticatedUserOrNull());

        List<Category> categoriesList = categoryService
            .findAll(GraviteeContext.getCurrentEnvironment())
            .stream()
            .filter(c -> !c.isHidden())
            .sorted(Comparator.comparingInt(CategoryEntity::getOrder))
            .peek(c -> c.setTotalApis(countByCategory.applyAsLong(c.getId())))
            .filter(c -> c.getTotalApis() > 0)
            .map(c -> categoryMapper.convert(c, uriInfo.getBaseUriBuilder()))
            .collect(Collectors.toList());

        return createListResponse(GraviteeContext.getExecutionContext(), categoriesList, paginationParam);
    }

    @Path("{categoryId}")
    public CategoryResource getCategoryResource() {
        return resourceContext.getResource(CategoryResource.class);
    }
}
