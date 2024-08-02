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
package io.gravitee.apim.core.subscription.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.ApplicationModelFixtures;
import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.PlanFixtures;
import fixtures.core.model.SubscriptionFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiKeyCrudServiceInMemory;
import inmemory.ApiKeyQueryServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IntegrationAgentInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import inmemory.TriggerNotificationDomainServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_key.domain_service.GenerateApiKeyDomainService;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.SubscriptionAuditEvent;
import io.gravitee.apim.core.notification.model.Recipient;
import io.gravitee.apim.core.notification.model.hook.SubscriptionAcceptedApiHookContext;
import io.gravitee.apim.core.notification.model.hook.SubscriptionAcceptedApplicationHookContext;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.federation.FederatedPlan;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AcceptSubscriptionDomainServiceTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";

    private static final Plan PLAN_CLOSED = PlanFixtures.aPlanV4().toBuilder().id("plan-closed").build().setPlanStatus(PlanStatus.CLOSED);
    private static final Plan PLAN_PUBLISHED = PlanFixtures
        .anApiKeyV4()
        .toBuilder()
        .id("plan-published")
        .build()
        .setPlanStatus(PlanStatus.PUBLISHED);
    private static final Plan PUSH_PLAN = PlanFixtures.aPushPlan().toBuilder().id("plan-push").build().setPlanStatus(PlanStatus.PUBLISHED);

    private static final ZonedDateTime STARTING_AT = Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault());
    private static final ZonedDateTime ENDING_AT = Instant.parse("2024-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault());
    private static final String REASON = "Subscription accepted";

    private static final String APPLICATION_ID = "application-id";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    SubscriptionCrudServiceInMemory subscriptionCrudService = new SubscriptionCrudServiceInMemory();

    AuditCrudServiceInMemory auditCrudServiceInMemory = new AuditCrudServiceInMemory();
    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    TriggerNotificationDomainServiceInMemory triggerNotificationDomainService = new TriggerNotificationDomainServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    ApiKeyCrudServiceInMemory apiKeyCrudService = new ApiKeyCrudServiceInMemory();
    ApplicationCrudServiceInMemory applicationCrudService = new ApplicationCrudServiceInMemory();
    IntegrationAgentInMemory integrationAgent = new IntegrationAgentInMemory();
    AcceptSubscriptionDomainService cut;

    @BeforeAll
    static void init() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @BeforeEach
    void setUp() {
        var auditDomainService = new AuditDomainService(auditCrudServiceInMemory, userCrudService, new JacksonJsonDiffProcessor());

        var generateApiKeyDomainService = new GenerateApiKeyDomainService(
            apiKeyCrudService,
            new ApiKeyQueryServiceInMemory(apiKeyCrudService),
            applicationCrudService,
            auditDomainService
        );

        cut =
            new AcceptSubscriptionDomainService(
                subscriptionCrudService,
                auditDomainService,
                apiCrudService,
                applicationCrudService,
                planCrudService,
                generateApiKeyDomainService,
                integrationAgent,
                triggerNotificationDomainService,
                userCrudService
            );

        planCrudService.initWith(List.of(PLAN_CLOSED, PLAN_PUBLISHED, PUSH_PLAN));

        applicationCrudService.initWith(List.of(ApplicationModelFixtures.anApplicationEntity().toBuilder().id(APPLICATION_ID).build()));
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(
                apiCrudService,
                apiKeyCrudService,
                applicationCrudService,
                auditCrudServiceInMemory,
                integrationAgent,
                planCrudService,
                subscriptionCrudService,
                userCrudService
            )
            .forEach(InMemoryAlternative::reset);
        triggerNotificationDomainService.reset();
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Test
    void should_throw_when_null_subscription() {
        assertThatThrownBy(() -> accept(null, PLAN_PUBLISHED))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Subscription should not be null");
    }

    @Test
    void should_accept_subscription() {
        // Given
        SubscriptionEntity subscription = givenExistingSubscription(
            SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .subscribedBy("subscriber")
                .planId(PLAN_PUBLISHED.getId())
                .applicationId(APPLICATION_ID)
                .status(SubscriptionEntity.Status.PENDING)
                .build()
        );

        // When
        final SubscriptionEntity result = accept(subscription, PLAN_PUBLISHED);

        // Then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getId()).isEqualTo("subscription-id");
            softly.assertThat(result.getStatus()).isEqualTo(SubscriptionEntity.Status.ACCEPTED);
        });

        assertThat(auditCrudServiceInMemory.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt", "patch")
            .contains(
                new AuditEntity(
                    "generated-id",
                    ORGANIZATION_ID,
                    ENVIRONMENT_ID,
                    AuditEntity.AuditReferenceType.API,
                    "api-id",
                    USER_ID,
                    Map.of("APPLICATION", "application-id"),
                    SubscriptionAuditEvent.SUBSCRIPTION_UPDATED.name(),
                    ZonedDateTime.now(),
                    ""
                ),
                new AuditEntity(
                    "generated-id",
                    ORGANIZATION_ID,
                    ENVIRONMENT_ID,
                    AuditEntity.AuditReferenceType.APPLICATION,
                    "application-id",
                    USER_ID,
                    Map.of("API", "api-id"),
                    SubscriptionAuditEvent.SUBSCRIPTION_UPDATED.name(),
                    ZonedDateTime.now(),
                    ""
                )
            );
    }

    @Test
    void should_generated_key_for_API_Key_plan() {
        // Given
        SubscriptionEntity subscription = givenExistingSubscription(
            SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .subscribedBy("subscriber")
                .planId(PLAN_PUBLISHED.getId())
                .applicationId(APPLICATION_ID)
                .status(SubscriptionEntity.Status.PENDING)
                .build()
        );

        // When
        accept(subscription, PLAN_PUBLISHED);

        // Then
        assertThat(apiKeyCrudService.storage())
            .containsOnly(
                ApiKeyEntity
                    .builder()
                    .id("generated-id")
                    .applicationId(subscription.getApplicationId())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .key("generated-id")
                    .subscriptions(List.of(subscription.getId()))
                    .expireAt(ENDING_AT)
                    .build()
            );
    }

    @Test
    void should_not_generated_key_for_not_API_Key_plan() {
        // Given
        SubscriptionEntity subscription = givenExistingSubscription(
            SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .subscribedBy("subscriber")
                .planId(PUSH_PLAN.getId())
                .applicationId(APPLICATION_ID)
                .status(SubscriptionEntity.Status.PENDING)
                .build()
        );

        // When
        accept(subscription, PUSH_PLAN);

        // Then
        assertThat(apiKeyCrudService.storage()).isEmpty();
    }

    @Test
    void should_trigger_notifications_for_API_and_Application_owners() {
        // Given
        SubscriptionEntity subscription = givenExistingSubscription(
            SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .subscribedBy("subscriber")
                .planId(PLAN_PUBLISHED.getId())
                .status(SubscriptionEntity.Status.PENDING)
                .build()
        );

        // When
        accept(subscription, PLAN_PUBLISHED);

        // Then
        assertThat(triggerNotificationDomainService.getApiNotifications())
            .containsExactly(new SubscriptionAcceptedApiHookContext("api-id", "application-id", "plan-published", "subscription-id"));

        assertThat(triggerNotificationDomainService.getApplicationNotifications())
            .containsExactly(
                new TriggerNotificationDomainServiceInMemory.ApplicationNotification(
                    new SubscriptionAcceptedApplicationHookContext("application-id", "api-id", "plan-published", "subscription-id")
                )
            );
    }

    @Test
    void should_trigger_notifications_for_subscriber_when_it_has_email() {
        // Given
        SubscriptionEntity subscription = givenExistingSubscription(
            SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .subscribedBy("subscriber")
                .planId(PLAN_PUBLISHED.getId())
                .status(SubscriptionEntity.Status.PENDING)
                .build()
        );
        userCrudService.initWith(List.of(BaseUserEntity.builder().id("subscriber").email("subscriber@mail.fake").build()));

        // When
        accept(subscription, PLAN_PUBLISHED);

        // Then
        assertThat(triggerNotificationDomainService.getApplicationNotifications())
            .contains(
                new TriggerNotificationDomainServiceInMemory.ApplicationNotification(
                    new Recipient("EMAIL", "subscriber@mail.fake"),
                    new SubscriptionAcceptedApplicationHookContext("application-id", "api-id", "plan-published", "subscription-id")
                )
            );
    }

    @Nested
    class FederatedSubscription {

        @ParameterizedTest
        @ValueSource(strings = { "api-key", "oauth2" })
        void should_trigger_notifications_for_federated_subscriber_when_it_has_email(String securityType) {
            // Given
            Plan plan = PlanFixtures
                .anApiKeyV4()
                .toBuilder()
                .definitionVersion(DefinitionVersion.FEDERATED)
                .id("plan-published")
                .federatedPlanDefinition(FederatedPlan.builder().security(PlanSecurity.builder().type(securityType).build()).build())
                .build()
                .setPlanStatus(PlanStatus.PUBLISHED);
            SubscriptionEntity subscription = givenExistingSubscription(
                SubscriptionFixtures
                    .aSubscription()
                    .toBuilder()
                    .subscribedBy("subscriber")
                    .planId(plan.getId())
                    .status(SubscriptionEntity.Status.PENDING)
                    .build()
            );
            userCrudService.initWith(List.of(BaseUserEntity.builder().id("subscriber").email("subscriber@mail.fake").build()));
            apiCrudService.create(
                Api.builder().id(subscription.getApiId()).originContext(new OriginContext.Integration("integration-id")).build()
            );

            // When
            accept(subscription, plan);

            // Then
            assertThat(triggerNotificationDomainService.getApplicationNotifications())
                .contains(
                    new TriggerNotificationDomainServiceInMemory.ApplicationNotification(
                        new Recipient("EMAIL", "subscriber@mail.fake"),
                        new SubscriptionAcceptedApplicationHookContext(
                            "application-id",
                            subscription.getApiId(),
                            "plan-published",
                            "subscription-id"
                        )
                    )
                );
        }
    }

    private SubscriptionEntity givenExistingSubscription(SubscriptionEntity subscription) {
        subscriptionCrudService.initWith(List.of(subscription));
        return subscription;
    }

    private SubscriptionEntity accept(SubscriptionEntity subscription, Plan plan) {
        return cut.accept(subscription, plan, STARTING_AT, ENDING_AT, REASON, "", AUDIT_INFO);
    }
}
