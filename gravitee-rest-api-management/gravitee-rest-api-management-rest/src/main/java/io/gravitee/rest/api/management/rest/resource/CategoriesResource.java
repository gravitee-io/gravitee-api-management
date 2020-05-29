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
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.service.CategoryService;
import io.swagger.annotations.Api;

import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Categories"})
public class CategoriesResource extends AbstractCategoryResource  {

    @Context
    private ResourceContext resourceContext;

    @Autowired
    private CategoryService categoryService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<CategoryEntity> list()  {
        Set<ApiEntity> apis;
        if (isAdmin()) {
            apis = apiService.findAll();
        } else if (isAuthenticated()) {
            apis = apiService.findByUser(getAuthenticatedUser(), null);
        } else {
            apis = apiService.findByVisibility(Visibility.PUBLIC);
        }

        boolean All = hasPermission(RolePermission.ENVIRONMENT_CATEGORY, RolePermissionAction.UPDATE, RolePermissionAction.CREATE, RolePermissionAction.DELETE);

        return categoryService.findAll()
                .stream()
                .filter(c -> All || !c.isHidden())
                .sorted(Comparator.comparingInt(CategoryEntity::getOrder))
                // set picture
                .map(c -> setPicture(c, true))
                .map(c -> {
                    c.setTotalApis(categoryService.getTotalApisByCategory(apis, c));
                    return c;
                })
                .collect(Collectors.toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_CATEGORY, acls = RolePermissionAction.CREATE)
    })
    public CategoryEntity create(@Valid @NotNull final NewCategoryEntity category) {
        return categoryService.create(category);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_CATEGORY, acls = RolePermissionAction.UPDATE)
    })
    public List<CategoryEntity> update(@Valid @NotNull final List<UpdateCategoryEntity> categories) {
        return categoryService.update(categories);
    }

    @Path("{id}")
    public CategoryResource getCategoryResource() {
        return resourceContext.getResource(CategoryResource.class);
    }
}
