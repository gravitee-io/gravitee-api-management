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
package io.gravitee.apim.core.api_key.domain_service;

import static fixtures.core.model.AuditInfoFixtures.anAuditInfo;
import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.ApiKeyFixtures;
import fixtures.core.model.SubscriptionFixtures;
import inmemory.ApiKeyCrudServiceInMemory;
import inmemory.ApiKeyQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.SubscriptionCrudServiceInMemory;
import inmemory.TriggerNotificationDomainServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiKeyAuditEvent;
import io.gravitee.apim.core.notification.model.hook.ApiKeyRevokedApiHookContext;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RevokeApiKeyDomainServiceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private static final String SUBSCRIPTION_ID_1 = "subscription1";
    private static final String SUBSCRIPTION_ID_2 = "subscription2";
    private static final String API_ID_1 = "api1";
    private static final String API_ID_2 = "api2";
    private static final String PLAN_ID_1 = "plan1";
    private static final String PLAN_ID_2 = "plan2";
    private static final String APPLICATION_ID_1 = "app1";
    private static final String APPLICATION_ID_2 = "app2";

    ApiKeyCrudServiceInMemory apiKeyCrudService = new ApiKeyCrudServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    SubscriptionCrudServiceInMemory subscriptionCrudService = new SubscriptionCrudServiceInMemory();
    TriggerNotificationDomainServiceInMemory triggerNotificationDomainService = new TriggerNotificationDomainServiceInMemory();
    RevokeApiKeyDomainService service;

    @BeforeEach
    void setup() {
        UuidString.overrideGenerator(() -> "audit-id");

        service = new RevokeApiKeyDomainService(
            apiKeyCrudService,
            new ApiKeyQueryServiceInMemory(apiKeyCrudService),
            subscriptionCrudService,
            new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor()),
            triggerNotificationDomainService
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(apiKeyCrudService, auditCrudService, userCrudService).forEach(InMemoryAlternative::reset);
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
    }

    @Nested
    class RevokeAllSubscriptionsApiKeys {

        @SneakyThrows
        @Test
        void should_do_nothing_when_no_api_keys_for_subscription() {
            // Given no API key for subscription
            givenApiKeys(List.of());

            // When
            service.revokeAllSubscriptionsApiKeys(
                SubscriptionEntity.builder().id("subscription-id").apiId("api-id").applicationId("application-id").build(),
                AUDIT_INFO
            );

            // Then
            assertThat(auditCrudService.storage()).isEmpty();
        }

        @Test
        void should_do_nothing_when_no_active_api_keys_for_subscription() {
            // Given no API key for subscription
            givenApiKeys(
                List.of(
                    ApiKeyFixtures.anApiKey()
                        .toBuilder()
                        .subscriptions(List.of("subscription-id"))
                        .id("revoked-api-key")
                        .revoked(true)
                        .build(),
                    ApiKeyFixtures.anApiKey()
                        .toBuilder()
                        .subscriptions(List.of("subscription-id"))
                        .id("expired-api-key")
                        .expireAt(Instant.now().minus(1, ChronoUnit.DAYS).atZone(ZoneOffset.systemDefault()))
                        .build()
                )
            );

            // When
            service.revokeAllSubscriptionsApiKeys(
                SubscriptionEntity.builder().id("subscription-id").apiId("api-id").applicationId("application-id").build(),
                AUDIT_INFO
            );

            // Then
            // No modification
            assertThat(auditCrudService.storage()).isEmpty();
        }

        @Test
        void should_revoke_active_api_keys_for_subscription() {
            ZonedDateTime now = ZonedDateTime.now();
            givenApiKeys(
                List.of(
                    ApiKeyFixtures.anApiKey()
                        .toBuilder()
                        .subscriptions(List.of("subscription-id"))
                        .id("api-key")
                        .revoked(false)
                        .expireAt(Instant.now().plus(1, ChronoUnit.DAYS).atZone(ZoneOffset.systemDefault()))
                        .key("key")
                        .applicationId("application-id")
                        .build()
                )
            );

            // When
            var revokedKeys = service.revokeAllSubscriptionsApiKeys(
                SubscriptionEntity.builder().id("subscription-id").apiId("api-id").applicationId("application-id").build(),
                AUDIT_INFO
            );

            // Then
            assertThat(revokedKeys.size()).isOne();
            var revokedKey = revokedKeys.stream().findFirst();
            assertThat(revokedKey).isPresent();
            assertThat(revokedKey.get().isRevoked()).isTrue();
            assertThat(revokedKey.get().getRevokedAt()).isAfterOrEqualTo(now);

            assertThat(auditCrudService.storage())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt", "patch")
                .containsExactly(
                    AuditEntity.builder()
                        .id("audit-id")
                        .organizationId(ORGANIZATION_ID)
                        .environmentId(ENVIRONMENT_ID)
                        .referenceType(AuditEntity.AuditReferenceType.API)
                        .referenceId("api-id")
                        .user(USER_ID)
                        .properties(Map.of("APPLICATION", "application-id", "API", "api-id", "API_KEY", "key"))
                        .event(ApiKeyAuditEvent.APIKEY_REVOKED.name())
                        .build()
                );
        }

        @Test
        void should_create_an_audit_when_revoking_an_api_key() {
            givenApiKeys(
                List.of(
                    ApiKeyFixtures.anApiKey()
                        .toBuilder()
                        .subscriptions(List.of("subscription-id"))
                        .id("api-key")
                        .revoked(false)
                        .expireAt(Instant.now().plus(1, ChronoUnit.DAYS).atZone(ZoneOffset.systemDefault()))
                        .key("key")
                        .applicationId("application-id")
                        .build()
                )
            );

            // When
            service.revokeAllSubscriptionsApiKeys(
                SubscriptionEntity.builder().id("subscription-id").apiId("api-id").applicationId("application-id").build(),
                AUDIT_INFO
            );

            // Then
            assertThat(auditCrudService.storage())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt", "patch")
                .containsExactly(
                    AuditEntity.builder()
                        .id("audit-id")
                        .organizationId(ORGANIZATION_ID)
                        .environmentId(ENVIRONMENT_ID)
                        .referenceType(AuditEntity.AuditReferenceType.API)
                        .referenceId("api-id")
                        .user(USER_ID)
                        .properties(Map.of("APPLICATION", "application-id", "API", "api-id", "API_KEY", "key"))
                        .event(ApiKeyAuditEvent.APIKEY_REVOKED.name())
                        .build()
                );
        }
    }

    @Nested
    class Revoke {

        @Test
        void should_do_nothing_when_api_key_is_revoked() {
            // Given
            var apiKey = ApiKeyFixtures.anApiKey().toBuilder().revoked(true).build();

            // When
            var result = service.revoke(apiKey, AUDIT_INFO);

            // Then
            assertThat(auditCrudService.storage()).isEmpty();
            assertThat(result).isSameAs(apiKey);
        }

        @Test
        void should_do_nothing_when_api_key_is_expired() {
            // Given
            var apiKey = ApiKeyFixtures.anApiKey().toBuilder().expireAt(ZonedDateTime.now().minusSeconds(30)).build();

            // When
            var result = service.revoke(apiKey, AUDIT_INFO);

            // Then
            assertThat(auditCrudService.storage()).isEmpty();
            assertThat(result).isSameAs(apiKey);
        }

        @Test
        void should_revoke_api_key() {
            // Given
            var now = ZonedDateTime.now();
            var subscriptionIds = givenSubscriptions(List.of(SubscriptionFixtures.aSubscription(), SubscriptionFixtures.aSubscription()))
                .stream()
                .map(SubscriptionEntity::getId)
                .toList();
            var apiKey = givenApiKey(ApiKeyFixtures.anApiKey().toBuilder().subscriptions(subscriptionIds).build());

            // When
            var result = service.revoke(apiKey, AUDIT_INFO);

            // Then
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result.isRevoked()).describedAs("ApiKey is not revoked").isTrue();
                soft.assertThat(result.getRevokedAt()).isEqualTo(result.getUpdatedAt());
                soft.assertThat(result.getRevokedAt()).isAfterOrEqualTo(now);
            });
        }

        @Test
        void should_create_an_audit_for_each_subscriptions() {
            // Given
            var subscriptions = givenSubscriptions();
            var apiKey = givenApiKey(
                ApiKeyFixtures.anApiKey().toBuilder().subscriptions(subscriptions.stream().map(SubscriptionEntity::getId).toList()).build()
            );

            // When
            var result = service.revoke(apiKey, AUDIT_INFO);

            // Then
            assertThat(auditCrudService.storage())
                .hasSize(2)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
                .contains(
                    new AuditEntity(
                        "audit-id",
                        ORGANIZATION_ID,
                        ENVIRONMENT_ID,
                        AuditEntity.AuditReferenceType.API,
                        API_ID_2,
                        USER_ID,
                        Map.of("API_KEY", apiKey.getKey(), "API", API_ID_2, "APPLICATION", APPLICATION_ID_2),
                        ApiKeyAuditEvent.APIKEY_REVOKED.name(),
                        result.getRevokedAt(),
                        ""
                    ),
                    new AuditEntity(
                        "audit-id",
                        ORGANIZATION_ID,
                        ENVIRONMENT_ID,
                        AuditEntity.AuditReferenceType.API,
                        API_ID_1,
                        USER_ID,
                        Map.of("API_KEY", apiKey.getKey(), "API", API_ID_1, "APPLICATION", APPLICATION_ID_1),
                        ApiKeyAuditEvent.APIKEY_REVOKED.name(),
                        result.getRevokedAt(),
                        ""
                    )
                );
        }

        @Test
        void should_trigger_api_notification_for_each_subscription() {
            // Given
            var subscriptions = givenSubscriptions();
            var apiKey = givenApiKey(
                ApiKeyFixtures.anApiKey().toBuilder().subscriptions(subscriptions.stream().map(SubscriptionEntity::getId).toList()).build()
            );

            // When
            service.revoke(apiKey, AUDIT_INFO);

            // Then
            assertThat(triggerNotificationDomainService.getApiNotifications()).containsExactly(
                new ApiKeyRevokedApiHookContext(API_ID_1, APPLICATION_ID_1, PLAN_ID_1, apiKey.getKey()),
                new ApiKeyRevokedApiHookContext(API_ID_2, APPLICATION_ID_2, PLAN_ID_2, apiKey.getKey())
            );
        }
    }

    @SneakyThrows
    private void givenApiKeys(List<ApiKeyEntity> keys) {
        apiKeyCrudService.initWith(keys);
    }

    @SneakyThrows
    private ApiKeyEntity givenApiKey(ApiKeyEntity key) {
        apiKeyCrudService.initWith(List.of(key));
        return key;
    }

    private List<SubscriptionEntity> givenSubscriptions(List<SubscriptionEntity> subscriptions) {
        subscriptionCrudService.initWith(subscriptions);
        return subscriptions;
    }

    private List<SubscriptionEntity> givenSubscriptions() {
        var subscriptions = List.of(
            SubscriptionFixtures.aSubscription()
                .toBuilder()
                .id(SUBSCRIPTION_ID_1)
                .apiId(API_ID_1)
                .applicationId(APPLICATION_ID_1)
                .planId(PLAN_ID_1)
                .build(),
            SubscriptionFixtures.aSubscription()
                .toBuilder()
                .id(SUBSCRIPTION_ID_2)
                .apiId(API_ID_2)
                .applicationId(APPLICATION_ID_2)
                .planId(PLAN_ID_2)
                .build()
        );
        subscriptionCrudService.initWith(subscriptions);
        return subscriptions;
    }
}
