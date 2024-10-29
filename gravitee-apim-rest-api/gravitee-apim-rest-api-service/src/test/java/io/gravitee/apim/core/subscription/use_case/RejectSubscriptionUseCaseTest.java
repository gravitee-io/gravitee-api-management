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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.PlanFixtures;
import fixtures.core.model.SubscriptionFixtures;
import inmemory.ApiKeyCrudServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PlanCrudServiceInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import inmemory.TriggerNotificationDomainServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.SubscriptionAuditEvent;
import io.gravitee.apim.core.notification.model.Recipient;
import io.gravitee.apim.core.notification.model.hook.SubscriptionRejectedApiHookContext;
import io.gravitee.apim.core.notification.model.hook.SubscriptionRejectedApplicationHookContext;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.domain_service.RejectSubscriptionDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyClosedException;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RejectSubscriptionUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String API_ID = "api-id";
    private static final String USER_ID = "user-id";

    private static final String REASON_MESSAGE = "a reason explaining the reject";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    private final SubscriptionCrudServiceInMemory subscriptionCrudService = new SubscriptionCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final TriggerNotificationDomainServiceInMemory triggerNotificationService = new TriggerNotificationDomainServiceInMemory();
    private final ApplicationCrudServiceInMemory applicationCrudService = new ApplicationCrudServiceInMemory();
    private final ApiKeyCrudServiceInMemory apiKeyCrudService = new ApiKeyCrudServiceInMemory();
    private final PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();

    private RejectSubscriptionUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.reset();
    }

    @BeforeEach
    void setUp() {
        var auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());

        var rejectDomainService = new RejectSubscriptionDomainService(
            subscriptionCrudService,
            auditDomainService,
            triggerNotificationService,
            userCrudService
        );
        useCase = new RejectSubscriptionUseCase(subscriptionCrudService, planCrudService, rejectDomainService);
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(apiKeyCrudService, applicationCrudService, auditCrudService, planCrudService, subscriptionCrudService, userCrudService)
            .forEach(InMemoryAlternative::reset);
        triggerNotificationService.reset();
    }

    @Test
    void should_reject_subscription() {
        // Given
        var plan = givenExistingPlan(PlanFixtures.aPlanHttpV4().toBuilder().id("plan-id").build().setPlanStatus(PlanStatus.PUBLISHED));
        var subscription = givenExistingSubscription(
            SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .subscribedBy("subscriber")
                .planId(plan.getId())
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
    void should_create_audit() {
        // Given
        var plan = givenExistingPlan(PlanFixtures.aPlanHttpV4().toBuilder().id("plan-id").build().setPlanStatus(PlanStatus.PUBLISHED));
        var subscription = givenExistingSubscription(
            SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .subscribedBy("subscriber")
                .planId(plan.getId())
                .status(SubscriptionEntity.Status.PENDING)
                .build()
        );

        // When
        reject(subscription.getId());

        // Then
        assertThat(auditCrudService.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt", "patch")
            .containsExactly(
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
    void should_send_notifications_to_api_and_application_owners() {
        // Given
        var plan = givenExistingPlan(PlanFixtures.aPlanHttpV4().toBuilder().id("plan-id").build().setPlanStatus(PlanStatus.PUBLISHED));
        var subscription = givenExistingSubscription(
            SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .subscribedBy("subscriber")
                .planId(plan.getId())
                .status(SubscriptionEntity.Status.PENDING)
                .build()
        );

        // When
        reject(subscription.getId());

        // Then
        assertThat(triggerNotificationService.getApiNotifications())
            .containsExactly(new SubscriptionRejectedApiHookContext("api-id", "application-id", "plan-published", "subscription-id"));

        assertThat(triggerNotificationService.getApplicationNotifications())
            .containsExactly(
                new TriggerNotificationDomainServiceInMemory.ApplicationNotification(
                    new SubscriptionRejectedApplicationHookContext("application-id", "api-id", "plan-published", "subscription-id")
                )
            );
    }

    @Test
    void should_send_notifications_to_subscriber_when_they_have_an_email() {
        // Given
        var subscriber = givenExistingUser(BaseUserEntity.builder().id("subscriber").email("subscriber@mail.fake").build());
        var plan = givenExistingPlan(PlanFixtures.aPlanHttpV4().toBuilder().id("plan-id").build().setPlanStatus(PlanStatus.PUBLISHED));
        var subscription = givenExistingSubscription(
            SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .subscribedBy(subscriber.getId())
                .planId(plan.getId())
                .status(SubscriptionEntity.Status.PENDING)
                .build()
        );

        // When
        reject(subscription.getId());

        // Then
        assertThat(triggerNotificationService.getApplicationNotifications())
            .contains(
                new TriggerNotificationDomainServiceInMemory.ApplicationNotification(
                    new Recipient("EMAIL", "subscriber@mail.fake"),
                    new SubscriptionRejectedApplicationHookContext("application-id", "api-id", "plan-published", "subscription-id")
                )
            );
    }

    @Test
    @SneakyThrows
    void should_throw_when_subscription_not_found() {
        assertThatThrownBy(() -> reject("unknown"))
            .isInstanceOf(SubscriptionNotFoundException.class)
            .hasMessage("Subscription [unknown] cannot be found.");
    }

    @Test
    void should_throw_when_subscription_does_not_belong_to_API() {
        // Given
        var subscription = givenExistingSubscription(SubscriptionFixtures.aSubscription().toBuilder().apiId("other-id").build());

        // When
        var throwable = catchThrowable(() -> reject(subscription.getId()));

        // Then
        assertThat(throwable).isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    void should_throw_when_plan_is_closed() {
        var plan = givenExistingPlan(PlanFixtures.aPlanHttpV4().toBuilder().id("plan-id").build().setPlanStatus(PlanStatus.CLOSED));
        var subscription = givenExistingSubscription(
            SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .subscribedBy("subscriber")
                .planId(plan.getId())
                .status(SubscriptionEntity.Status.PENDING)
                .build()
        );
        assertThatThrownBy(() -> reject(subscription.getId()))
            .isInstanceOf(PlanAlreadyClosedException.class)
            .hasMessage("Plan " + plan.getId() + " is already closed !");
    }

    private SubscriptionEntity givenExistingSubscription(SubscriptionEntity subscription) {
        subscriptionCrudService.initWith(List.of(subscription));
        return subscription;
    }

    private Plan givenExistingPlan(Plan plan) {
        planCrudService.initWith(List.of(plan));
        return plan;
    }

    private BaseUserEntity givenExistingUser(BaseUserEntity user) {
        userCrudService.initWith(List.of(user));
        return user;
    }

    private RejectSubscriptionUseCase.Output reject(String subscriptionId) {
        return useCase.execute(new RejectSubscriptionUseCase.Input(API_ID, subscriptionId, REASON_MESSAGE, AUDIT_INFO));
    }
}
