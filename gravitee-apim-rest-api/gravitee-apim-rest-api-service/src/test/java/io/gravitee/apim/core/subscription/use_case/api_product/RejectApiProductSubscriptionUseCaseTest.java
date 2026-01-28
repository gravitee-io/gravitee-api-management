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

import static fixtures.core.model.MembershipFixtures.anApplicationPrimaryOwnerUserMembership;
import static fixtures.core.model.SubscriptionFixtures.aSubscription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import fixtures.ApplicationModelFixtures;
import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.SubscriptionFixtures;
import inmemory.*;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.ApplicationPrimaryOwnerDomainService;
import io.gravitee.apim.core.subscription.domain_service.RejectSubscriptionDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RejectApiProductSubscriptionUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String API_PRODUCT_ID = "api-product-id";
    private static final String USER_ID = "user-id";
    private static final String APPLICATION_ID = "application-id";
    private static final String SUBSCRIPTION_ID = "subscription-id";
    private static final String REASON_MESSAGE = "Subscription rejected";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    private final ApiProductCrudServiceInMemory apiProductCrudService = new ApiProductCrudServiceInMemory();
    private final SubscriptionCrudServiceInMemory subscriptionCrudService = new SubscriptionCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final TriggerNotificationDomainServiceInMemory triggerNotificationService = new TriggerNotificationDomainServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final ApplicationCrudServiceInMemory applicationCrudService = new ApplicationCrudServiceInMemory();
    private final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    private final MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    private final RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    private RejectApiProductSubscriptionUseCase useCase;

    @BeforeEach
    void setUp() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));

        var auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());

        var applicationPrimaryOwnerDomainService = new ApplicationPrimaryOwnerDomainService(
            groupQueryService,
            membershipQueryService,
            roleQueryService,
            userCrudService
        );

        var rejectDomainService = new RejectSubscriptionDomainService(
            subscriptionCrudService,
            auditDomainService,
            triggerNotificationService,
            userCrudService,
            applicationPrimaryOwnerDomainService
        );

        useCase = new RejectApiProductSubscriptionUseCase(subscriptionCrudService, rejectDomainService, apiProductCrudService);

        roleQueryService.resetSystemRoles(ORGANIZATION_ID);
        membershipQueryService.initWith(List.of(anApplicationPrimaryOwnerUserMembership(APPLICATION_ID, USER_ID, ORGANIZATION_ID)));
        applicationCrudService.initWith(
            List.of(
                ApplicationModelFixtures.anApplicationEntity()
                    .toBuilder()
                    .id(APPLICATION_ID)
                    .primaryOwner(PrimaryOwnerEntity.builder().id(USER_ID).displayName("Jane").build())
                    .build()
            )
        );
        userCrudService.initWith(
            List.of(BaseUserEntity.builder().id(USER_ID).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(apiProductCrudService, auditCrudService, subscriptionCrudService, userCrudService, applicationCrudService).forEach(
            InMemoryAlternative::reset
        );
        triggerNotificationService.reset();
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.reset();
    }

    @Test
    void should_reject_subscription_for_api_product() {
        // Given
        var apiProduct = givenExistingApiProduct(
            ApiProduct.builder().id(API_PRODUCT_ID).name("Test API Product").environmentId(ENVIRONMENT_ID).build()
        );
        var subscription = givenExistingSubscription(
            aSubscription()
                .toBuilder()
                .id(SUBSCRIPTION_ID)
                .referenceId(API_PRODUCT_ID)
                .referenceType(SubscriptionReferenceType.API_PRODUCT)
                .applicationId(APPLICATION_ID)
                .status(SubscriptionEntity.Status.PENDING)
                .build()
        );

        // When
        var result = reject(subscription.getId());

        // Then
        assertThat(result.subscription())
            .extracting(
                SubscriptionEntity::getId,
                SubscriptionEntity::getStatus,
                SubscriptionEntity::getReasonMessage,
                SubscriptionEntity::getClosedAt
            )
            .containsExactly(
                subscription.getId(),
                SubscriptionEntity.Status.REJECTED,
                REASON_MESSAGE,
                INSTANT_NOW.atZone(ZoneId.systemDefault())
            );
    }

    @Test
    void should_throw_when_api_product_not_found() {
        // When
        var throwable = catchThrowable(() -> reject(SUBSCRIPTION_ID));

        // Then
        assertThat(throwable).isInstanceOf(ApiProductNotFoundException.class);
    }

    @Test
    void should_throw_when_subscription_not_found() {
        // Given
        var apiProduct = givenExistingApiProduct(
            ApiProduct.builder().id(API_PRODUCT_ID).name("Test API Product").environmentId(ENVIRONMENT_ID).build()
        );

        // When
        var throwable = catchThrowable(() -> reject("unknown-subscription"));

        // Then
        assertThat(throwable).isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    void should_throw_when_subscription_does_not_belong_to_api_product() {
        // Given
        var apiProduct = givenExistingApiProduct(
            ApiProduct.builder().id(API_PRODUCT_ID).name("Test API Product").environmentId(ENVIRONMENT_ID).build()
        );
        var subscription = givenExistingSubscription(
            aSubscription()
                .toBuilder()
                .id(SUBSCRIPTION_ID)
                .referenceId("other-api-product-id")
                .referenceType(SubscriptionReferenceType.API_PRODUCT)
                .status(SubscriptionEntity.Status.PENDING)
                .build()
        );

        // When
        var throwable = catchThrowable(() -> reject(subscription.getId()));

        // Then
        assertThat(throwable).isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    void should_throw_when_subscription_status_not_pending() {
        // Given
        var apiProduct = givenExistingApiProduct(
            ApiProduct.builder().id(API_PRODUCT_ID).name("Test API Product").environmentId(ENVIRONMENT_ID).build()
        );
        var subscription = givenExistingSubscription(
            aSubscription()
                .toBuilder()
                .id(SUBSCRIPTION_ID)
                .referenceId(API_PRODUCT_ID)
                .referenceType(SubscriptionReferenceType.API_PRODUCT)
                .status(SubscriptionEntity.Status.ACCEPTED)
                .build()
        );

        // When
        var throwable = catchThrowable(() -> reject(subscription.getId()));

        // Then
        assertThat(throwable).isInstanceOf(IllegalStateException.class).hasMessage("Cannot reject subscription");
    }

    private ApiProduct givenExistingApiProduct(ApiProduct apiProduct) {
        apiProductCrudService.initWith(List.of(apiProduct));
        return apiProduct;
    }

    private SubscriptionEntity givenExistingSubscription(SubscriptionEntity subscription) {
        subscriptionCrudService.initWith(List.of(subscription));
        return subscription;
    }

    private RejectApiProductSubscriptionUseCase.Output reject(String subscriptionId) {
        return useCase.execute(
            RejectApiProductSubscriptionUseCase.Input.builder()
                .apiProductId(API_PRODUCT_ID)
                .subscriptionId(subscriptionId)
                .reasonMessage(REASON_MESSAGE)
                .auditInfo(AUDIT_INFO)
                .build()
        );
    }
}
