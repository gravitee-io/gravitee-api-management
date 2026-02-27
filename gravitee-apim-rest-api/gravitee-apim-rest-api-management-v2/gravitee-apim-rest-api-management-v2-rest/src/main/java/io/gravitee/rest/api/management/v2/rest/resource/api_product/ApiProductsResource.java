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

import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.CreateApiProduct;
import io.gravitee.apim.core.api_product.use_case.CreateApiProductUseCase;
import io.gravitee.apim.core.api_product.use_case.GetApiProductsUseCase;
import io.gravitee.apim.core.api_product.use_case.SearchApiProductsUseCase;
import io.gravitee.apim.core.api_product.use_case.VerifyApiProductNameUseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiProductMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiProductSearchQuery;
import io.gravitee.rest.api.management.v2.rest.model.VerifyApiProduct;
import io.gravitee.rest.api.management.v2.rest.model.VerifyApiProductResponse;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.ApiProductSortByParam;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;

@CustomLog
@Path("/api-products")
public class ApiProductsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private VerifyApiProductNameUseCase verifyApiProductNameUseCase;

    @Inject
    private CreateApiProductUseCase createApiProductUseCase;

    @Inject
    private GetApiProductsUseCase getApiProductsUseCase;

    @Inject
    private SearchApiProductsUseCase searchApiProductsUseCase;

    @Path("{apiProductId}")
    public ApiProductResource getApiProductResource() {
        return resourceContext.getResource(ApiProductResource.class);
    }

    @POST
    @Path("_verify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions(
        { @Permission(value = RolePermission.ENVIRONMENT_API_PRODUCT, acls = { RolePermissionAction.CREATE, RolePermissionAction.UPDATE }) }
    )
    public Response verifyApiProductName(@Valid @NotNull final VerifyApiProduct verifyApiProduct) {
        var executionContext = GraviteeContext.getExecutionContext();
        try {
            log.debug(
                "Verifying API Product name [{}] for environment [{}]",
                verifyApiProduct.getName(),
                executionContext.getEnvironmentId()
            );
            var input = VerifyApiProductNameUseCase.Input.of(
                executionContext.getEnvironmentId(),
                verifyApiProduct.getName(),
                verifyApiProduct.getApiProductId()
            );
            verifyApiProductNameUseCase.execute(input);

            VerifyApiProductResponse response = new VerifyApiProductResponse();
            response.setOk(true);
            return Response.ok(response).build();
        } catch (Exception e) {
            log.debug("API Product name verification failed: {}", e.getMessage());
            VerifyApiProductResponse response = new VerifyApiProductResponse();
            response.setOk(false);
            response.setReason(e.getMessage());
            return Response.ok(response).build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API_PRODUCT, acls = { RolePermissionAction.CREATE }) })
    public Response createApiProduct(@Valid @NotNull final CreateApiProduct createApiProduct) throws TechnicalException {
        AuditInfo audit = getAuditInfo();
        var input = new CreateApiProductUseCase.Input(createApiProduct, audit);
        log.debug("Creating API Product [{}]", createApiProduct.getName());
        var output = createApiProductUseCase.execute(input);
        log.debug("API Product [{}] created with id [{}]", output.apiProduct().getName(), output.apiProduct().getId());
        return Response.created(uriInfo.getAbsolutePathBuilder().path(output.apiProduct().getId()).build())
            .entity(output.apiProduct())
            .build();
    }

    @POST
    @Path("_search")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API_PRODUCT, acls = { RolePermissionAction.READ }) })
    public Response searchApiProducts(
        @BeanParam @Valid PaginationParam paginationParam,
        @BeanParam ApiProductSortByParam apiProductSortByParam,
        @Valid @NotNull ApiProductSearchQuery searchQuery
    ) {
        apiProductSortByParam.validate();
        boolean hasIds = CollectionUtils.isNotEmpty(searchQuery.getIds());
        var executionContext = GraviteeContext.getExecutionContext();
        Set<String> ids = hasIds
            ? searchQuery
                .getIds()
                .stream()
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet())
            : Set.of();

        var input = SearchApiProductsUseCase.Input.of(
            executionContext.getEnvironmentId(),
            executionContext.getOrganizationId(),
            searchQuery.getQuery(),
            ids.isEmpty() ? null : ids,
            paginationParam.toPageable(),
            apiProductSortByParam.toSortable()
        );
        var output = searchApiProductsUseCase.execute(input);
        var page = output.page();

        long totalElements = page.getTotalElements();
        int pageItemsCount = Math.toIntExact(page.getPageElements());
        log.debug(
            "Search API Products for environment [{}]: {} total, page has {} items",
            executionContext.getEnvironmentId(),
            totalElements,
            pageItemsCount
        );
        return Response.ok()
            .entity(
                Map.of(
                    "data",
                    page.getContent().stream().map(ApiProductMapper.INSTANCE::map).toList(),
                    "pagination",
                    PaginationInfo.computePaginationInfo(totalElements, pageItemsCount, paginationParam),
                    "links",
                    computePaginationLinks(totalElements, paginationParam)
                )
            )
            .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API_PRODUCT, acls = { RolePermissionAction.READ }) })
    public Response getApiProducts(@BeanParam @Valid PaginationParam paginationParam) {
        var executionContext = GraviteeContext.getExecutionContext();
        var output = getApiProductsUseCase.execute(
            GetApiProductsUseCase.Input.of(executionContext.getEnvironmentId(), executionContext.getOrganizationId())
        );
        Set<ApiProduct> allApiProducts = output.apiProducts();
        List<ApiProduct> paginationApiProducts = computePaginationData(allApiProducts, paginationParam);
        log.debug("Get API Products for environment [{}]: {} found", executionContext, allApiProducts.size());
        List<io.gravitee.rest.api.management.v2.rest.model.ApiProduct> restPaginationData = paginationApiProducts
            .stream()
            .map(ApiProductMapper.INSTANCE::map)
            .toList();
        return Response.ok()
            .entity(
                Map.of(
                    "data",
                    restPaginationData,
                    "pagination",
                    PaginationInfo.computePaginationInfo(allApiProducts.size(), paginationApiProducts.size(), paginationParam),
                    "links",
                    computePaginationLinks(allApiProducts.size(), paginationParam)
                )
            )
            .build();
    }
}
