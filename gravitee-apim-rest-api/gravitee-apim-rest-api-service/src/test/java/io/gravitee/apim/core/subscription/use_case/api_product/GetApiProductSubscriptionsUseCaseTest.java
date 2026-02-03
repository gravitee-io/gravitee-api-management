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
package io.gravitee.apim.core.subscription.use_case.api_product;

import static fixtures.core.model.SubscriptionFixtures.aSubscription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import fixtures.core.model.PlanFixtures;
import inmemory.ApiProductCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PlanCrudServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetApiProductSubscriptionsUseCaseTest {

    private static final String API_PRODUCT_ID = "api-product-id";
    private static final String SUBSCRIPTION_ID_1 = "subscription-id-1";
    private static final String SUBSCRIPTION_ID_2 = "subscription-id-2";

    private final ApiProductCrudServiceInMemory apiProductCrudService = new ApiProductCrudServiceInMemory();
    private final SubscriptionQueryServiceInMemory subscriptionQueryService = new SubscriptionQueryServiceInMemory();
    private final PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    private GetApiProductSubscriptionsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetApiProductSubscriptionsUseCase(subscriptionQueryService, apiProductCrudService, planCrudService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(apiProductCrudService, subscriptionQueryService, planCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_return_all_subscriptions_for_api_product() {
        // Given
        var apiProduct = givenExistingApiProduct(
            ApiProduct.builder().id(API_PRODUCT_ID).name("Test API Product").environmentId("env-id").build()
        );
        givenExistingPlanWithStatus("plan-id", PlanStatus.PUBLISHED);
        var subscription1 = aSubscription()
            .toBuilder()
            .id(SUBSCRIPTION_ID_1)
            .referenceId(API_PRODUCT_ID)
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .build();
        var subscription2 = aSubscription()
            .toBuilder()
            .id(SUBSCRIPTION_ID_2)
            .referenceId(API_PRODUCT_ID)
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .build();
        givenExistingSubscriptions(List.of(subscription1, subscription2));

        // When
        var result = useCase.execute(GetApiProductSubscriptionsUseCase.Input.of(API_PRODUCT_ID));

        // Then
        assertThat(result.subscriptions())
            .hasSize(2)
            .extracting(SubscriptionEntity::getId)
            .containsExactly(SUBSCRIPTION_ID_1, SUBSCRIPTION_ID_2);
        assertThat(result.subscription()).isEmpty();
    }

    @Test
    void should_return_single_subscription_when_subscription_id_provided() {
        // Given
        var apiProduct = givenExistingApiProduct(
            ApiProduct.builder().id(API_PRODUCT_ID).name("Test API Product").environmentId("env-id").build()
        );
        var subscription1 = aSubscription()
            .toBuilder()
            .id(SUBSCRIPTION_ID_1)
            .referenceId(API_PRODUCT_ID)
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .build();
        var subscription2 = aSubscription()
            .toBuilder()
            .id(SUBSCRIPTION_ID_2)
            .referenceId(API_PRODUCT_ID)
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .build();
        givenExistingSubscriptions(List.of(subscription1, subscription2));

        // When
        var result = useCase.execute(GetApiProductSubscriptionsUseCase.Input.of(API_PRODUCT_ID, SUBSCRIPTION_ID_1));

        // Then
        assertThat(result.subscription()).isPresent();
        assertThat(result.subscription().get().getId()).isEqualTo(SUBSCRIPTION_ID_1);
        assertThat(result.subscriptions()).isNull();
    }

    @Test
    void should_return_empty_when_subscription_not_found() {
        // Given
        var apiProduct = givenExistingApiProduct(
            ApiProduct.builder().id(API_PRODUCT_ID).name("Test API Product").environmentId("env-id").build()
        );
        givenExistingSubscription(
            aSubscription()
                .toBuilder()
                .id(SUBSCRIPTION_ID_1)
                .referenceId(API_PRODUCT_ID)
                .referenceType(SubscriptionReferenceType.API_PRODUCT)
                .build()
        );

        // When
        var result = useCase.execute(GetApiProductSubscriptionsUseCase.Input.of(API_PRODUCT_ID, "unknown-subscription"));

        // Then
        assertThat(result.subscription()).isEmpty();
    }

    @Test
    void should_throw_when_api_product_not_found() {
        // When
        var throwable = catchThrowable(() -> useCase.execute(GetApiProductSubscriptionsUseCase.Input.of("unknown-api-product")));

        // Then
        assertThat(throwable).isInstanceOf(ApiProductNotFoundException.class);
    }

    @Test
    void should_only_return_subscriptions_for_specified_api_product() {
        // Given
        var apiProduct1 = ApiProduct.builder().id(API_PRODUCT_ID).name("Test API Product 1").environmentId("env-id").build();
        var apiProduct2 = ApiProduct.builder().id("other-api-product-id").name("Test API Product 2").environmentId("env-id").build();
        givenExistingApiProducts(List.of(apiProduct1, apiProduct2));
        givenExistingPlanWithStatus("plan-id", PlanStatus.PUBLISHED);
        var subscription1 = aSubscription()
            .toBuilder()
            .id(SUBSCRIPTION_ID_1)
            .referenceId(API_PRODUCT_ID)
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .build();
        var subscription2 = aSubscription()
            .toBuilder()
            .id(SUBSCRIPTION_ID_2)
            .referenceId("other-api-product-id")
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .build();
        givenExistingSubscriptions(List.of(subscription1, subscription2));

        // When
        var result = useCase.execute(GetApiProductSubscriptionsUseCase.Input.of(API_PRODUCT_ID));

        // Then
        assertThat(result.subscriptions()).hasSize(1).extracting(SubscriptionEntity::getId).containsExactly(SUBSCRIPTION_ID_1);
    }

    private ApiProduct givenExistingApiProduct(ApiProduct apiProduct) {
        apiProductCrudService.initWith(List.of(apiProduct));
        return apiProduct;
    }

    private void givenExistingApiProducts(List<ApiProduct> apiProducts) {
        apiProductCrudService.initWith(apiProducts);
    }

    private SubscriptionEntity givenExistingSubscription(SubscriptionEntity subscription) {
        subscriptionQueryService.initWith(List.of(subscription));
        return subscription;
    }

    private void givenExistingSubscriptions(List<SubscriptionEntity> subscriptions) {
        subscriptionQueryService.initWith(subscriptions);
    }

    private void givenExistingPlanWithStatus(String planId, PlanStatus status) {
        var plan = PlanFixtures.aPlanHttpV4().toBuilder().id(planId).build();
        plan.setPlanStatus(status);
        planCrudService.initWith(List.of(plan));
    }

    @Test
    void should_exclude_subscriptions_with_closed_or_deprecated_plan() {
        // Given: one subscription with published plan, one with closed plan
        givenExistingApiProduct(ApiProduct.builder().id(API_PRODUCT_ID).name("Test API Product").environmentId("env-id").build());
        givenExistingPlanWithStatus("plan-published", PlanStatus.PUBLISHED);
        givenExistingPlanWithStatus("plan-closed", PlanStatus.CLOSED);
        var subscriptionPublished = aSubscription()
            .toBuilder()
            .id(SUBSCRIPTION_ID_1)
            .referenceId(API_PRODUCT_ID)
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .planId("plan-published")
            .build();
        var subscriptionClosed = aSubscription()
            .toBuilder()
            .id(SUBSCRIPTION_ID_2)
            .referenceId(API_PRODUCT_ID)
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .planId("plan-closed")
            .build();
        givenExistingSubscriptions(List.of(subscriptionPublished, subscriptionClosed));

        // When
        var result = useCase.execute(GetApiProductSubscriptionsUseCase.Input.of(API_PRODUCT_ID));

        // Then: only subscription with published plan is returned
        assertThat(result.subscriptions()).hasSize(1).extracting(SubscriptionEntity::getId).containsExactly(SUBSCRIPTION_ID_1);
        assertThat(result.subscription()).isEmpty();
    }
}
