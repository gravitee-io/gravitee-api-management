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
package io.gravitee.rest.api.management.v2.rest.resource.api_product;

import io.gravitee.apim.core.api_product.use_case.GetApiProductApisUseCase;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiMapper;
import io.gravitee.rest.api.management.v2.rest.model.Api;
import io.gravitee.rest.api.management.v2.rest.model.ApisResponse;
import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.ApiSortByParam;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Objects;
import lombok.CustomLog;

@CustomLog
public class ApiProductApisResource extends AbstractResource {

    @PathParam("apiProductId")
    private String apiProductId;

    @Inject
    private GetApiProductApisUseCase getApiProductApisUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_DEFINITION, acls = { RolePermissionAction.READ }) })
    public Response getApiProductApis(
        @BeanParam @Valid PaginationParam paginationParam,
        @BeanParam ApiSortByParam apiSortByParam,
        @QueryParam("query") String query
    ) {
        apiSortByParam.validate();

        var executionContext = GraviteeContext.getExecutionContext();
        var input = new GetApiProductApisUseCase.Input(
            executionContext,
            apiProductId,
            query,
            paginationParam.toPageable(),
            apiSortByParam.toSortable(),
            getAuthenticatedUser(),
            isAdmin()
        );

        var output = getApiProductApisUseCase.execute(input);

        if (output.apiProduct().isEmpty()) {
            log.debug("API Product not found: {}", apiProductId);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Page<GenericApiEntity> apisPage = output.apisPage();
        if (apisPage == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(toApisResponse(apisPage, paginationParam)).build();
    }

    private ApisResponse toApisResponse(Page<GenericApiEntity> apisPage, PaginationParam paginationParam) {
        long totalCount = apisPage.getTotalElements();
        int pageItemsCount = Math.toIntExact(apisPage.getPageElements());
        List<Api> data = apisPage
            .getContent()
            .stream()
            .map(api -> ApiMapper.INSTANCE.map(api, uriInfo, (Boolean) null))
            .filter(Objects::nonNull)
            .toList();
        Pagination pagination = totalCount == 0
            ? new Pagination()
                .page(paginationParam.getPage())
                .perPage(paginationParam.getPerPage())
                .pageItemsCount(0)
                .pageCount(0)
                .totalCount(0L)
            : PaginationInfo.computePaginationInfo(totalCount, pageItemsCount, paginationParam);
        Links links = computePaginationLinks(totalCount, paginationParam);
        return new ApisResponse().data(data).pagination(pagination).links(links != null ? links : new Links().self(""));
    }
}
