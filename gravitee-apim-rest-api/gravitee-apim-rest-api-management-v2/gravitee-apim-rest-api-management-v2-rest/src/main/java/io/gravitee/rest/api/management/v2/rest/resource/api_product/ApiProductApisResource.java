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

import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_TYPE_VALUE;

import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.use_case.GetApiProductsUseCase;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApisResponse;
import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.ApiSortByParam;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;

/**
 * APIs sub-resource for an API Product: list/search APIs that belong to the product with Lucene-backed search and pagination.
 */
@CustomLog
public class ApiProductApisResource extends AbstractResource {

    @PathParam("apiProductId")
    private String apiProductId;

    @Inject
    private GetApiProductsUseCase getApiProductsUseCase;

    @Inject
    private ApiSearchService apiSearchService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_DEFINITION, acls = { RolePermissionAction.READ }) })
    public Response getApiProductApis(
        @BeanParam @Valid PaginationParam paginationParam,
        @BeanParam ApiSortByParam apiSortByParam,
        @QueryParam("query") String query
    ) {
        var apiProductOpt = resolveApiProduct();
        if (apiProductOpt.isEmpty()) {
            log.debug("API Product not found: {}", apiProductId);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        var apiProduct = apiProductOpt.get();
        var apiIds = Optional.ofNullable(apiProduct.getApiIds()).map(List::copyOf).orElse(List.<String>of());

        if (apiIds.isEmpty()) {
            return Response.ok(emptyApisResponse(paginationParam)).build();
        }

        apiSortByParam.validate();

        var apiQueryBuilder = QueryBuilder.create(ApiEntity.class).addFilter(FIELD_TYPE_VALUE, apiIds).setSort(apiSortByParam.toSortable());
        if (query != null && !query.isBlank()) {
            apiQueryBuilder.setQuery(query.strip());
        }

        var executionContext = GraviteeContext.getExecutionContext();
        var apis = apiSearchService.search(
            executionContext,
            getAuthenticatedUser(),
            isAdmin(),
            apiQueryBuilder,
            paginationParam.toPageable(),
            false, // mapToFullGenericApiEntity
            true // manageOnly
        );

        return Response.ok(buildApisResponse(apis, paginationParam)).build();
    }

    private Optional<ApiProduct> resolveApiProduct() {
        var executionContext = GraviteeContext.getExecutionContext();
        var input = GetApiProductsUseCase.Input.of(executionContext.getEnvironmentId(), apiProductId, executionContext.getOrganizationId());
        return getApiProductsUseCase.execute(input).apiProduct();
    }

    private ApisResponse emptyApisResponse(PaginationParam paginationParam) {
        Pagination pagination = new Pagination()
            .page(paginationParam.getPage())
            .perPage(paginationParam.getPerPage())
            .pageItemsCount(0)
            .pageCount(0)
            .totalCount(0L);
        Links links = computePaginationLinks(0, paginationParam);
        return new ApisResponse().data(List.of()).pagination(pagination).links(links != null ? links : new Links().self(""));
    }

    private ApisResponse buildApisResponse(Page<GenericApiEntity> apis, PaginationParam paginationParam) {
        var totalCount = apis.getTotalElements();
        var pageItemsCount = Math.toIntExact(apis.getPageElements());
        return new ApisResponse()
            .data(ApiMapper.INSTANCE.map(apis.getContent(), uriInfo, api -> null))
            .pagination(PaginationInfo.computePaginationInfo(totalCount, pageItemsCount, paginationParam))
            .links(computePaginationLinks(totalCount, paginationParam));
    }
}
