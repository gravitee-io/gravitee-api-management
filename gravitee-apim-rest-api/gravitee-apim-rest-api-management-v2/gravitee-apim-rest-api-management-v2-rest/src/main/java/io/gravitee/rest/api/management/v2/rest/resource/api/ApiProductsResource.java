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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import io.gravitee.apim.core.api_product.use_case.GetApiProductsByApiIdUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiProductMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiProduct;
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
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApiProductsResource extends AbstractResource {

    @Inject
    private GetApiProductsByApiIdUseCase getApiProductsByApiIdUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API_PRODUCT, acls = { RolePermissionAction.READ }) })
    public Response getApiProductsForApi(@PathParam("apiId") String apiId, @BeanParam @Valid PaginationParam paginationParam) {
        var executionContext = GraviteeContext.getExecutionContext();
        var input = GetApiProductsByApiIdUseCase.Input.of(apiId, executionContext.getOrganizationId());
        var output = getApiProductsByApiIdUseCase.execute(input);
        List<io.gravitee.rest.api.management.v2.rest.model.ApiProduct> restApiProducts = output
            .apiProducts()
            .stream()
            .map(ApiProductMapper.INSTANCE::map)
            .collect(Collectors.toList());
        List<ApiProduct> paginationApiProducts = computePaginationData(restApiProducts, paginationParam);
        return Response.ok()
            .entity(
                Map.of(
                    "data",
                    paginationApiProducts,
                    "pagination",
                    PaginationInfo.computePaginationInfo(restApiProducts.size(), paginationApiProducts.size(), paginationParam),
                    "links",
                    computePaginationLinks(restApiProducts.size(), paginationParam)
                )
            )
            .build();
    }
}
