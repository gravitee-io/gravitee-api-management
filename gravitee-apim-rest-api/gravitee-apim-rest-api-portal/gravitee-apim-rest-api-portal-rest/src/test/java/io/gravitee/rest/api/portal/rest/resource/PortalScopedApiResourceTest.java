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

import static io.gravitee.rest.api.portal.rest.fixture.PortalNavigationFixtures.givenDefaultPortal;
import static io.gravitee.rest.api.portal.rest.resource.PortalScopedApisResourceTest.apiListing;
import static io.gravitee.rest.api.portal.rest.resource.PortalScopedNavigationItemsResourceTest.ENV_ID;
import static io.gravitee.rest.api.portal.rest.resource.PortalScopedNavigationItemsResourceTest.ORG_ID;
import static io.gravitee.rest.api.portal.rest.resource.PortalScopedNavigationItemsResourceTest.givenPortal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import fixtures.core.model.SubscriptionFormFixtures;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.SubscriptionFormQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.use_case.CreateSubscriptionUseCase;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.portal.rest.fixture.PortalNavigationFixtures;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.model.SubscriptionInput;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An API's page, an API product and subscribing, on one portal: only for what the portal lists.
 */
public class PortalScopedApiResourceTest extends AbstractResourceTest {

    private static final String OWN_API = "api-own";
    private static final String OTHER_API = "api-other";
    private static final String APPLICATION = "my-application";
    private static final String PLAN = "my-plan";

    @Autowired
    private PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService;

    @Autowired
    private PortalCrudServiceInMemory portalCrudService;

    @Autowired
    private ApiProductQueryServiceInMemory apiProductQueryService;

    @Autowired
    private SubscriptionFormQueryServiceInMemory subscriptionFormQueryService;

    private PortalId portal;
    private PortalId otherPortal;

    @Override
    protected String contextPath() {
        return "portal/";
    }

    @BeforeEach
    public void init() {
        resetAllMocks();
        GraviteeContext.setCurrentOrganization(ORG_ID);
        GraviteeContext.setCurrentEnvironment(ENV_ID);
        portal = givenPortal(portalCrudService, ENV_ID);
        otherPortal = givenPortal(portalCrudService, ENV_ID);
        portalNavigationItemsQueryService.initWith(List.of(apiListing(OWN_API, portal), apiListing(OTHER_API, otherPortal)));
        givenApis(OWN_API, OTHER_API);
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
        portalNavigationItemsQueryService.reset();
        portalCrudService.reset();
        apiProductQueryService.reset();
        subscriptionFormQueryService.reset();
    }

    @Nested
    class Api_page {

        @Test
        void should_get_an_api_the_portal_lists() {
            Response response = target(portal + "/apis/" + OWN_API).request().get();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.readEntity(Api.class).getId()).isEqualTo(OWN_API);
        }

        @Test
        void should_not_find_a_public_api_only_another_portal_lists() {
            assertThat(target(portal + "/apis/" + OTHER_API).request().get().getStatus()).isEqualTo(404);
        }

        @Test
        void should_not_find_on_the_default_portal_an_api_only_another_portal_lists() {
            var defaultPortal = givenDefaultPortal(portalCrudService, ENV_ID);

            assertThat(target(defaultPortal + "/apis/" + OTHER_API).request().get().getStatus()).isEqualTo(404);
        }

        @ParameterizedTest
        @ValueSource(strings = { "plans", "subscription-form" })
        void should_answer_the_sub_resources_of_an_api_the_portal_lists(String subPath) {
            givenTheUnscopedRulesAllow();

            assertThat(target(portal + "/apis/" + OWN_API + "/" + subPath).request().get().getStatus()).isEqualTo(200);
        }

        @ParameterizedTest
        @ValueSource(strings = { "plans", "subscription-form" })
        void should_not_find_the_sub_resources_of_an_api_another_portal_lists(String subPath) {
            givenTheUnscopedRulesAllow();
            assertThat(root().path("environments/" + ENV_ID + "/apis/" + OTHER_API + "/" + subPath).request().get().getStatus()).isEqualTo(
                200
            );

            assertThat(target(portal + "/apis/" + OTHER_API + "/" + subPath).request().get().getStatus()).isEqualTo(404);
        }

        @ParameterizedTest
        @ValueSource(strings = { "pages", "pages/a-page", "informations", "ratings", "subscribers" })
        void should_not_route_the_other_sub_resources_under_a_portal(String subPath) {
            givenTheUnscopedRulesAllow();

            assertThat(target(portal + "/apis/" + OWN_API + "/" + subPath).request().get().getStatus()).isEqualTo(404);
        }

        private void givenTheUnscopedRulesAllow() {
            doReturn(true)
                .when(accessControlService)
                .canConsumeApiFromPortal(eq(GraviteeContext.getExecutionContext()), any(GenericApiEntity.class));
            doReturn(true).when(accessControlService).canAccessApiFromPortal(eq(GraviteeContext.getExecutionContext()), any(String.class));
            doReturn(Set.of())
                .when(planSearchService)
                .findByApi(eq(GraviteeContext.getExecutionContext()), any(GenericApiEntity.class), eq(true));
            subscriptionFormQueryService.initWith(
                List.of(
                    SubscriptionFormFixtures.aSubscriptionFormBuilder()
                        .apiIds(List.of(OWN_API, OTHER_API))
                        .environmentId(ENV_ID)
                        .enabled(true)
                        .build()
                )
            );
        }

        @Test
        void should_keep_the_unscoped_rules_on_top_of_the_portals_listing() {
            doReturn(false)
                .when(accessControlService)
                .canConsumeApiFromPortal(eq(GraviteeContext.getExecutionContext()), any(GenericApiEntity.class));

            assertThat(target(portal + "/apis/" + OWN_API + "/plans").request().get().getStatus()).isEqualTo(404);
        }

        @Test
        void should_not_find_an_api_once_unpublished_from_the_portal() {
            var unpublished = apiListing(OWN_API, portal);
            unpublished.setPublished(false);
            portalNavigationItemsQueryService.initWith(List.of(unpublished));

            assertThat(target(portal + "/apis/" + OWN_API).request().get().getStatus()).isEqualTo(404);
        }

        @Test
        void should_answer_not_found_for_an_unknown_portal() {
            assertThat(target(UUID.randomUUID() + "/apis/" + OWN_API).request().get().getStatus()).isEqualTo(404);
        }

        @Test
        void should_answer_bad_request_for_a_malformed_portal_id() {
            assertThat(target("not-a-uuid/apis/" + OWN_API).request().get().getStatus()).isEqualTo(400);
        }

        @Test
        void should_keep_finding_on_the_unscoped_path_an_api_any_portal_lists() {
            Response response = root()
                .path("environments/" + ENV_ID + "/apis/" + OTHER_API)
                .queryParam("view", "documentation")
                .request()
                .get();

            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Nested
    class Api_product {

        private final String productId = UUID.randomUUID().toString();

        @BeforeEach
        void a_product_listed_on_the_other_portal() {
            apiProductQueryService.initWith(List.of(ApiProduct.builder().id(productId).environmentId(ENV_ID).name("Product").build()));
            portalNavigationItemsQueryService.initWith(List.of(productListing(productId, otherPortal)));
        }

        @Test
        void should_get_an_api_product_the_portal_lists() {
            assertThat(target(otherPortal + "/api-products/" + productId).request().get().getStatus()).isEqualTo(200);
            assertThat(target(otherPortal + "/api-products/" + productId + "/plans").request().get().getStatus()).isEqualTo(200);
        }

        @Test
        void should_not_find_an_api_product_only_another_portal_lists() {
            assertThat(target(portal + "/api-products/" + productId).request().get().getStatus()).isEqualTo(404);
            assertThat(target(portal + "/api-products/" + productId + "/plans").request().get().getStatus()).isEqualTo(404);
        }

        @Test
        void should_refuse_subscribing_to_an_api_product_only_another_portal_lists() {
            givenPlan(productId, GenericPlanEntity.ReferenceType.API_PRODUCT);

            assertThat(subscribe(portal).getStatus()).isEqualTo(404);
            verify(createSubscriptionUseCase, never()).execute(any());
        }

        @Test
        void should_subscribe_to_an_api_product_the_portal_lists() {
            givenPlan(productId, GenericPlanEntity.ReferenceType.API_PRODUCT);
            givenSubscriptionCreated(productId, "API_PRODUCT");

            assertThat(subscribe(otherPortal).getStatus()).isEqualTo(200);
        }
    }

    @Nested
    class Subscribing {

        @BeforeEach
        void allowed_by_the_unscoped_rules() {
            doReturn(true).when(permissionService).hasPermission(any(), any(), any(), any());
            doReturn(true).when(accessControlService).canConsumeApiFromPortal(eq(GraviteeContext.getExecutionContext()), any(String.class));
        }

        @Test
        void should_subscribe_to_an_api_the_portal_lists() {
            givenPlan(OWN_API, GenericPlanEntity.ReferenceType.API);
            givenSubscriptionCreated(OWN_API, "API");

            assertThat(subscribe(portal).getStatus()).isEqualTo(200);
            verify(createSubscriptionUseCase).execute(any());
        }

        @Test
        void should_refuse_subscribing_to_a_public_api_only_another_portal_lists() {
            givenPlan(OTHER_API, GenericPlanEntity.ReferenceType.API);

            assertThat(subscribe(portal).getStatus()).isEqualTo(404);
            verify(createSubscriptionUseCase, never()).execute(any());
        }

        @Test
        void should_keep_subscribing_on_the_unscoped_path_to_an_api_any_portal_lists() {
            givenPlan(OTHER_API, GenericPlanEntity.ReferenceType.API);
            givenSubscriptionCreated(OTHER_API, "API");

            Response response = root()
                .path("environments/" + ENV_ID + "/subscriptions")
                .request()
                .post(Entity.json(new SubscriptionInput().application(APPLICATION).plan(PLAN)));

            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    private Response subscribe(PortalId portalId) {
        doReturn(true).when(permissionService).hasPermission(any(), any(), any(), any());
        return target(portalId + "/subscriptions").request().post(Entity.json(new SubscriptionInput().application(APPLICATION).plan(PLAN)));
    }

    private void givenApis(String... apiIds) {
        for (var apiId : apiIds) {
            var api = new ApiEntity();
            api.setId(apiId);
            api.setVisibility(Visibility.PUBLIC);
            doReturn(api).when(apiSearchService).findGenericById(GraviteeContext.getExecutionContext(), apiId, false, false, true);
            doReturn(api).when(apiSearchService).findGenericById(GraviteeContext.getExecutionContext(), apiId, false, false, false);
            var mapped = new Api();
            mapped.setId(apiId);
            doReturn(mapped).when(apiMapper).convert(GraviteeContext.getExecutionContext(), api);
        }
    }

    private void givenPlan(String referenceId, GenericPlanEntity.ReferenceType referenceType) {
        var plan = new PlanEntity();
        plan.setReferenceId(referenceId);
        plan.setReferenceType(referenceType);
        doReturn(plan).when(planSearchService).findById(GraviteeContext.getExecutionContext(), PLAN);
    }

    private void givenSubscriptionCreated(String referenceId, String referenceType) {
        doReturn(new CreateSubscriptionUseCase.Output(SubscriptionEntity.builder().id("subscription").build()))
            .when(createSubscriptionUseCase)
            .execute(any(CreateSubscriptionUseCase.Input.class));
        var created = new io.gravitee.rest.api.model.SubscriptionEntity();
        created.setId("subscription");
        created.setStatus(SubscriptionStatus.ACCEPTED);
        created.setReferenceId(referenceId);
        created.setReferenceType(referenceType);
        created.setApplication(APPLICATION);
        doReturn(created).when(subscriptionService).findById("subscription");
    }

    private static PortalNavigationApiProduct productListing(String apiProductId, PortalId portalId) {
        var listing = PortalNavigationApiProduct.builder()
            .id(PortalNavigationFixtures.randomNavigationId())
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title("Product")
            .segment("product")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .apiProductId(apiProductId)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .reference(new NavigationItemReference.PortalReference(portalId))
            .build();
        listing.markAsRoot();
        return listing;
    }
}
