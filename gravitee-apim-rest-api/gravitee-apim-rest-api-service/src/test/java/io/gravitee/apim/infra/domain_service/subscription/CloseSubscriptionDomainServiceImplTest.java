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
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

import inmemory.*;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.event.SubscriptionAuditEvent;
import io.gravitee.apim.core.notification.model.hook.SubscriptionClosedApiHookContext;
import io.gravitee.apim.core.notification.model.hook.SubscriptionClosedApplicationHookContext;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.domain_service.RejectSubscriptionDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.apim.infra.domain_service.audit.AuditDomainServiceImpl;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CloseSubscriptionDomainServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private TriggerNotificationDomainServiceInMemory triggerNotificationService;

    private AuditCrudServiceInMemory auditCrudServiceInMemory;
    private CloseSubscriptionDomainService service;
    private ApplicationCrudServiceInMemory applicationCrudService;
    private RevokeApiKeyDomainServiceInMemory revokeApiKeyDomainService;

    private RejectSubscriptionDomainService rejectSubscriptionDomainService;

    @BeforeEach
    void setUp() {
        UuidString.overrideGenerator(() -> "audit-id");

        GraviteeContext.setCurrentOrganization("organization-id");
        GraviteeContext.setCurrentEnvironment("environment-id");

        triggerNotificationService = new TriggerNotificationDomainServiceInMemory();

        auditCrudServiceInMemory = new AuditCrudServiceInMemory();
        applicationCrudService = new ApplicationCrudServiceInMemory();
        var auditDomainService = new AuditDomainServiceImpl(
            auditCrudServiceInMemory,
            new UserCrudServiceInMemory(),
            GraviteeJacksonMapper.getInstance()
        );
        revokeApiKeyDomainService = new RevokeApiKeyDomainServiceInMemory();
        rejectSubscriptionDomainService = new RejectSubscriptionDomainServiceInMemory();
        service =
            new CloseSubscriptionDomainServiceImpl(
                subscriptionRepository,
                rejectSubscriptionDomainService,
                triggerNotificationService,
                auditDomainService,
                applicationCrudService,
                revokeApiKeyDomainService
            );

        allowSubscriptionRepositorySave();
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    void should_throw_when_close_unknown_subscription() {
        // Given no subscription
        Throwable throwable = catchThrowable(() ->
            service.closeSubscription(
                GraviteeContext.getExecutionContext(),
                "subscription-id",
                AuditActor.builder().userId("user-id").build()
            )
        );
        assertThat(throwable).isInstanceOf(SubscriptionNotFoundException.class).hasMessageContaining("subscription-id");
    }

    @ParameterizedTest
    @EnumSource(value = Subscription.Status.class, names = { "CLOSED", "REJECTED" })
    void should_throw_when_close_subscription(Subscription.Status status) {
        // Given
        givenExistingSubscription(
            Subscription.builder().id("subscription-id").application("application-id").api("api-id").plan("plan-id").status(status).build()
        );

        Throwable throwable = catchThrowable(() ->
            service.closeSubscription(
                GraviteeContext.getExecutionContext(),
                "subscription-id",
                AuditActor.builder().userId("user-id").build()
            )
        );
        assertThat(throwable)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot close subscription with status " + status.name());
    }

    @Test
    void should_reject_pending_subscription() {
        // Given
        givenExistingSubscription(
            Subscription
                .builder()
                .id("subscription-id")
                .application("application-id")
                .api("api-id")
                .plan("plan-id")
                .status(Subscription.Status.PENDING)
                .build()
        );

        final AuditActor currentUser = AuditActor.builder().userId("user-id").build();
        final SubscriptionEntity expecpectedSubscriptionEntity = SubscriptionEntity
            .builder()
            .id("subscription-id")
            .status(SubscriptionEntity.Status.REJECTED)
            .build();

        // When
        final SubscriptionEntity result = service.closeSubscription(GraviteeContext.getExecutionContext(), "subscription-id", currentUser);

        // Then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getId()).isEqualTo(expecpectedSubscriptionEntity.getId());
            softly.assertThat(result.getStatus()).isEqualTo(expecpectedSubscriptionEntity.getStatus());
        });
    }

    @ParameterizedTest
    @EnumSource(value = Subscription.Status.class, names = { "ACCEPTED", "PAUSED" })
    void should_close_accepted_or_paused_subscription_and_not_revoke_keys_for_application_in_shared_api_key_mode(
        Subscription.Status status
    ) {
        // Given
        givenExistingSubscription(
            Subscription.builder().id("subscription-id").application("application-id").api("api-id").plan("plan-id").status(status).build()
        );
        givenExistingApplication(BaseApplicationEntity.builder().id("application-id").apiKeyMode(ApiKeyMode.SHARED).build());

        // When
        final SubscriptionEntity result = service.closeSubscription(
            GraviteeContext.getExecutionContext(),
            "subscription-id",
            AuditActor.builder().userId("user-id").build()
        );

        // Then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getId()).isEqualTo("subscription-id");
            softly.assertThat(result.getStatus()).isEqualTo(SubscriptionEntity.Status.CLOSED);
        });
        assertThat(triggerNotificationService.getApiNotifications())
            .containsExactly(new SubscriptionClosedApiHookContext("api-id", "application-id", "plan-id"));

        assertThat(triggerNotificationService.getApplicationNotifications())
            .containsExactly(new SubscriptionClosedApplicationHookContext("application-id", "api-id", "plan-id"));

        assertThat(triggerNotificationService.getApplicationEmailNotifications()).isEmpty();

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
                    SubscriptionAuditEvent.SUBSCRIPTION_CLOSED.name(),
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
                    SubscriptionAuditEvent.SUBSCRIPTION_CLOSED.name(),
                    ZonedDateTime.now(),
                    ""
                )
            );
    }

    @Test
    void should_revoke_keys_if_application_not_in_shared_api_key_mode() {
        // Given
        var now = new Date();
        givenExistingSubscription(
            Subscription
                .builder()
                .id("subscription-id")
                .application("application-id")
                .api("api-id")
                .plan("plan-id")
                .status(Subscription.Status.ACCEPTED)
                .build()
        );
        givenExistingApplication(BaseApplicationEntity.builder().id("application-id").apiKeyMode(ApiKeyMode.EXCLUSIVE).build());
        givenExistingApiKeysForSubscription(
            "subscription-id",
            Set.of(
                ApiKeyEntity
                    .builder()
                    .id("api-key-id")
                    .key("api-key")
                    .revoked(false)
                    .expireAt(new Date(Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli()))
                    .build()
            )
        );

        // When
        final SubscriptionEntity result = service.closeSubscription(
            GraviteeContext.getExecutionContext(),
            "subscription-id",
            AuditActor.builder().userId("user-id").build()
        );

        // Then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getId()).isEqualTo("subscription-id");
            softly.assertThat(result.getStatus()).isEqualTo(SubscriptionEntity.Status.CLOSED);
        });
        var revokedKeys = revokeApiKeyDomainService.getApiKeysBySubscriptionId("subscription-id");
        assertThat(revokedKeys).hasSize(1);
        var revokedKey = revokedKeys.stream().findFirst().get();
        assertThat(revokedKey.getId()).isEqualTo("api-key-id");
        assertThat(revokedKey.getKey()).isEqualTo("api-key");
        assertThat(revokedKey.isRevoked()).isTrue();
        assertThat(revokedKey.getRevokedAt()).isAfterOrEqualTo(now);
    }

    @SneakyThrows
    private void givenExistingApiKeysForSubscription(String subscriptionId, Set<ApiKeyEntity> apiKeys) {
        revokeApiKeyDomainService.initWith(Map.of(subscriptionId, Set.copyOf(apiKeys)));
    }

    private void givenExistingApplication(BaseApplicationEntity application) {
        applicationCrudService.initWith(List.of(application));
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
