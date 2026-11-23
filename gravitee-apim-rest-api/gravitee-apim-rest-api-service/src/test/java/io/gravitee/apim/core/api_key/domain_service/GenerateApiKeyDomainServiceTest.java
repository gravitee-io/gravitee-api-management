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

import static assertions.CoreAssertions.assertThat;
import static fixtures.core.model.AuditInfoFixtures.anAuditInfo;
import static org.assertj.core.api.Assertions.catchThrowable;

import fixtures.core.model.ApiKeyFixtures;
import fixtures.core.model.SubscriptionFixtures;
import inmemory.ApiKeyCrudServiceInMemory;
import inmemory.ApiKeyQueryServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiKeyAuditEvent;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.ApiKeyAlreadyExistingException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class GenerateApiKeyDomainServiceTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final String SUBSCRIPTION_ID_1 = "subscription1";
    private static final String SUBSCRIPTION_ID_2 = "subscription2";
    private static final String API_ID_1 = "api1";
    private static final String PLAN_ID_1 = "plan1";
    private static final String APPLICATION_ID = "app1";

    private static final BaseApplicationEntity APPLICATION_1 = BaseApplicationEntity.builder().id(APPLICATION_ID).build();
    private static final SubscriptionEntity SUBSCRIPTION_1 = SubscriptionFixtures.aSubscription()
        .toBuilder()
        .id(SUBSCRIPTION_ID_1)
        .applicationId(APPLICATION_ID)
        .planId(PLAN_ID_1)
        .apiId(API_ID_1)
        .endingAt(Instant.parse("2024-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
        .build();
    private static final AuditInfo AUDIT_INFO = anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    ApiKeyCrudServiceInMemory apiKeyCrudService = new ApiKeyCrudServiceInMemory();
    ApplicationCrudServiceInMemory applicationCrudService = new ApplicationCrudServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();

    GenerateApiKeyDomainService service;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @BeforeEach
    void setup() {
        service = new GenerateApiKeyDomainService(
            apiKeyCrudService,
            new ApiKeyQueryServiceInMemory(apiKeyCrudService),
            applicationCrudService,
            new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor())
        );

        applicationCrudService.initWith(List.of(APPLICATION_1));
    }

    @AfterEach
    void tearDown() {
        Stream.of(apiKeyCrudService, auditCrudService, userCrudService).forEach(InMemoryAlternative::reset);
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void generate_new_key_when_not_custom_key_provided(String customKey) {
        // When
        service.generate(SUBSCRIPTION_1, AUDIT_INFO, customKey);

        // Then
        assertThat(apiKeyCrudService.storage()).contains(
            ApiKeyEntity.builder()
                .id("generated-id")
                .applicationId(APPLICATION_ID)
                .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                .key("generated-id")
                .subscriptions(List.of(SUBSCRIPTION_ID_1))
                .expireAt(SUBSCRIPTION_1.getEndingAt())
                .build()
        );
    }

    @Test
    void generate_key_using_custom_key_provided() {
        // When
        service.generate(SUBSCRIPTION_1, AUDIT_INFO, "custom-key");

        // Then
        assertThat(apiKeyCrudService.storage()).contains(
            ApiKeyEntity.builder()
                .id("generated-id")
                .applicationId(APPLICATION_ID)
                .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                .key("custom-key")
                .subscriptions(List.of(SUBSCRIPTION_ID_1))
                .expireAt(SUBSCRIPTION_1.getEndingAt())
                .build()
        );
    }

    @Test
    void should_create_an_audit() {
        // When
        var result = service.generate(SUBSCRIPTION_1, AUDIT_INFO, "custom-key");

        // Then
        assertThat(auditCrudService.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
            .contains(
                new AuditEntity(
                    "generated-id",
                    ORGANIZATION_ID,
                    ENVIRONMENT_ID,
                    AuditEntity.AuditReferenceType.API,
                    API_ID_1,
                    USER_ID,
                    Map.of("API_KEY", result.getId(), "API", API_ID_1, "APPLICATION", APPLICATION_ID),
                    ApiKeyAuditEvent.APIKEY_CREATED.name(),
                    result.getCreatedAt(),
                    ""
                )
            );
    }

    @Test
    void should_keep_the_api_key_value_out_of_the_audit() {
        var result = service.generate(SUBSCRIPTION_1, AUDIT_INFO, "custom-key");

        assertThat(result.getKey()).isEqualTo("custom-key");
        assertThat(auditCrudService.storage()).allSatisfy(audit -> {
            assertThat(audit.getProperties()).doesNotContainValue("custom-key");
            assertThat(audit.getPatch()).doesNotContain("custom-key");
        });
    }

    @Nested
    class WhenSharedApiKeyModeEnabled {

        private static final BaseApplicationEntity APPLICATION_SHARED = BaseApplicationEntity.builder()
            .id(APPLICATION_ID)
            .apiKeyMode(ApiKeyMode.SHARED)
            .build();

        @BeforeEach
        void setUp() {
            applicationCrudService.initWith(List.of(APPLICATION_SHARED));
        }

        @Test
        void add_subscription_to_existing_api_keys() {
            // Given
            var subscription = SUBSCRIPTION_1.toBuilder().applicationId(APPLICATION_SHARED.getId()).build();
            var keys = givenApiKeys(
                ApiKeyFixtures.anApiKey()
                    .toBuilder()
                    .id("key1")
                    .applicationId(APPLICATION_ID)
                    .subscriptions(List.of(SUBSCRIPTION_ID_2))
                    .key("existing-key-1")
                    .build(),
                ApiKeyFixtures.anApiKey()
                    .toBuilder()
                    .id("key2")
                    .applicationId(APPLICATION_ID)
                    .subscriptions(List.of(SUBSCRIPTION_ID_2))
                    .key("existing-key-2")
                    .build()
            );

            // When
            service.generate(subscription, AUDIT_INFO, null);

            // Then
            assertThat(apiKeyCrudService.storage()).containsOnly(
                keys
                    .get(0)
                    .toBuilder()
                    .subscriptions(List.of(SUBSCRIPTION_ID_2, SUBSCRIPTION_ID_1))
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build(),
                keys
                    .get(1)
                    .toBuilder()
                    .subscriptions(List.of(SUBSCRIPTION_ID_2, SUBSCRIPTION_ID_1))
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
        }

        @Test
        void attach_subscription_to_the_previous_and_the_renewed_key_during_the_renewal_overlap() {
            // Given a renewed shared key: the previous key still works for 2 hours, an older one was revoked
            var previousKey = ApiKeyFixtures.anApiKey()
                .toBuilder()
                .id("previous-key")
                .applicationId(APPLICATION_ID)
                .subscriptions(List.of(SUBSCRIPTION_ID_2))
                .key("previous-key-value")
                .expireAt(INSTANT_NOW.plusSeconds(7200).atZone(ZoneId.systemDefault()))
                .build();
            var renewedKey = previousKey.toBuilder().id("renewed-key").key("renewed-key-value").expireAt(null).build();
            var revokedKey = previousKey.toBuilder().id("revoked-key").key("revoked-key-value").revoked(true).build();
            givenApiKeys(previousKey, renewedKey, revokedKey);
            var subscription = SUBSCRIPTION_1.toBuilder().applicationId(APPLICATION_SHARED.getId()).build();

            // When
            var result = service.generate(subscription, AUDIT_INFO, null);

            // Then
            assertThat(result.getId()).isEqualTo("renewed-key");
            assertThat(apiKeyCrudService.storage()).hasSize(3);
            assertThat(apiKeyCrudService.storage())
                .filteredOn(key -> "revoked-key".equals(key.getId()))
                .singleElement()
                .satisfies(key -> assertThat(key.getSubscriptions()).containsExactly(SUBSCRIPTION_ID_2));
            assertThat(apiKeyCrudService.storage())
                .filteredOn(key -> !"revoked-key".equals(key.getId()))
                .allSatisfy(key -> assertThat(key.getSubscriptions()).containsExactlyInAnyOrder(SUBSCRIPTION_ID_2, SUBSCRIPTION_ID_1));
        }

        @Test
        void do_not_attach_subscription_to_a_revoked_key_that_shares_the_active_value() {
            var revoked = ApiKeyFixtures.anApiKey()
                .toBuilder()
                .id("revoked-key")
                .applicationId(APPLICATION_ID)
                .subscriptions(List.of("closed-orders"))
                .key("my-key")
                .revoked(true)
                .revokedAt(INSTANT_NOW.minusSeconds(3600).atZone(ZoneId.systemDefault()))
                .build();
            var active = revoked
                .toBuilder()
                .id("active-key")
                .subscriptions(List.of("customers"))
                .key("my-key")
                .revoked(false)
                .revokedAt(null)
                .build();
            givenApiKeys(revoked, active);
            var subscription = SUBSCRIPTION_1.toBuilder().applicationId(APPLICATION_SHARED.getId()).build();

            var result = service.generate(subscription, AUDIT_INFO, null);

            assertThat(result.getId()).isEqualTo("active-key");
            assertThat(apiKeyCrudService.storage())
                .filteredOn(key -> "revoked-key".equals(key.getId()))
                .singleElement()
                .satisfies(key -> {
                    assertThat(key.isRevoked()).isTrue();
                    assertThat(key.getSubscriptions()).containsExactly("closed-orders");
                });
            assertThat(apiKeyCrudService.storage())
                .filteredOn(key -> "active-key".equals(key.getId()))
                .singleElement()
                .satisfies(key -> assertThat(key.getSubscriptions()).containsExactlyInAnyOrder("customers", SUBSCRIPTION_ID_1));
        }

        @Test
        void still_attach_subscription_to_an_expired_key_while_a_live_key_exists() {
            var expired = ApiKeyFixtures.anApiKey()
                .toBuilder()
                .id("expired-key")
                .applicationId(APPLICATION_ID)
                .subscriptions(List.of(SUBSCRIPTION_ID_2))
                .key("previous-key-value")
                .revoked(false)
                .expireAt(INSTANT_NOW.minusSeconds(60).atZone(ZoneId.systemDefault()))
                .build();
            var live = expired.toBuilder().id("live-key").key("live-key-value").expireAt(null).build();
            var revoked = expired.toBuilder().id("revoked-key").key("revoked-key-value").revoked(true).expireAt(null).build();
            givenApiKeys(expired, live, revoked);
            var subscription = SUBSCRIPTION_1.toBuilder().applicationId(APPLICATION_SHARED.getId()).build();

            var result = service.generate(subscription, AUDIT_INFO, null);

            assertThat(result.getId()).isEqualTo("live-key");
            assertThat(apiKeyCrudService.storage())
                .filteredOn(key -> "expired-key".equals(key.getId()) || "live-key".equals(key.getId()))
                .allSatisfy(key -> assertThat(key.getSubscriptions()).containsExactlyInAnyOrder(SUBSCRIPTION_ID_2, SUBSCRIPTION_ID_1));
            assertThat(apiKeyCrudService.storage())
                .filteredOn(key -> "revoked-key".equals(key.getId()))
                .singleElement()
                .satisfies(key -> assertThat(key.getSubscriptions()).containsExactly(SUBSCRIPTION_ID_2));
        }

        @Test
        void attach_a_federated_subscription_to_revoked_keys_when_none_is_live() {
            var revoked = ApiKeyFixtures.anApiKey()
                .toBuilder()
                .id("revoked-key")
                .applicationId(APPLICATION_ID)
                .subscriptions(List.of(SUBSCRIPTION_ID_2))
                .key("old-key")
                .revoked(true)
                .revokedAt(INSTANT_NOW.minusSeconds(3600).atZone(ZoneId.systemDefault()))
                .build();
            givenApiKeys(revoked);
            var subscription = SUBSCRIPTION_1.toBuilder()
                .applicationId(APPLICATION_SHARED.getId())
                .referenceId("product-1")
                .referenceType(SubscriptionReferenceType.API_PRODUCT)
                .apiId(null)
                .build();

            var result = service.generateForFederated(subscription, AUDIT_INFO, "ignored-custom");

            assertThat(result.getId()).isEqualTo("revoked-key");
            assertThat(apiKeyCrudService.storage())
                .filteredOn(key -> "revoked-key".equals(key.getId()))
                .singleElement()
                .satisfies(key -> assertThat(key.getSubscriptions()).containsExactlyInAnyOrder(SUBSCRIPTION_ID_2, SUBSCRIPTION_ID_1));
        }

        @Test
        void generate_new_key_when_no_key_exists() {
            // Given
            var subscription = SUBSCRIPTION_1.toBuilder().applicationId(APPLICATION_SHARED.getId()).build();

            // When
            service.generate(subscription, AUDIT_INFO, "custom-key");

            // Then
            assertThat(apiKeyCrudService.storage()).containsOnly(
                ApiKeyEntity.builder()
                    .id("generated-id")
                    .applicationId(APPLICATION_ID)
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .key("custom-key")
                    .subscriptions(List.of(SUBSCRIPTION_ID_1))
                    .expireAt(SUBSCRIPTION_1.getEndingAt())
                    .build()
            );
        }
    }

    @Test
    void should_not_generate_when_custom_key_already_exists() {
        // Given
        givenApiKey(
            ApiKeyFixtures.anApiKey()
                .toBuilder()
                .applicationId(APPLICATION_ID)
                .subscriptions(List.of(SUBSCRIPTION_ID_1))
                .key("existing-key")
                .build()
        );

        // When
        var throwable = catchThrowable(() -> service.generate(SUBSCRIPTION_1, AUDIT_INFO, "existing-key"));

        // Then
        assertThat(throwable).isInstanceOf(ApiKeyAlreadyExistingException.class);
    }

    @Test
    void should_create_api_product_audit_when_subscription_is_api_product() {
        // Given
        var apiProductId = "product-1";
        var apiProductSubscription = SUBSCRIPTION_1.toBuilder()
            .referenceId(apiProductId)
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .apiId(null)
            .build();

        // When
        service.generate(apiProductSubscription, AUDIT_INFO, "custom-key");

        // Then
        assertThat(auditCrudService.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "patch")
            .anyMatch(
                audit ->
                    audit.getReferenceType() == AuditEntity.AuditReferenceType.API_PRODUCT &&
                    apiProductId.equals(audit.getReferenceId()) &&
                    audit.getProperties().containsKey("API_PRODUCT") &&
                    apiProductId.equals(audit.getProperties().get("API_PRODUCT")) &&
                    ApiKeyAuditEvent.APIKEY_CREATED.name().equals(audit.getEvent())
            );
    }

    @Test
    void generateForFederated_creates_federated_api_key() {
        var apiProductSubscription = SUBSCRIPTION_1.toBuilder()
            .referenceId("product-1")
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .apiId(null)
            .build();

        service.generateForFederated(apiProductSubscription, AUDIT_INFO, "federated-key");

        assertThat(apiKeyCrudService.storage()).anyMatch(
            key -> "federated-key".equals(key.getKey()) && key.getSubscriptions().contains(SUBSCRIPTION_ID_1)
        );
    }

    @SneakyThrows
    private List<ApiKeyEntity> givenApiKeys(ApiKeyEntity... keys) {
        List<ApiKeyEntity> list = Arrays.asList(keys);
        apiKeyCrudService.initWith(list);
        return list;
    }

    @SneakyThrows
    private void givenApiKey(ApiKeyEntity key) {
        apiKeyCrudService.initWith(List.of(key));
    }
}
