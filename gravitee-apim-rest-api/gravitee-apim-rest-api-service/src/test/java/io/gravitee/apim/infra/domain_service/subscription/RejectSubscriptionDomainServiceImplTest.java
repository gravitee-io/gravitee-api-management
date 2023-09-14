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
package io.gravitee.apim.infra.domain_service.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import inmemory.AuditCrudServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.TriggerNotificationDomainServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.event.SubscriptionAuditEvent;
import io.gravitee.apim.core.notification.model.hook.SubscriptionClosedApiHookContext;
import io.gravitee.apim.core.notification.model.hook.SubscriptionClosedApplicationHookContext;
import io.gravitee.apim.core.notification.model.hook.SubscriptionRejectedApiHookContext;
import io.gravitee.apim.core.notification.model.hook.SubscriptionRejectedApplicationHookContext;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.apim.infra.domain_service.audit.AuditDomainServiceImpl;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.model.v4.plan.BasePlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyClosedException;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class RejectSubscriptionDomainServiceImplTest {

    public static final AuditActor CURRENT_USER = AuditActor.builder().userId("user-id").build();
    public static final String PLAN_CLOSED = "plan-closed";
    public static final String PLAN_PUBLISHED = "plan-published";

    @Mock
    SubscriptionRepository subscriptionRepository;

    AuditCrudServiceInMemory auditCrudServiceInMemory;
    PlanCrudServiceInMemory planCrudService;
    TriggerNotificationDomainServiceInMemory triggerNotificationDomainService;
    UserCrudServiceInMemory userCrudService;
    RejectSubscriptionDomainServiceImpl cut;

    @BeforeEach
    void setUp() {
        UuidString.overrideGenerator(() -> "audit-id");

        GraviteeContext.setCurrentOrganization("organization-id");
        GraviteeContext.setCurrentEnvironment("environment-id");

        triggerNotificationDomainService = new TriggerNotificationDomainServiceInMemory();

        auditCrudServiceInMemory = new AuditCrudServiceInMemory();
        planCrudService = new PlanCrudServiceInMemory();
        userCrudService = new UserCrudServiceInMemory();
        var auditDomainService = new AuditDomainServiceImpl(auditCrudServiceInMemory, userCrudService, GraviteeJacksonMapper.getInstance());
        cut =
            new RejectSubscriptionDomainServiceImpl(
                subscriptionRepository,
                planCrudService,
                auditDomainService,
                triggerNotificationDomainService,
                userCrudService
            );
        planCrudService.initWith(
            List.of(
                BasePlanEntity.builder().id(PLAN_CLOSED).status(PlanStatus.CLOSED).build(),
                BasePlanEntity.builder().id(PLAN_PUBLISHED).status(PlanStatus.PUBLISHED).build()
            )
        );

        allowSubscriptionRepositorySave();
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
        planCrudService.reset();
        triggerNotificationDomainService.reset();
        userCrudService.reset();
    }

    @Nested
    class RejectEntity {

        @Test
        void should_throw_when_null_subscription() {
            assertThatThrownBy(() -> cut.rejectSubscription(GraviteeContext.getExecutionContext(), (SubscriptionEntity) null, CURRENT_USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Subscription should not be null");
        }

        @ParameterizedTest
        @EnumSource(value = SubscriptionEntity.Status.class, mode = EnumSource.Mode.EXCLUDE, names = "PENDING")
        void should_throw_when_status_not_pending(SubscriptionEntity.Status status) {
            assertThatThrownBy(() ->
                    cut.rejectSubscription(
                        GraviteeContext.getExecutionContext(),
                        SubscriptionEntity.builder().status(status).build(),
                        CURRENT_USER
                    )
                )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot reject subscription");
        }

        @Test
        void should_throw_when_plan_is_closed() {
            assertThatThrownBy(() ->
                    cut.rejectSubscription(
                        GraviteeContext.getExecutionContext(),
                        SubscriptionEntity.builder().status(SubscriptionEntity.Status.PENDING).planId(PLAN_CLOSED).build(),
                        CURRENT_USER
                    )
                )
                .isInstanceOf(PlanAlreadyClosedException.class)
                .hasMessage("Plan " + PLAN_CLOSED + " is already closed !");
        }

        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        void should_reject_subscription(boolean shouldTriggerEmailNotification) {
            // Given
            givenExistingSubscription(
                Subscription
                    .builder()
                    .subscribedBy("subscriber")
                    .id("subscription-id")
                    .application("application-id")
                    .api("api-id")
                    .plan(PLAN_PUBLISHED)
                    .status(Subscription.Status.PENDING)
                    .build()
            );
            if (shouldTriggerEmailNotification) {
                userCrudService.initWith(List.of(BaseUserEntity.builder().id("subscriber").email("subscriber@mail.fake").build()));
            }

            // When
            final SubscriptionEntity result = cut.rejectSubscription(
                GraviteeContext.getExecutionContext(),
                SubscriptionEntity
                    .builder()
                    .id("subscription-id")
                    .subscribedBy("subscriber")
                    .applicationId("application-id")
                    .apiId("api-id")
                    .status(SubscriptionEntity.Status.PENDING)
                    .planId(PLAN_PUBLISHED)
                    .build(),
                CURRENT_USER
            );

            // Then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getId()).isEqualTo("subscription-id");
                softly.assertThat(result.getStatus()).isEqualTo(SubscriptionEntity.Status.REJECTED);
            });

            assertThat(triggerNotificationDomainService.getApiNotifications())
                .containsExactly(new SubscriptionRejectedApiHookContext("api-id", "application-id", "plan-published"));

            assertThat(triggerNotificationDomainService.getApplicationNotifications())
                .containsExactly(new SubscriptionRejectedApplicationHookContext("application-id", "api-id", "plan-published"));

            if (shouldTriggerEmailNotification) {
                assertThat(triggerNotificationDomainService.getApplicationEmailNotifications())
                    .containsExactly(new SubscriptionRejectedApplicationHookContext("application-id", "api-id", "plan-published"));
            } else {
                assertThat(triggerNotificationDomainService.getApplicationEmailNotifications()).isEmpty();
            }

            assertThat(auditCrudServiceInMemory.storage())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt", "patch")
                .containsExactly(
                    new AuditEntity(
                        "audit-id",
                        "organization-id",
                        "environment-id",
                        AuditEntity.AuditReferenceType.API,
                        "api-id",
                        "user-id",
                        Map.of("APPLICATION", "application-id"),
                        SubscriptionAuditEvent.SUBSCRIPTION_UPDATED.name(),
                        ZonedDateTime.now(),
                        ""
                    ),
                    new AuditEntity(
                        "audit-id",
                        "organization-id",
                        "environment-id",
                        AuditEntity.AuditReferenceType.APPLICATION,
                        "application-id",
                        "user-id",
                        Map.of("API", "api-id"),
                        SubscriptionAuditEvent.SUBSCRIPTION_UPDATED.name(),
                        ZonedDateTime.now(),
                        ""
                    )
                );
        }
    }

    @Nested
    class RejectById {

        @Test
        @SneakyThrows
        void should_throw_when_subscription_not_found() {
            when(subscriptionRepository.findById("subscription-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cut.rejectSubscription(GraviteeContext.getExecutionContext(), "subscription-id", CURRENT_USER))
                .isInstanceOf(SubscriptionNotFoundException.class)
                .hasMessage("Subscription [subscription-id] cannot be found.");
        }

        @ParameterizedTest
        @EnumSource(value = SubscriptionEntity.Status.class, mode = EnumSource.Mode.EXCLUDE, names = "PENDING")
        void should_throw_when_status_not_pending(SubscriptionEntity.Status status) {
            givenExistingSubscription(
                Subscription
                    .builder()
                    .subscribedBy("subscriber")
                    .id("subscription-id")
                    .application("application-id")
                    .api("api-id")
                    .plan(PLAN_PUBLISHED)
                    .status(Subscription.Status.valueOf(status.name()))
                    .build()
            );
            assertThatThrownBy(() -> cut.rejectSubscription(GraviteeContext.getExecutionContext(), "subscription-id", CURRENT_USER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot reject subscription");
        }

        @Test
        void should_throw_when_plan_is_closed() {
            givenExistingSubscription(
                Subscription
                    .builder()
                    .subscribedBy("subscriber")
                    .id("subscription-id")
                    .application("application-id")
                    .api("api-id")
                    .plan(PLAN_CLOSED)
                    .status(Subscription.Status.PENDING)
                    .build()
            );
            assertThatThrownBy(() -> cut.rejectSubscription(GraviteeContext.getExecutionContext(), "subscription-id", CURRENT_USER))
                .isInstanceOf(PlanAlreadyClosedException.class)
                .hasMessage("Plan " + PLAN_CLOSED + " is already closed !");
        }

        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        void should_reject_subscription(boolean shouldTriggerEmailNotification) {
            // Given
            givenExistingSubscription(
                Subscription
                    .builder()
                    .subscribedBy("subscriber")
                    .id("subscription-id")
                    .application("application-id")
                    .api("api-id")
                    .plan(PLAN_PUBLISHED)
                    .status(Subscription.Status.PENDING)
                    .build()
            );
            if (shouldTriggerEmailNotification) {
                userCrudService.initWith(List.of(BaseUserEntity.builder().id("subscriber").email("subscriber@mail.fake").build()));
            }

            // When
            final SubscriptionEntity result = cut.rejectSubscription(
                GraviteeContext.getExecutionContext(),
                "subscription-id",
                CURRENT_USER
            );

            // Then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getId()).isEqualTo("subscription-id");
                softly.assertThat(result.getStatus()).isEqualTo(SubscriptionEntity.Status.REJECTED);
            });

            assertThat(triggerNotificationDomainService.getApiNotifications())
                .containsExactly(new SubscriptionRejectedApiHookContext("api-id", "application-id", "plan-published"));

            assertThat(triggerNotificationDomainService.getApplicationNotifications())
                .containsExactly(new SubscriptionRejectedApplicationHookContext("application-id", "api-id", "plan-published"));

            if (shouldTriggerEmailNotification) {
                assertThat(triggerNotificationDomainService.getApplicationEmailNotifications())
                    .containsExactly(new SubscriptionRejectedApplicationHookContext("application-id", "api-id", "plan-published"));
            } else {
                assertThat(triggerNotificationDomainService.getApplicationEmailNotifications()).isEmpty();
            }

            assertThat(auditCrudServiceInMemory.storage())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt", "patch")
                .containsExactly(
                    new AuditEntity(
                        "audit-id",
                        "organization-id",
                        "environment-id",
                        AuditEntity.AuditReferenceType.API,
                        "api-id",
                        "user-id",
                        Map.of("APPLICATION", "application-id"),
                        SubscriptionAuditEvent.SUBSCRIPTION_UPDATED.name(),
                        ZonedDateTime.now(),
                        ""
                    ),
                    new AuditEntity(
                        "audit-id",
                        "organization-id",
                        "environment-id",
                        AuditEntity.AuditReferenceType.APPLICATION,
                        "application-id",
                        "user-id",
                        Map.of("API", "api-id"),
                        SubscriptionAuditEvent.SUBSCRIPTION_UPDATED.name(),
                        ZonedDateTime.now(),
                        ""
                    )
                );
        }
    }

    @SneakyThrows
    private void givenExistingSubscription(Subscription subscription) {
        lenient().when(subscriptionRepository.findById(any())).thenReturn(Optional.empty());
        lenient().when(subscriptionRepository.findById(eq(subscription.getId()))).thenReturn(Optional.of(subscription));
    }

    @SneakyThrows
    private void allowSubscriptionRepositorySave() {
        lenient().when(subscriptionRepository.update(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    }
}
