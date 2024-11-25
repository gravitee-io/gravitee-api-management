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

import static fixtures.core.model.MembershipFixtures.anApplicationPrimaryOwnerUserMembership;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.ApplicationModelFixtures;
import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.PlanFixtures;
import fixtures.core.model.SubscriptionFixtures;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import inmemory.TriggerNotificationDomainServiceInMemory;
import inmemory.TriggerNotificationDomainServiceInMemory.ApplicationNotification;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.SubscriptionAuditEvent;
import io.gravitee.apim.core.membership.domain_service.ApplicationPrimaryOwnerDomainService;
import io.gravitee.apim.core.notification.model.Recipient;
import io.gravitee.apim.core.notification.model.hook.SubscriptionRejectedApiHookContext;
import io.gravitee.apim.core.notification.model.hook.SubscriptionRejectedApplicationHookContext;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class RejectSubscriptionDomainServiceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private static final String PLAN_CLOSED = "plan-closed";
    private static final String PLAN_PUBLISHED = "plan-published";
    private static final String APPLICATION_ID = "application-id";

    SubscriptionCrudServiceInMemory subscriptionCrudService = new SubscriptionCrudServiceInMemory();

    AuditCrudServiceInMemory auditCrudServiceInMemory;
    PlanCrudServiceInMemory planCrudService;
    TriggerNotificationDomainServiceInMemory triggerNotificationDomainService;
    UserCrudServiceInMemory userCrudService;
    ApplicationCrudServiceInMemory applicationCrudService = new ApplicationCrudServiceInMemory();
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    RejectSubscriptionDomainService cut;

    @BeforeEach
    void setUp() {
        UuidString.overrideGenerator(() -> "audit-id");

        triggerNotificationDomainService = new TriggerNotificationDomainServiceInMemory();

        auditCrudServiceInMemory = new AuditCrudServiceInMemory();
        planCrudService = new PlanCrudServiceInMemory();
        userCrudService = new UserCrudServiceInMemory();
        var auditDomainService = new AuditDomainService(auditCrudServiceInMemory, userCrudService, new JacksonJsonDiffProcessor());
        var applicationPrimaryOwnerDomainService = new ApplicationPrimaryOwnerDomainService(
            groupQueryService,
            membershipQueryService,
            roleQueryService,
            userCrudService
        );
        cut =
            new RejectSubscriptionDomainService(
                subscriptionCrudService,
                planCrudService,
                auditDomainService,
                triggerNotificationDomainService,
                userCrudService,
                applicationPrimaryOwnerDomainService
            );
        planCrudService.initWith(
            List.of(
                PlanFixtures.aPlanV4().toBuilder().id(PLAN_CLOSED).build().setPlanStatus(PlanStatus.CLOSED),
                PlanFixtures.aPlanV4().toBuilder().id(PLAN_PUBLISHED).build().setPlanStatus(PlanStatus.PUBLISHED)
            )
        );

        membershipQueryService.initWith(List.of(anApplicationPrimaryOwnerUserMembership(APPLICATION_ID, USER_ID, ORGANIZATION_ID)));
        applicationCrudService.initWith(
            List.of(
                ApplicationModelFixtures
                    .anApplicationEntity()
                    .toBuilder()
                    .id(APPLICATION_ID)
                    .primaryOwner(PrimaryOwnerEntity.builder().id(USER_ID).displayName("Jane").build())
                    .build()
            )
        );
        roleQueryService.resetSystemRoles(ORGANIZATION_ID);
        userCrudService.initWith(
            List.of(BaseUserEntity.builder().id(USER_ID).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(planCrudService, subscriptionCrudService, userCrudService).forEach(InMemoryAlternative::reset);
        triggerNotificationDomainService.reset();
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
    }

    @Nested
    class RejectEntity {

        @Test
        void should_throw_when_null_subscription() {
            assertThatThrownBy(() -> reject(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Subscription should not be null");
        }

        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        void should_reject_subscription(boolean shouldTriggerEmailNotification) {
            // Given
            SubscriptionEntity subscription = SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .subscribedBy("subscriber")
                .planId(PLAN_PUBLISHED)
                .status(SubscriptionEntity.Status.PENDING)
                .build();
            givenExistingSubscription(subscription);
            if (shouldTriggerEmailNotification) {
                userCrudService.initWith(List.of(BaseUserEntity.builder().id("subscriber").email("subscriber@mail.fake").build()));
            }

            // When
            final SubscriptionEntity result = reject(subscription);

            // Then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getId()).isEqualTo("subscription-id");
                softly.assertThat(result.getStatus()).isEqualTo(SubscriptionEntity.Status.REJECTED);
            });

            assertThat(triggerNotificationDomainService.getApiNotifications())
                .containsExactly(
                    new SubscriptionRejectedApiHookContext("api-id", "application-id", "plan-published", "subscription-id", USER_ID)
                );

            if (shouldTriggerEmailNotification) {
                assertThat(triggerNotificationDomainService.getApplicationNotifications())
                    .containsExactly(
                        new ApplicationNotification(
                            new SubscriptionRejectedApplicationHookContext(
                                "application-id",
                                "api-id",
                                "plan-published",
                                "subscription-id",
                                USER_ID
                            )
                        ),
                        new ApplicationNotification(
                            new Recipient("EMAIL", "subscriber@mail.fake"),
                            new SubscriptionRejectedApplicationHookContext(
                                "application-id",
                                "api-id",
                                "plan-published",
                                "subscription-id",
                                USER_ID
                            )
                        )
                    );
            } else {
                assertThat(triggerNotificationDomainService.getApplicationNotifications())
                    .containsExactly(
                        new ApplicationNotification(
                            new SubscriptionRejectedApplicationHookContext(
                                "application-id",
                                "api-id",
                                "plan-published",
                                "subscription-id",
                                USER_ID
                            )
                        )
                    );
            }

            assertThat(auditCrudServiceInMemory.storage())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt", "patch")
                .containsExactly(
                    new AuditEntity(
                        "audit-id",
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
                        "audit-id",
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
    }

    @Nested
    class RejectById {

        @Test
        @SneakyThrows
        void should_throw_when_subscription_not_found() {
            assertThatThrownBy(() -> rejectById(SubscriptionFixtures.aSubscription()))
                .isInstanceOf(SubscriptionNotFoundException.class)
                .hasMessage("Subscription [subscription-id] cannot be found.");
        }

        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        void should_reject_subscription(boolean shouldTriggerEmailNotification) {
            // Given
            var subscription = givenExistingSubscription(
                SubscriptionFixtures
                    .aSubscription()
                    .toBuilder()
                    .subscribedBy("subscriber")
                    .planId(PLAN_PUBLISHED)
                    .status(SubscriptionEntity.Status.PENDING)
                    .build()
            );
            if (shouldTriggerEmailNotification) {
                userCrudService.initWith(List.of(BaseUserEntity.builder().id("subscriber").email("subscriber@mail.fake").build()));
            }

            // When
            final SubscriptionEntity result = rejectById(subscription);

            // Then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getId()).isEqualTo("subscription-id");
                softly.assertThat(result.getStatus()).isEqualTo(SubscriptionEntity.Status.REJECTED);
            });

            assertThat(triggerNotificationDomainService.getApiNotifications())
                .containsExactly(
                    new SubscriptionRejectedApiHookContext("api-id", "application-id", "plan-published", "subscription-id", USER_ID)
                );

            if (shouldTriggerEmailNotification) {
                assertThat(triggerNotificationDomainService.getApplicationNotifications())
                    .containsExactly(
                        new ApplicationNotification(
                            new SubscriptionRejectedApplicationHookContext(
                                "application-id",
                                "api-id",
                                "plan-published",
                                "subscription-id",
                                USER_ID
                            )
                        ),
                        new ApplicationNotification(
                            new Recipient("EMAIL", "subscriber@mail.fake"),
                            new SubscriptionRejectedApplicationHookContext(
                                "application-id",
                                "api-id",
                                "plan-published",
                                "subscription-id",
                                USER_ID
                            )
                        )
                    );
            } else {
                assertThat(triggerNotificationDomainService.getApplicationNotifications())
                    .containsExactly(
                        new ApplicationNotification(
                            new SubscriptionRejectedApplicationHookContext(
                                "application-id",
                                "api-id",
                                "plan-published",
                                "subscription-id",
                                USER_ID
                            )
                        )
                    );
            }

            assertThat(auditCrudServiceInMemory.storage())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt", "patch")
                .containsExactly(
                    new AuditEntity(
                        "audit-id",
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
                        "audit-id",
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
    }

    private SubscriptionEntity givenExistingSubscription(SubscriptionEntity subscription) {
        subscriptionCrudService.initWith(List.of(subscription));
        return subscription;
    }

    private SubscriptionEntity reject(SubscriptionEntity subscription) {
        return cut.reject(subscription, "reason", AUDIT_INFO);
    }

    private SubscriptionEntity rejectById(SubscriptionEntity subscription) {
        return cut.reject(subscription.getId(), "reason", AUDIT_INFO);
    }
}
