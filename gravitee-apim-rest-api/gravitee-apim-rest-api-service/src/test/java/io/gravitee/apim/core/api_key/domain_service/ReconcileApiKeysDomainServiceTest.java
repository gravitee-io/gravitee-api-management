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

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.AuditInfoFixtures;
import inmemory.ApiKeyCrudServiceInMemory;
import inmemory.ApiKeyQueryServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import inmemory.TriggerNotificationDomainServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.crd.ApiKeyCRDSpec;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ReconcileApiKeysDomainServiceTest {

    private static final Instant THEN = Instant.parse("2023-10-22T10:15:30Z");
    private static final ZonedDateTime FIXED_NOW = ZonedDateTime.ofInstant(THEN, ZoneId.systemDefault());

    private static final String ORGANIZATION_ID = "org-id";
    private static final String ENVIRONMENT_ID = "env-id";
    private static final String USER_ID = "user-id";
    private static final String SUBSCRIPTION_ID = "subscription-id";
    private static final String APPLICATION_ID = "application-id";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    private static final SubscriptionEntity SUBSCRIPTION = SubscriptionEntity.builder()
        .id(SUBSCRIPTION_ID)
        .applicationId(APPLICATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .planId("plan-id")
        .status(SubscriptionEntity.Status.ACCEPTED)
        .build();

    private final ApiKeyCrudServiceInMemory apiKeyCrudService = new ApiKeyCrudServiceInMemory();
    private final ApiKeyQueryServiceInMemory apiKeyQueryService = new ApiKeyQueryServiceInMemory(apiKeyCrudService);
    private final SubscriptionCrudServiceInMemory subscriptionCrudService = new SubscriptionCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final TriggerNotificationDomainServiceInMemory notificationService = new TriggerNotificationDomainServiceInMemory();
    private final ApplicationCrudServiceInMemory applicationCrudService = new ApplicationCrudServiceInMemory();

    private ReconcileApiKeysDomainService cut;

    @BeforeAll
    static void init() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(THEN, ZoneId.systemDefault()));
    }

    @BeforeEach
    void setUp() {
        var auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var generateApiKeyDomainService = new GenerateApiKeyDomainService(
            apiKeyCrudService,
            new ApiKeyQueryServiceInMemory(apiKeyCrudService),
            applicationCrudService,
            auditDomainService
        );
        var revokeApiKeyDomainService = new RevokeApiKeyDomainService(
            apiKeyCrudService,
            apiKeyQueryService,
            subscriptionCrudService,
            auditDomainService,
            notificationService
        );

        cut = new ReconcileApiKeysDomainService(
            apiKeyQueryService,
            apiKeyCrudService,
            generateApiKeyDomainService,
            revokeApiKeyDomainService
        );

        applicationCrudService.initWith(
            List.of(fixtures.ApplicationModelFixtures.anApplicationEntity().toBuilder().id(APPLICATION_ID).build())
        );
        subscriptionCrudService.initWith(List.of(SUBSCRIPTION));
    }

    @AfterEach
    void tearDown() {
        apiKeyCrudService.reset();
        subscriptionCrudService.reset();
        auditCrudService.reset();
        userCrudService.reset();
        notificationService.reset();
        applicationCrudService.reset();
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Test
    void should_do_nothing_when_desired_keys_is_null() {
        cut.reconcile(SUBSCRIPTION, null, AUDIT_INFO);

        assertThat(apiKeyCrudService.storage()).isEmpty();
    }

    @Test
    void should_do_nothing_when_desired_keys_is_empty() {
        cut.reconcile(SUBSCRIPTION, List.of(), AUDIT_INFO);

        assertThat(apiKeyCrudService.storage()).isEmpty();
    }

    @Nested
    class CreateKeys {

        @Test
        void should_create_key_not_present_in_apim() {
            var desired = List.of(ApiKeyCRDSpec.builder().key("key-v1").build());

            cut.reconcile(SUBSCRIPTION, desired, AUDIT_INFO);

            assertThat(apiKeyCrudService.storage()).hasSize(1);
            assertThat(apiKeyCrudService.storage().get(0).getKey()).isEqualTo("key-v1");
            assertThat(apiKeyCrudService.storage().get(0).isRevoked()).isFalse();
        }

        @Test
        void should_create_key_with_expire_at() {
            var expiry = FIXED_NOW.plusDays(30);
            var desired = List.of(ApiKeyCRDSpec.builder().key("key-v1").expireAt(expiry).build());

            cut.reconcile(SUBSCRIPTION, desired, AUDIT_INFO);

            assertThat(apiKeyCrudService.storage()).hasSize(1);
            assertThat(apiKeyCrudService.storage().get(0).getKey()).isEqualTo("key-v1");
            assertThat(apiKeyCrudService.storage().get(0).getExpireAt()).isEqualTo(expiry);
        }

        @Test
        void should_create_multiple_keys() {
            var desired = List.of(ApiKeyCRDSpec.builder().key("key-v1").build(), ApiKeyCRDSpec.builder().key("key-v2").build());

            cut.reconcile(SUBSCRIPTION, desired, AUDIT_INFO);

            assertThat(apiKeyCrudService.storage()).hasSize(2);
            assertThat(apiKeyCrudService.storage()).extracting(ApiKeyEntity::getKey).containsExactlyInAnyOrder("key-v1", "key-v2");
        }
    }

    @Nested
    class RevokeKeys {

        @Test
        void should_revoke_active_key_not_in_spec() {
            givenExistingActiveKey("key-old", null);

            var desired = List.of(ApiKeyCRDSpec.builder().key("key-new").build());

            cut.reconcile(SUBSCRIPTION, desired, AUDIT_INFO);

            var oldKey = apiKeyCrudService
                .storage()
                .stream()
                .filter(k -> k.getKey().equals("key-old"))
                .findFirst()
                .orElseThrow();
            assertThat(oldKey.isRevoked()).isTrue();
            assertThat(oldKey.getRevokedAt()).isNotNull();
        }

        @Test
        void should_not_revoke_already_revoked_key_not_in_spec() {
            givenExistingRevokedKey("key-old");

            var desired = List.of(ApiKeyCRDSpec.builder().key("key-new").build());

            cut.reconcile(SUBSCRIPTION, desired, AUDIT_INFO);

            var oldKey = apiKeyCrudService
                .storage()
                .stream()
                .filter(k -> k.getKey().equals("key-old"))
                .findFirst()
                .orElseThrow();
            assertThat(oldKey.isRevoked()).isTrue();
        }
    }

    @Nested
    class ReactivateKeys {

        @Test
        void should_reactivate_revoked_key_present_in_spec() {
            givenExistingRevokedKey("key-v1");

            var desired = List.of(ApiKeyCRDSpec.builder().key("key-v1").build());

            cut.reconcile(SUBSCRIPTION, desired, AUDIT_INFO);

            var key = apiKeyCrudService
                .storage()
                .stream()
                .filter(k -> k.getKey().equals("key-v1"))
                .findFirst()
                .orElseThrow();
            assertThat(key.isRevoked()).isFalse();
            assertThat(key.getRevokedAt()).isNull();
        }

        @Test
        void should_reactivate_and_set_expire_at() {
            givenExistingRevokedKey("key-v1");

            var expiry = FIXED_NOW.plusDays(30);
            var desired = List.of(ApiKeyCRDSpec.builder().key("key-v1").expireAt(expiry).build());

            cut.reconcile(SUBSCRIPTION, desired, AUDIT_INFO);

            var key = apiKeyCrudService
                .storage()
                .stream()
                .filter(k -> k.getKey().equals("key-v1"))
                .findFirst()
                .orElseThrow();
            assertThat(key.isRevoked()).isFalse();
            assertThat(key.getExpireAt()).isEqualTo(expiry);
        }
    }

    @Nested
    class UpdateExpiry {

        @Test
        void should_update_expire_at_when_changed() {
            var oldExpiry = FIXED_NOW.plusDays(10);
            givenExistingActiveKey("key-v1", oldExpiry);

            var newExpiry = FIXED_NOW.plusDays(60);
            var desired = List.of(ApiKeyCRDSpec.builder().key("key-v1").expireAt(newExpiry).build());

            cut.reconcile(SUBSCRIPTION, desired, AUDIT_INFO);

            var key = apiKeyCrudService
                .storage()
                .stream()
                .filter(k -> k.getKey().equals("key-v1"))
                .findFirst()
                .orElseThrow();
            assertThat(key.getExpireAt()).isEqualTo(newExpiry);
        }

        @Test
        void should_not_update_when_expire_at_unchanged() {
            var expiry = FIXED_NOW.plusDays(10);
            givenExistingActiveKey("key-v1", expiry);

            var desired = List.of(ApiKeyCRDSpec.builder().key("key-v1").expireAt(expiry).build());

            cut.reconcile(SUBSCRIPTION, desired, AUDIT_INFO);

            var key = apiKeyCrudService
                .storage()
                .stream()
                .filter(k -> k.getKey().equals("key-v1"))
                .findFirst()
                .orElseThrow();
            assertThat(key.getExpireAt()).isEqualTo(expiry);
        }

        @Test
        void should_clear_expire_at_when_removed_from_spec() {
            var expiry = FIXED_NOW.plusDays(10);
            givenExistingActiveKey("key-v1", expiry);

            var desired = List.of(ApiKeyCRDSpec.builder().key("key-v1").expireAt(null).build());

            cut.reconcile(SUBSCRIPTION, desired, AUDIT_INFO);

            var key = apiKeyCrudService
                .storage()
                .stream()
                .filter(k -> k.getKey().equals("key-v1"))
                .findFirst()
                .orElseThrow();
            assertThat(key.getExpireAt()).isNull();
        }
    }

    @Nested
    class FullRotationScenario {

        @Test
        void should_create_new_and_revoke_old_in_single_reconciliation() {
            givenExistingActiveKey("key-v1", null);

            var desired = List.of(ApiKeyCRDSpec.builder().key("key-v2").build());

            cut.reconcile(SUBSCRIPTION, desired, AUDIT_INFO);

            var oldKey = apiKeyCrudService
                .storage()
                .stream()
                .filter(k -> k.getKey().equals("key-v1"))
                .findFirst()
                .orElseThrow();
            assertThat(oldKey.isRevoked()).isTrue();

            var newKey = apiKeyCrudService
                .storage()
                .stream()
                .filter(k -> k.getKey().equals("key-v2"))
                .findFirst()
                .orElseThrow();
            assertThat(newKey.isRevoked()).isFalse();
        }

        @Test
        void should_support_gradual_rotation_with_overlap() {
            givenExistingActiveKey("key-v1", null);

            var desired = List.of(
                ApiKeyCRDSpec.builder().key("key-v1").expireAt(FIXED_NOW.plusHours(2)).build(),
                ApiKeyCRDSpec.builder().key("key-v2").build()
            );

            cut.reconcile(SUBSCRIPTION, desired, AUDIT_INFO);

            var oldKey = apiKeyCrudService
                .storage()
                .stream()
                .filter(k -> k.getKey().equals("key-v1"))
                .findFirst()
                .orElseThrow();
            assertThat(oldKey.isRevoked()).isFalse();
            assertThat(oldKey.getExpireAt()).isEqualTo(FIXED_NOW.plusHours(2));

            var newKey = apiKeyCrudService
                .storage()
                .stream()
                .filter(k -> k.getKey().equals("key-v2"))
                .findFirst()
                .orElseThrow();
            assertThat(newKey.isRevoked()).isFalse();
        }

        @Test
        void should_handle_complete_key_replacement_revoking_multiple_old_keys() {
            givenExistingActiveKey("key-v1", null);
            givenExistingActiveKey("key-v2", null);

            var desired = List.of(ApiKeyCRDSpec.builder().key("key-v3").build());

            cut.reconcile(SUBSCRIPTION, desired, AUDIT_INFO);

            assertThat(
                apiKeyCrudService
                    .storage()
                    .stream()
                    .filter(k -> k.getKey().equals("key-v1"))
                    .findFirst()
                    .orElseThrow()
                    .isRevoked()
            ).isTrue();
            assertThat(
                apiKeyCrudService
                    .storage()
                    .stream()
                    .filter(k -> k.getKey().equals("key-v2"))
                    .findFirst()
                    .orElseThrow()
                    .isRevoked()
            ).isTrue();
            assertThat(
                apiKeyCrudService
                    .storage()
                    .stream()
                    .filter(k -> k.getKey().equals("key-v3"))
                    .findFirst()
                    .orElseThrow()
                    .isRevoked()
            ).isFalse();
        }

        @Test
        void should_be_idempotent_when_desired_matches_actual() {
            givenExistingActiveKey("key-v1", null);
            givenExistingActiveKey("key-v2", null);

            var desired = List.of(ApiKeyCRDSpec.builder().key("key-v1").build(), ApiKeyCRDSpec.builder().key("key-v2").build());

            cut.reconcile(SUBSCRIPTION, desired, AUDIT_INFO);

            assertThat(apiKeyCrudService.storage()).hasSize(2);
            assertThat(apiKeyCrudService.storage()).allMatch(k -> !k.isRevoked());
        }
    }

    private void givenExistingActiveKey(String key, ZonedDateTime expireAt) {
        apiKeyCrudService.create(
            ApiKeyEntity.builder()
                .id("id-" + key)
                .key(key)
                .applicationId(APPLICATION_ID)
                .subscriptions(List.of(SUBSCRIPTION_ID))
                .environmentId(ENVIRONMENT_ID)
                .createdAt(FIXED_NOW)
                .updatedAt(FIXED_NOW)
                .expireAt(expireAt)
                .revoked(false)
                .build()
        );
    }

    private void givenExistingRevokedKey(String key) {
        apiKeyCrudService.create(
            ApiKeyEntity.builder()
                .id("id-" + key)
                .key(key)
                .applicationId(APPLICATION_ID)
                .subscriptions(List.of(SUBSCRIPTION_ID))
                .environmentId(ENVIRONMENT_ID)
                .createdAt(FIXED_NOW)
                .updatedAt(FIXED_NOW)
                .revoked(true)
                .revokedAt(FIXED_NOW)
                .build()
        );
    }
}
