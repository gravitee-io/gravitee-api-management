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
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.NewCategoryEntity;
import io.gravitee.rest.api.model.UpdateCategoryEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Categories")
public class CategoriesResource extends AbstractCategoryResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private CategoryService categoryService;

    private static final String INCLUDE_TOTAL_APIS = "total-apis";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve list of categories")
    public List<CategoryEntity> getCategories(@QueryParam("include") List<String> include) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        boolean hasAllPermissions = hasPermission(
            executionContext,
            RolePermission.ENVIRONMENT_CATEGORY,
            RolePermissionAction.UPDATE,
            RolePermissionAction.CREATE,
            RolePermissionAction.DELETE
        );

        Stream<CategoryEntity> categoryEntityStream = categoryService
            .findAll(GraviteeContext.getCurrentEnvironment())
            .stream()
            .filter(c -> hasAllPermissions || !c.isHidden())
            .sorted(Comparator.comparingInt(CategoryEntity::getOrder))
            // set picture
            .map(c -> setPictures(c, true));

        if (include.contains(INCLUDE_TOTAL_APIS)) {
            Set<ApiEntity> apis;
            if (isAdmin()) {
                apis = apiService.findAllByEnvironment(executionContext);
            } else if (isAuthenticated()) {
                apis = apiService.findByUser(executionContext, getAuthenticatedUser(), null, true);
            } else {
                apis = apiService.findByVisibility(executionContext, Visibility.PUBLIC);
            }
            categoryEntityStream =
                categoryEntityStream.map(
                    c -> {
                        c.setTotalApis(categoryService.getTotalApisByCategory(apis, c));
                        return c;
                    }
                );
        }

        return categoryEntityStream.collect(Collectors.toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a category", description = "User must have the PORTAL_CATEGORY[CREATE] permission to use this service")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CATEGORY, acls = RolePermissionAction.CREATE) })
    public CategoryEntity createCategory(@Valid @NotNull final NewCategoryEntity category) {
        return categoryService.create(GraviteeContext.getExecutionContext(), category);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update an existing category",
        description = "User must have the PORTAL_CATEGORY[UPDATE] permission to use this service"
    )
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CATEGORY, acls = RolePermissionAction.UPDATE) })
    public List<CategoryEntity> updateCategories(@Valid @NotNull final List<UpdateCategoryEntity> categories) {
        return categoryService.update(GraviteeContext.getExecutionContext(), categories);
    }

    @Path("{categoryId}")
    public CategoryResource getCategoryResource() {
        return resourceContext.getResource(CategoryResource.class);
    }
}
