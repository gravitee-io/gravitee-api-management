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

import io.gravitee.apim.core.api_product.model.UpdateApiProduct;
import io.gravitee.apim.core.api_product.use_case.DeleteApiFromApiProductUseCase;
import io.gravitee.apim.core.api_product.use_case.DeleteApiProductUseCase;
import io.gravitee.apim.core.api_product.use_case.GetApiProductsUseCase;
import io.gravitee.apim.core.api_product.use_case.UpdateApiProductUseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApiSubscriptionsResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApiProductResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private GetApiProductsUseCase getApiProductByIdUseCase;

    @Inject
    private DeleteApiProductUseCase deleteApiProductUseCase;

    @Inject
    private UpdateApiProductUseCase updateApiProductUseCase;

    @Inject
    private DeleteApiFromApiProductUseCase deleteApiFromApiProductUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_DEFINITION, acls = { RolePermissionAction.READ }) })
    public Response getApiProductById(@PathParam("apiProductId") String apiProductId) {
        var executionContext = GraviteeContext.getExecutionContext();
        var input = GetApiProductsUseCase.Input.of(executionContext.getEnvironmentId(), apiProductId, executionContext.getOrganizationId());
        log.debug("Get API Product by id: {}", apiProductId);
        var output = getApiProductByIdUseCase.execute(input);
        var apiProductOpt = output.apiProduct();
        if (apiProductOpt.isEmpty()) {
            log.debug("API Product not found: {}", apiProductId);
            return Response.status(Response.Status.NOT_FOUND).entity("API Product not found").build();
        }
        log.debug("API Product found: {} - {}", apiProductId, apiProductOpt.get().getName());
        return Response.ok(apiProductOpt.get()).build();
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_DEFINITION, acls = { RolePermissionAction.DELETE }) })
    public Response deleteApiProductById(@PathParam("apiProductId") String apiProductId) {
        AuditInfo audit = getAuditInfo();
        deleteApiProductUseCase.execute(DeleteApiProductUseCase.Input.of(apiProductId, audit));
        return Response.noContent().build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_DEFINITION, acls = { RolePermissionAction.UPDATE }) })
    public Response updateApiProductById(
        @PathParam("apiProductId") String apiProductId,
        @Valid @NotNull UpdateApiProduct updateApiProduct
    ) {
        AuditInfo audit = getAuditInfo();
        var input = new UpdateApiProductUseCase.Input(apiProductId, updateApiProduct, audit);
        log.debug("Update API Product by id: {}", apiProductId);
        var output = updateApiProductUseCase.execute(input);
        log.debug("API Product updated: {} - {}", apiProductId, output.apiProduct().getName());
        return Response.ok(output.apiProduct()).build();
    }

    @DELETE
    @Path("/apis")
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_DEFINITION, acls = { RolePermissionAction.UPDATE }) })
    public Response deleteAllApisFromApiProduct(@PathParam("apiProductId") String apiProductId) {
        log.debug("Delete all APIs from API Product: {}", apiProductId);
        AuditInfo audit = getAuditInfo();
        var input = DeleteApiFromApiProductUseCase.Input.of(apiProductId, audit);
        deleteApiFromApiProductUseCase.execute(input);
        log.debug("All APIs removed from API Product {}", apiProductId);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/apis/{apiId}")
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_DEFINITION, acls = { RolePermissionAction.UPDATE }) })
    public Response deleteApiFromApiProduct(@PathParam("apiProductId") String apiProductId, @PathParam("apiId") String apiId) {
        log.debug("Delete API {} from API Product: {}", apiId, apiProductId);
        AuditInfo audit = getAuditInfo();
        var input = DeleteApiFromApiProductUseCase.Input.of(apiProductId, apiId, audit);
        deleteApiFromApiProductUseCase.execute(input);
        log.debug("API {} removed from API Product {}", apiId, apiProductId);
        return Response.noContent().build();
    }

    @Path("/plans")
    public ApiProductPlansResource getApiProductPlansResource() {
        return resourceContext.getResource(ApiProductPlansResource.class);
    }

    @Path("/subscriptions")
    public ApiProductSubscriptionsResource getApiProductSubscriptionsResource() {
        return resourceContext.getResource(ApiProductSubscriptionsResource.class);
    }
}
