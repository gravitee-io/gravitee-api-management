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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import inmemory.ApiProductCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanSearchQueryService;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import io.gravitee.rest.api.portal.rest.model.ApiProductPlan;
import io.gravitee.rest.api.portal.rest.model.ApiProductPlansResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductPlansResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT_ID = "DEFAULT";
    private static final UUID API_PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

    @Autowired
    private ApiProductQueryServiceInMemory apiProductQueryService;

    @Autowired
    private ApiProductCrudServiceInMemory apiProductCrudService;

    @Autowired
    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;

    @Autowired
    private PlanSearchQueryService planSearchQueryService;

    @Override
    protected String contextPath() {
        return "api-products/";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(NotAuthenticatedAuthenticationFilter.class);
    }

    @BeforeEach
    void setUp() {
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);
        reset(planSearchQueryService);
        var apiProduct = apiProduct();
        apiProductQueryService.initWith(List.of(apiProduct));
        apiProductCrudService.initWith(List.of(apiProduct));
        navigationItemsQueryService.initWith(List.of(apiProductNavigationItem()));
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
        apiProductQueryService.reset();
        apiProductCrudService.reset();
        navigationItemsQueryService.reset();
        reset(planSearchQueryService);
    }

    @Test
    void should_return_product_plans_ordered_by_plan_order() {
        var second = plan("plan-2", "Second", 2);
        var first = plan("plan-1", "First", 1);
        givenPlans(second, first);

        var response = target(API_PRODUCT_ID + "/plans").request().get();

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        var result = response.readEntity(ApiProductPlansResponse.class);
        assertThat(result.getData()).extracting(ApiProductPlan::getId).containsExactly("plan-1", "plan-2");
    }

    @Test
    void should_exclude_restricted_plan_for_anonymous_viewer() {
        var available = plan("available", "Available", 1);
        var restricted = plan("restricted", "Restricted", 2).toBuilder().excludedGroups(List.of("excluded-group")).build();
        givenPlans(available, restricted);

        var response = target(API_PRODUCT_ID + "/plans").request().get();

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(ApiProductPlansResponse.class).getData())
            .extracting(ApiProductPlan::getId)
            .containsExactly("available");
    }

    @Test
    void should_return_empty_list_when_no_plan_is_available() {
        givenPlans();

        var response = target(API_PRODUCT_ID + "/plans").request().get();

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(ApiProductPlansResponse.class).getData()).isEmpty();
    }

    @Test
    void should_return_not_found_when_product_is_not_exposed() {
        navigationItemsQueryService.reset();
        givenPlans(plan("plan-id", "Plan", 1));

        var response = target(API_PRODUCT_ID + "/plans").request().get();

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    private void givenPlans(io.gravitee.apim.core.plan.model.Plan... plans) {
        when(
            planSearchQueryService.searchPlans(
                eq(API_PRODUCT_ID.toString()),
                eq(GenericPlanEntity.ReferenceType.API_PRODUCT),
                any(PlanQuery.class),
                isNull(),
                eq(false)
            )
        ).thenReturn(List.of(plans));
    }

    private static Plan plan(String id, String name, int order) {
        return Plan.builder()
            .id(id)
            .name(name)
            .order(order)
            .definitionVersion(DefinitionVersion.V4)
            .validation(Plan.PlanValidationType.AUTO)
            .referenceId(API_PRODUCT_ID.toString())
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .planDefinitionHttpV4(
                io.gravitee.definition.model.v4.plan.Plan.builder().mode(PlanMode.STANDARD).status(PlanStatus.PUBLISHED).build()
            )
            .build();
    }

    private static ApiProduct apiProduct() {
        return ApiProduct.builder().id(API_PRODUCT_ID.toString()).environmentId(ENVIRONMENT_ID).name("AI Workspace").build();
    }

    private static PortalNavigationApiProduct apiProductNavigationItem() {
        return PortalNavigationApiProduct.builder()
            .id(PortalNavigationItemId.random())
            .organizationId("organization-id")
            .environmentId(ENVIRONMENT_ID)
            .title("AI Workspace")
            .segment("ai-workspace")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .apiProductId(API_PRODUCT_ID.toString())
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }
}
