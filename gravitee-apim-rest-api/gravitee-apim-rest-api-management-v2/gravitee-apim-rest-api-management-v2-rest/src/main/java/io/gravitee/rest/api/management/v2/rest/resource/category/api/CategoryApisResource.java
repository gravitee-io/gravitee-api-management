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
package io.gravitee.rest.api.management.v2.rest.resource.category.api;

import io.gravitee.apim.core.category.use_case.GetCategoryApisUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.CategoryApiMapper;
import io.gravitee.rest.api.management.v2.rest.model.CategoryApi;
import io.gravitee.rest.api.management.v2.rest.model.CategoryApisResponse;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.List;

public class CategoryApisResource extends AbstractResource {

    @PathParam("categoryIdOrKey")
    String categoryIdOrKey;

    @Context
    private ResourceContext resourceContext;

    @Inject
    private GetCategoryApisUseCase getCategoryApisUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CATEGORY, acls = { RolePermissionAction.READ }) })
    public Response getCategoryApisPage(@BeanParam @Valid PaginationParam paginationParam) {
        var output = getCategoryApisUseCase.execute(
            new GetCategoryApisUseCase.Input(GraviteeContext.getExecutionContext(), categoryIdOrKey, getAuthenticatedUser(), isAdmin())
        );

        List<CategoryApi> data = output
            .results()
            .stream()
            .map(result -> CategoryApiMapper.INSTANCE.map(result.apiCategoryOrder(), result.api()))
            .toList();

        var paginationData = computePaginationData(data, paginationParam);

        return Response
            .ok()
            .entity(
                new CategoryApisResponse()
                    .data(paginationData)
                    .pagination(PaginationInfo.computePaginationInfo(output.results().size(), paginationData.size(), paginationParam))
            )
            .build();
    }

    @Path("{apiId}")
    public CategoryApiResource getCategoryApiResource() {
        return resourceContext.getResource(CategoryApiResource.class);
    }
}
