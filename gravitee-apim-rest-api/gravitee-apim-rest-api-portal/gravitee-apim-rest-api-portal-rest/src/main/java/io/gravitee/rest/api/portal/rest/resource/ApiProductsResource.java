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

import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanSearchQueryService;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import io.gravitee.rest.api.portal.rest.model.ApiProductPlan;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only catalog of AI Products for the Developer Portal: list the AI Products and fetch one with its
 * published plans, so a portal user can discover a product and obtain a plan id to subscribe with.
 *
 * <p>Demo visibility shortcut: any authenticated portal user sees ALL {@code AI_PRODUCT} products and their
 * PUBLISHED plans (no published/visibility model, no group filtering yet — see the plan's productization note).
 *
 * @author GraviteeSource Team
 */
public class ApiProductsResource extends AbstractResource {

    private static final String AI_PRODUCT_TYPE = "AI_PRODUCT";
    private static final com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    @Inject
    private ApiProductQueryService apiProductQueryService;

    @Inject
    private PlanSearchQueryService planSearchQueryService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getApiProducts(@BeanParam PaginationParam paginationParam) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        List<io.gravitee.rest.api.portal.rest.model.ApiProduct> products = apiProductQueryService
            .findByEnvironmentId(executionContext.getEnvironmentId())
            .stream()
            .filter(p -> AI_PRODUCT_TYPE.equalsIgnoreCase(p.getType()))
            .sorted(Comparator.comparing(p -> p.getName() == null ? "" : p.getName().toLowerCase()))
            .map(this::toSummary)
            .collect(Collectors.toList());

        return createListResponse(executionContext, products, paginationParam);
    }

    @GET
    @Path("{apiProductId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getApiProduct(@PathParam("apiProductId") String apiProductId) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        var product = apiProductQueryService
            .findById(apiProductId)
            .filter(p -> AI_PRODUCT_TYPE.equalsIgnoreCase(p.getType()))
            .orElseThrow(() -> new ApiProductNotFoundException(apiProductId));

        return Response.ok(toDetail(product)).build();
    }

    private io.gravitee.rest.api.portal.rest.model.ApiProduct toSummary(io.gravitee.apim.core.api_product.model.ApiProduct product) {
        var dto = new io.gravitee.rest.api.portal.rest.model.ApiProduct();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setVersion(product.getVersion());
        enrichComponentsAndModels(dto, product);
        return dto;
    }

    /** Read each bundled LLM proxy and surface its name, context path and models for the catalog. */
    private void enrichComponentsAndModels(
        io.gravitee.rest.api.portal.rest.model.ApiProduct dto,
        io.gravitee.apim.core.api_product.model.ApiProduct product
    ) {
        if (product.getApiIds() == null || product.getApiIds().isEmpty()) {
            return;
        }
        var executionContext = io.gravitee.rest.api.service.common.GraviteeContext.getExecutionContext();
        var allModels = new java.util.LinkedHashSet<String>();
        var components = new java.util.ArrayList<io.gravitee.rest.api.portal.rest.model.ApiProductComponent>();
        for (String apiId : product.getApiIds()) {
            try {
                var api = apiSearchService.findGenericById(executionContext, apiId, false, false, false);
                var models = modelsOf(api);
                allModels.addAll(models);
                var component = new io.gravitee.rest.api.portal.rest.model.ApiProductComponent();
                component.setName(api.getName());
                component.setPath(firstPath(api));
                component.setModels(models);
                components.add(component);
            } catch (Exception ignored) {
                // best-effort — skip components we can't read
            }
        }
        dto.setModels(new java.util.ArrayList<>(allModels));
        dto.setComponents(components);
    }

    private List<String> modelsOf(io.gravitee.rest.api.model.v4.api.GenericApiEntity api) {
        var names = new java.util.LinkedHashSet<String>();
        if (api instanceof io.gravitee.rest.api.model.v4.api.ApiEntity v4 && v4.getEndpointGroups() != null) {
            for (var group : v4.getEndpointGroups()) {
                if (group.getEndpoints() == null) {
                    continue;
                }
                for (var endpoint : group.getEndpoints()) {
                    readModelNames(endpoint.getConfiguration(), names);
                }
            }
        }
        return new java.util.ArrayList<>(names);
    }

    private void readModelNames(String configuration, java.util.Set<String> out) {
        if (configuration == null || configuration.isBlank()) {
            return;
        }
        try {
            var models = OBJECT_MAPPER.readTree(configuration).get("models");
            if (models != null && models.isArray()) {
                models.forEach(m -> {
                    var name = m.get("name");
                    if (name != null && !name.asText().isBlank()) {
                        out.add(name.asText());
                    }
                });
            }
        } catch (Exception ignored) {
            // configuration not parseable / no models — ignore
        }
    }

    private String firstPath(io.gravitee.rest.api.model.v4.api.GenericApiEntity api) {
        if (api instanceof io.gravitee.rest.api.model.v4.api.ApiEntity v4 && v4.getListeners() != null) {
            for (var listener : v4.getListeners()) {
                if (listener instanceof io.gravitee.definition.model.v4.listener.http.HttpListener http && http.getPaths() != null) {
                    return http
                        .getPaths()
                        .stream()
                        .findFirst()
                        .map(p -> p.getPath())
                        .orElse(null);
                }
            }
        }
        return null;
    }

    private io.gravitee.rest.api.portal.rest.model.ApiProduct toDetail(io.gravitee.apim.core.api_product.model.ApiProduct product) {
        final String user = getAuthenticatedUserOrNull();
        // isAdmin=true is the demo visibility shortcut so any authenticated user sees the PUBLISHED plans.
        List<ApiProductPlan> plans = planSearchQueryService
            // Pass an empty PlanQuery (not null) — searchPlans dereferences it.
            .searchPlans(product.getId(), GenericPlanEntity.ReferenceType.API_PRODUCT, PlanQuery.builder().build(), user, true)
            .stream()
            .filter(plan -> PlanStatus.PUBLISHED.equals(plan.getPlanStatus()))
            .sorted(Comparator.comparingInt(Plan::getOrder))
            .map(this::toPlan)
            .collect(Collectors.toList());

        var dto = toSummary(product);
        dto.setPlans(plans);
        return dto;
    }

    private ApiProductPlan toPlan(Plan plan) {
        var dto = new ApiProductPlan();
        dto.setId(plan.getId());
        dto.setName(plan.getName());
        if (plan.getPlanSecurity() != null) {
            dto.setSecurity(plan.getPlanSecurity().getType());
        }
        if (plan.getPlanValidation() != null) {
            dto.setValidation(plan.getPlanValidation().name());
        }
        return dto;
    }
}
