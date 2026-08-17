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
package io.gravitee.apim.core.subscription.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PlanFixtures;
import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiExposedEntrypointDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ExposedEntrypoint;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemVisibilityService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.subscription.model.PortalApiProductSubscriptionDetails;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetPortalApiProductSubscriptionDetailsUseCaseTest {

    private static final String ORGANIZATION_ID = PortalNavigationItemFixtures.ORG_ID;
    private static final String ENVIRONMENT_ID = PortalNavigationItemFixtures.ENV_ID;
    private static final String API_PRODUCT_ID = "00000000-0000-0000-0000-000000000101";
    private static final String API_PRODUCT_NAVIGATION_ID = "00000000-0000-0000-0000-000000000102";
    private static final String API_NAVIGATION_ID = "00000000-0000-0000-0000-000000000103";
    private static final String SECOND_API_NAVIGATION_ID = "00000000-0000-0000-0000-000000000104";
    private static final String PLAN_ID = "product-plan";
    private static final String API_ID = "api-id";

    private ApiProductQueryServiceInMemory apiProductQueryService;
    private ApiQueryServiceInMemory apiQueryService;
    private PlanCrudServiceInMemory planCrudService;
    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;

    @Mock
    private ApiExposedEntrypointDomainService apiExposedEntrypointDomainService;

    private GetPortalApiProductSubscriptionDetailsUseCase useCase;

    @BeforeEach
    void setUp() {
        apiProductQueryService = new ApiProductQueryServiceInMemory();
        apiQueryService = new ApiQueryServiceInMemory();
        planCrudService = new PlanCrudServiceInMemory();
        navigationItemsQueryService = spy(new PortalNavigationItemsQueryServiceInMemory());
        useCase = createUseCase(List.of());
    }

    private GetPortalApiProductSubscriptionDetailsUseCase createUseCase(List<PortalNavigationItemVisibilityService> visibilityServices) {
        return new GetPortalApiProductSubscriptionDetailsUseCase(
            apiProductQueryService,
            planCrudService,
            apiQueryService,
            apiExposedEntrypointDomainService,
            navigationItemsQueryService,
            visibilityServices
        );
    }

    @Test
    void should_return_product_plan_api_entrypoints_and_documentation_target() {
        var api = publishedApi();
        var productNavigation = productNavigation();
        var apiNavigation = apiNavigation(productNavigation);
        apiProductQueryService.initWith(List.of(apiProduct(Set.of(API_ID))));
        planCrudService.initWith(List.of(productPlan()));
        apiQueryService.initWith(List.of(api));
        navigationItemsQueryService.initWith(List.of(productNavigation, apiNavigation));
        when(apiExposedEntrypointDomainService.get(ORGANIZATION_ID, ENVIRONMENT_ID, api)).thenReturn(
            List.of(new ExposedEntrypoint("https://gateway.example.com/api"))
        );

        var result = useCase.execute(input()).apiProduct();

        assertThat(result.id()).isEqualTo(API_PRODUCT_ID);
        assertThat(result.name()).isEqualTo("API Product");
        assertThat(result.version()).isEqualTo("1.0");
        assertThat(result.availability()).isEqualTo(PortalApiProductSubscriptionDetails.Availability.AVAILABLE);
        assertThat(result.plan()).isNotNull();
        assertThat(result.plan().id()).isEqualTo(PLAN_ID);
        assertThat(result.plan().name()).isEqualTo("Product plan");
        assertThat(result.plan().security()).isNotBlank();
        assertThat(result.plan().mode()).isEqualTo("STANDARD");
        assertThat(result.apis())
            .singleElement()
            .satisfies(apiDetails -> {
                assertThat(apiDetails.id()).isEqualTo(API_ID);
                assertThat(apiDetails.name()).isEqualTo("API");
                assertThat(apiDetails.version()).isEqualTo("2.0");
                assertThat(apiDetails.type()).isEqualTo("PROXY");
                assertThat(apiDetails.availability()).isEqualTo(PortalApiProductSubscriptionDetails.ApiAvailability.AVAILABLE);
                assertThat(apiDetails.entrypoints()).containsExactly("https://gateway.example.com/api");
                assertThat(apiDetails.documentation()).isEqualTo(
                    new PortalApiProductSubscriptionDetails.DocumentationTarget(API_PRODUCT_NAVIGATION_ID, API_NAVIGATION_ID)
                );
            });
    }

    @Test
    void should_keep_product_details_when_navigation_is_unpublished() {
        var api = publishedApi();
        var productNavigation = productNavigation();
        productNavigation.setPublished(false);
        var apiNavigation = apiNavigation(productNavigation);
        apiProductQueryService.initWith(List.of(apiProduct(Set.of(API_ID))));
        planCrudService.initWith(List.of(productPlan()));
        apiQueryService.initWith(List.of(api));
        navigationItemsQueryService.initWith(List.of(productNavigation, apiNavigation));
        when(apiExposedEntrypointDomainService.get(ORGANIZATION_ID, ENVIRONMENT_ID, api)).thenReturn(
            List.of(new ExposedEntrypoint("https://gateway.example.com/api"))
        );

        var result = useCase.execute(input()).apiProduct();

        assertThat(result.availability()).isEqualTo(PortalApiProductSubscriptionDetails.Availability.AVAILABLE);
        assertThat(result.apis())
            .singleElement()
            .satisfies(apiDetails -> {
                assertThat(apiDetails.entrypoints()).containsExactly("https://gateway.example.com/api");
                assertThat(apiDetails.documentation()).isNull();
            });
    }

    @Test
    void should_load_shared_navigation_ancestor_once() {
        var api = publishedApi();
        var productNavigation = productNavigation();
        var firstApiNavigation = apiNavigation(productNavigation);
        var secondApiNavigation = apiNavigation(SECOND_API_NAVIGATION_ID, productNavigation);
        apiProductQueryService.initWith(List.of(apiProduct(Set.of(API_ID))));
        apiQueryService.initWith(List.of(api));
        navigationItemsQueryService.initWith(List.of(productNavigation, firstApiNavigation, secondApiNavigation));

        useCase.execute(input());

        verify(navigationItemsQueryService, times(1)).findByIdAndEnvironmentId(ENVIRONMENT_ID, productNavigation.getId());
    }

    @Test
    void should_mark_missing_api_as_unavailable_without_failing_the_product() {
        apiProductQueryService.initWith(List.of(apiProduct(Set.of(API_ID))));
        planCrudService.initWith(List.of(productPlan()));

        var result = useCase.execute(input()).apiProduct();

        assertThat(result.apis())
            .singleElement()
            .satisfies(apiDetails -> {
                assertThat(apiDetails.id()).isEqualTo(API_ID);
                assertThat(apiDetails.name()).isNull();
                assertThat(apiDetails.availability()).isEqualTo(PortalApiProductSubscriptionDetails.ApiAvailability.UNAVAILABLE);
                assertThat(apiDetails.entrypoints()).isEmpty();
                assertThat(apiDetails.documentation()).isNull();
            });
        verify(apiExposedEntrypointDomainService, never()).get(ORGANIZATION_ID, ENVIRONMENT_ID, publishedApi());
    }

    @Test
    void should_mark_unpublished_api_and_hide_its_entrypoints_and_documentation() {
        var api = publishedApi().toBuilder().apiLifecycleState(Api.ApiLifecycleState.UNPUBLISHED).build();
        var productNavigation = productNavigation();
        apiProductQueryService.initWith(List.of(apiProduct(Set.of(API_ID))));
        planCrudService.initWith(List.of(productPlan()));
        apiQueryService.initWith(List.of(api));
        navigationItemsQueryService.initWith(List.of(productNavigation, apiNavigation(productNavigation)));

        var result = useCase.execute(input()).apiProduct();

        assertThat(result.apis())
            .singleElement()
            .satisfies(apiDetails -> {
                assertThat(apiDetails.availability()).isEqualTo(PortalApiProductSubscriptionDetails.ApiAvailability.UNPUBLISHED);
                assertThat(apiDetails.entrypoints()).isEmpty();
                assertThat(apiDetails.documentation()).isNull();
            });
        verify(apiExposedEntrypointDomainService, never()).get(ORGANIZATION_ID, ENVIRONMENT_ID, api);
    }

    @Test
    void should_mark_api_with_unknown_lifecycle_as_unavailable() {
        var api = publishedApi().toBuilder().apiLifecycleState(null).build();
        var productNavigation = productNavigation();
        apiProductQueryService.initWith(List.of(apiProduct(Set.of(API_ID))));
        apiQueryService.initWith(List.of(api));
        navigationItemsQueryService.initWith(List.of(productNavigation, apiNavigation(productNavigation)));

        var result = useCase.execute(input()).apiProduct();

        assertThat(result.apis())
            .singleElement()
            .satisfies(apiDetails -> {
                assertThat(apiDetails.availability()).isEqualTo(PortalApiProductSubscriptionDetails.ApiAvailability.UNAVAILABLE);
                assertThat(apiDetails.entrypoints()).isEmpty();
                assertThat(apiDetails.documentation()).isNull();
            });
        verify(apiExposedEntrypointDomainService, never()).get(ORGANIZATION_ID, ENVIRONMENT_ID, api);
    }

    @Test
    void should_hide_documentation_target_when_api_navigation_is_not_visible() {
        var api = publishedApi();
        var productNavigation = productNavigation();
        var apiNavigation = apiNavigation(productNavigation);
        apiProductQueryService.initWith(List.of(apiProduct(Set.of(API_ID))));
        apiQueryService.initWith(List.of(api));
        navigationItemsQueryService.initWith(List.of(productNavigation, apiNavigation));
        useCase = createUseCase(List.of(hiddenApiNavigationVisibilityService()));

        var result = useCase.execute(input()).apiProduct();

        assertThat(result.apis())
            .singleElement()
            .satisfies(apiDetails -> assertThat(apiDetails.documentation()).isNull());
    }

    @Test
    void should_return_unavailable_product_when_current_product_cannot_be_resolved() {
        planCrudService.initWith(List.of(productPlan()));

        var result = useCase.execute(input()).apiProduct();

        assertThat(result.id()).isEqualTo(API_PRODUCT_ID);
        assertThat(result.name()).isNull();
        assertThat(result.availability()).isEqualTo(PortalApiProductSubscriptionDetails.Availability.UNAVAILABLE);
        assertThat(result.plan()).isNotNull();
        assertThat(result.apis()).isEmpty();
    }

    private static GetPortalApiProductSubscriptionDetailsUseCase.Input input() {
        return new GetPortalApiProductSubscriptionDetailsUseCase.Input(
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            API_PRODUCT_ID,
            PLAN_ID,
            PortalNavigationItemViewerContext.forPortal("user-id")
        );
    }

    private static ApiProduct apiProduct(Set<String> apiIds) {
        return ApiProduct.builder()
            .id(API_PRODUCT_ID)
            .environmentId(ENVIRONMENT_ID)
            .name("API Product")
            .version("1.0")
            .apiIds(apiIds)
            .build();
    }

    private static io.gravitee.apim.core.plan.model.Plan productPlan() {
        return PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .id(PLAN_ID)
            .name("Product plan")
            .referenceId(API_PRODUCT_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .build();
    }

    private static Api publishedApi() {
        return Api.builder()
            .id(API_ID)
            .environmentId(ENVIRONMENT_ID)
            .name("API")
            .version("2.0")
            .type(ApiType.PROXY)
            .apiLifecycleState(Api.ApiLifecycleState.PUBLISHED)
            .build();
    }

    private static PortalNavigationApiProduct productNavigation() {
        var item = PortalNavigationItemFixtures.anApiProduct(API_PRODUCT_NAVIGATION_ID, "API Product", null, API_PRODUCT_ID);
        item.markAsRoot();
        return item;
    }

    private static PortalNavigationApi apiNavigation(PortalNavigationApiProduct productNavigation) {
        return apiNavigation(API_NAVIGATION_ID, productNavigation);
    }

    private static PortalNavigationApi apiNavigation(String navigationId, PortalNavigationApiProduct productNavigation) {
        var item = PortalNavigationItemFixtures.anApi(navigationId, "API", productNavigation.getId(), API_ID);
        item.updateParent(productNavigation);
        return item;
    }

    private static PortalNavigationItemVisibilityService hiddenApiNavigationVisibilityService() {
        return new PortalNavigationItemVisibilityService() {
            @Override
            public boolean appliesTo(PortalNavigationItem item) {
                return item instanceof PortalNavigationApi;
            }

            @Override
            public Predicate<PortalNavigationItem> prepareVisibilityPredicate(
                String environmentId,
                PortalNavigationItemViewerContext viewerContext
            ) {
                return item -> false;
            }
        };
    }
}
