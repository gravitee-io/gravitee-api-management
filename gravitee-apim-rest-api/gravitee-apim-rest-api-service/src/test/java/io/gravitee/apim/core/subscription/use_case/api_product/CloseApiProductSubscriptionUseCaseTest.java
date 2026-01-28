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

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.SubscriptionFixtures;
import inmemory.*;
import io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.ApplicationPrimaryOwnerDomainService;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CloseApiProductSubscriptionUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String API_PRODUCT_ID = "api-product-id";
    private static final String USER_ID = "user-id";
    private static final String APPLICATION_ID = "application-id";
    private static final String SUBSCRIPTION_ID = "subscription-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    private final ApiProductCrudServiceInMemory apiProductCrudService = new ApiProductCrudServiceInMemory();
    private final SubscriptionCrudServiceInMemory subscriptionCrudService = new SubscriptionCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudServiceInMemory = new AuditCrudServiceInMemory();
    private final TriggerNotificationDomainServiceInMemory triggerNotificationService = new TriggerNotificationDomainServiceInMemory();
    private final ApplicationCrudServiceInMemory applicationCrudService = new ApplicationCrudServiceInMemory();
    private final ApiKeyCrudServiceInMemory apiKeyCrudService = new ApiKeyCrudServiceInMemory();
    private final IntegrationAgentInMemory integrationAgent = new IntegrationAgentInMemory();
    private final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    private final MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    private final RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private CloseApiProductSubscriptionUseCase useCase;

    @BeforeEach
    void setUp() {
        UuidString.overrideGenerator(() -> "audit-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));

        var auditDomainService = new AuditDomainService(auditCrudServiceInMemory, userCrudService, new JacksonJsonDiffProcessor());

        var applicationPrimaryOwnerDomainService = new ApplicationPrimaryOwnerDomainService(
            groupQueryService,
            membershipQueryService,
            roleQueryService,
            userCrudService
        );

        var rejectSubscriptionDomainService = new RejectSubscriptionDomainService(
            subscriptionCrudService,
            auditDomainService,
            new TriggerNotificationDomainServiceInMemory(),
            userCrudService,
            applicationPrimaryOwnerDomainService
        );

        var revokeApiKeyDomainService = new RevokeApiKeyDomainService(
            apiKeyCrudService,
            new ApiKeyQueryServiceInMemory(apiKeyCrudService),
            subscriptionCrudService,
            auditDomainService,
            triggerNotificationService
        );

        var closeSubscriptionDomainService = new CloseSubscriptionDomainService(
            subscriptionCrudService,
            applicationCrudService,
            auditDomainService,
            triggerNotificationService,
            rejectSubscriptionDomainService,
            revokeApiKeyDomainService,
            new ApiCrudServiceInMemory(),
            integrationAgent
        );

        useCase = new CloseApiProductSubscriptionUseCase(subscriptionCrudService, closeSubscriptionDomainService, apiProductCrudService);

        applicationCrudService.initWith(
            List.of(
                fixtures.ApplicationModelFixtures.anApplicationEntity()
                    .toBuilder()
                    .id(APPLICATION_ID)
                    .primaryOwner(PrimaryOwnerEntity.builder().id(USER_ID).displayName("Jane").build())
                    .build()
            )
        );
        roleQueryService.resetSystemRoles(ORGANIZATION_ID);
        membershipQueryService.initWith(
            List.of(
                fixtures.core.model.MembershipFixtures.anApplicationPrimaryOwnerUserMembership(APPLICATION_ID, USER_ID, ORGANIZATION_ID)
            )
        );
        userCrudService.initWith(
            List.of(BaseUserEntity.builder().id(USER_ID).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(
            apiProductCrudService,
            subscriptionCrudService,
            auditCrudServiceInMemory,
            applicationCrudService,
            apiKeyCrudService
        ).forEach(InMemoryAlternative::reset);
        triggerNotificationService.reset();
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.reset();
    }

    @ParameterizedTest
    @EnumSource(value = SubscriptionEntity.Status.class, names = { "ACCEPTED", "PAUSED" })
    void should_close_accepted_or_paused_subscription(SubscriptionEntity.Status status) {
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
                .status(status)
                .build()
        );

        // When
        var result = close(subscription.getId());

        // Then
        assertThat(result.subscription())
            .extracting(SubscriptionEntity::getId, SubscriptionEntity::getStatus)
            .containsExactly(SUBSCRIPTION_ID, SubscriptionEntity.Status.CLOSED);
    }

    @Test
    void should_reject_pending_subscription() {
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
        var result = close(subscription.getId());

        // Then
        assertThat(result.subscription())
            .extracting(SubscriptionEntity::getId, SubscriptionEntity::getStatus)
            .containsExactly(SUBSCRIPTION_ID, SubscriptionEntity.Status.REJECTED);
    }

    @ParameterizedTest
    @EnumSource(value = SubscriptionEntity.Status.class, names = { "CLOSED", "REJECTED" })
    void should_do_nothing_if_subscription_already_closed(SubscriptionEntity.Status status) {
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
                .status(status)
                .build()
        );

        // When
        var result = close(subscription.getId());

        // Then
        assertThat(result.subscription()).extracting(SubscriptionEntity::getStatus).isEqualTo(status);
    }

    @Test
    void should_throw_when_api_product_not_found() {
        // When
        var throwable = catchThrowable(() -> close(SUBSCRIPTION_ID));

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
        var throwable = catchThrowable(() -> close("unknown-subscription"));

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
                .status(SubscriptionEntity.Status.ACCEPTED)
                .build()
        );

        // When
        var throwable = catchThrowable(() -> close(subscription.getId()));

        // Then
        assertThat(throwable).isInstanceOf(SubscriptionNotFoundException.class);
    }

    private ApiProduct givenExistingApiProduct(ApiProduct apiProduct) {
        apiProductCrudService.initWith(List.of(apiProduct));
        return apiProduct;
    }

    private SubscriptionEntity givenExistingSubscription(SubscriptionEntity subscription) {
        subscriptionCrudService.initWith(List.of(subscription));
        return subscription;
    }

    private CloseApiProductSubscriptionUseCase.Output close(String subscriptionId) {
        return useCase.execute(
            CloseApiProductSubscriptionUseCase.Input.builder()
                .apiProductId(API_PRODUCT_ID)
                .subscriptionId(subscriptionId)
                .auditInfo(AUDIT_INFO)
                .build()
        );
    }
}
