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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.PlanFixtures;
import fixtures.core.model.SubscriptionFixtures;
import inmemory.ApiProductCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PlanCrudServiceInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.domain_service.CreateSubscriptionDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateApiProductSubscriptionUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String API_PRODUCT_ID = "api-product-id";
    private static final String PLAN_ID = "plan-id";
    private static final String APPLICATION_ID = "application-id";
    private static final String SUBSCRIPTION_ID = "subscription-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, "user-id");

    private final ApiProductCrudServiceInMemory apiProductCrudService = new ApiProductCrudServiceInMemory();
    private final PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    private final SubscriptionCrudServiceInMemory subscriptionCrudService = new SubscriptionCrudServiceInMemory();
    private final CreateSubscriptionDomainService createSubscriptionDomainService = mock(CreateSubscriptionDomainService.class);

    private CreateApiProductSubscriptionUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
        GraviteeContext.cleanContext();
        GraviteeContext.setCurrentOrganization(ORGANIZATION_ID);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);
    }

    @AfterAll
    static void afterAll() {
        TimeProvider.reset();
        GraviteeContext.cleanContext();
    }

    @BeforeEach
    void setUp() {
        useCase = new CreateApiProductSubscriptionUseCase(
            createSubscriptionDomainService,
            subscriptionCrudService,
            planCrudService,
            apiProductCrudService
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(apiProductCrudService, planCrudService, subscriptionCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_create_subscription_for_api_product() {
        // Given
        var apiProduct = givenExistingApiProduct(
            ApiProduct.builder().id(API_PRODUCT_ID).name("Test API Product").environmentId(ENVIRONMENT_ID).build()
        );
        var plan = givenExistingPlan(
            PlanFixtures.aPlanHttpV4()
                .toBuilder()
                .id(PLAN_ID)
                .referenceId(API_PRODUCT_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .build()
        );
        plan.setPlanStatus(PlanStatus.PUBLISHED);
        // SubscriptionServiceImpl.create sets referenceId/referenceType from plan; seed storage to reflect that
        var createdSubscriptionCore = aSubscription()
            .toBuilder()
            .id(SUBSCRIPTION_ID)
            .planId(PLAN_ID)
            .applicationId(APPLICATION_ID)
            .apiId(null)
            .referenceId(API_PRODUCT_ID)
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .build();
        var createdSubscriptionLegacy = io.gravitee.rest.api.model.SubscriptionEntity.builder()
            .id(SUBSCRIPTION_ID)
            .plan(PLAN_ID)
            .application(APPLICATION_ID)
            .build();
        when(createSubscriptionDomainService.create(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(
            createdSubscriptionLegacy
        );
        subscriptionCrudService.initWith(List.of(createdSubscriptionCore));

        // When
        var result = useCase.execute(
            CreateApiProductSubscriptionUseCase.Input.builder()
                .apiProductId(API_PRODUCT_ID)
                .planId(PLAN_ID)
                .applicationId(APPLICATION_ID)
                .auditInfo(AUDIT_INFO)
                .build()
        );

        // Then
        assertThat(result.subscription())
            .extracting(SubscriptionEntity::getId, SubscriptionEntity::getReferenceId, SubscriptionEntity::getReferenceType)
            .containsExactly(SUBSCRIPTION_ID, API_PRODUCT_ID, SubscriptionReferenceType.API_PRODUCT);
        verify(createSubscriptionDomainService).create(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void should_throw_when_api_product_not_found() {
        // When
        var throwable = catchThrowable(() ->
            useCase.execute(
                CreateApiProductSubscriptionUseCase.Input.builder()
                    .apiProductId("unknown-api-product")
                    .planId(PLAN_ID)
                    .applicationId(APPLICATION_ID)
                    .auditInfo(AUDIT_INFO)
                    .build()
            )
        );

        // Then
        assertThat(throwable).isInstanceOf(ApiProductNotFoundException.class);
    }

    @Test
    void should_throw_when_plan_does_not_belong_to_api_product() {
        // Given
        var apiProduct = givenExistingApiProduct(
            ApiProduct.builder().id(API_PRODUCT_ID).name("Test API Product").environmentId(ENVIRONMENT_ID).build()
        );
        var plan = givenExistingPlan(
            PlanFixtures.aPlanHttpV4()
                .toBuilder()
                .id(PLAN_ID)
                .referenceId("other-api-product-id")
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .build()
        );
        plan.setPlanStatus(PlanStatus.PUBLISHED);

        // When
        var throwable = catchThrowable(() ->
            useCase.execute(
                CreateApiProductSubscriptionUseCase.Input.builder()
                    .apiProductId(API_PRODUCT_ID)
                    .planId(PLAN_ID)
                    .applicationId(APPLICATION_ID)
                    .auditInfo(AUDIT_INFO)
                    .build()
            )
        );

        // Then
        assertThat(throwable).isInstanceOf(PlanNotFoundException.class);
    }

    @Test
    void should_throw_when_plan_is_deprecated() {
        // Given: plan exists for API Product but is DEPRECATED
        givenExistingApiProduct(ApiProduct.builder().id(API_PRODUCT_ID).name("Test API Product").environmentId(ENVIRONMENT_ID).build());
        var plan = givenExistingPlan(
            PlanFixtures.aPlanHttpV4()
                .toBuilder()
                .id(PLAN_ID)
                .referenceId(API_PRODUCT_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .build()
        );
        plan.setPlanStatus(PlanStatus.DEPRECATED);

        // When
        var throwable = catchThrowable(() ->
            useCase.execute(
                CreateApiProductSubscriptionUseCase.Input.builder()
                    .apiProductId(API_PRODUCT_ID)
                    .planId(PLAN_ID)
                    .applicationId(APPLICATION_ID)
                    .auditInfo(AUDIT_INFO)
                    .build()
            )
        );

        // Then
        assertThat(throwable).isInstanceOf(PlanNotFoundException.class);
    }

    private ApiProduct givenExistingApiProduct(ApiProduct apiProduct) {
        apiProductCrudService.initWith(List.of(apiProduct));
        return apiProduct;
    }

    private Plan givenExistingPlan(Plan plan) {
        planCrudService.initWith(List.of(plan));
        return plan;
    }
}
