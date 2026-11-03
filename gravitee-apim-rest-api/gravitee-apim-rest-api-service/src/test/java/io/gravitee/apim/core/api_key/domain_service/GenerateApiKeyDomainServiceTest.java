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
import inmemory.ParametersQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiKeyAuditEvent;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.ApiKeyAlreadyExistingException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
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
    private static final String FIRST_SHARED_KEY_ID = UUID.nameUUIDFromBytes(
        ("shared-api-key:" + APPLICATION_ID).getBytes(StandardCharsets.UTF_8)
    ).toString();

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

    ApiKeyCrudServiceInMemory apiKeyCrudService = new ApiKeyCrudServiceInMemory(true);
    ApplicationCrudServiceInMemory applicationCrudService = new ApplicationCrudServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();

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
            new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor()),
            parametersQueryService
        );

        applicationCrudService.initWith(List.of(APPLICATION_1));
    }

    @AfterEach
    void tearDown() {
        Stream.of(apiKeyCrudService, auditCrudService, userCrudService, parametersQueryService).forEach(InMemoryAlternative::reset);
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
        void keep_subscriptions_attached_to_the_shared_key_after_it_was_read() {
            // Given another Management API node attached SUBSCRIPTION_ID_2 after this node read the shared key
            var sharedKey = ApiKeyFixtures.anApiKey()
                .toBuilder()
                .id("shared-key")
                .applicationId(APPLICATION_ID)
                .subscriptions(List.of("seed-subscription", SUBSCRIPTION_ID_2))
                .key("shared-key-value")
                .build();
            givenApiKeys(sharedKey);
            var keyAsReadBeforeTheOtherNodeWrote = sharedKey.toBuilder().subscriptions(List.of("seed-subscription")).build();
            var subscription = SUBSCRIPTION_1.toBuilder().applicationId(APPLICATION_SHARED.getId()).build();

            // When
            var result = serviceReading(keyAsReadBeforeTheOtherNodeWrote).generate(subscription, AUDIT_INFO, null);

            // Then the stored key and the returned one carry the link written by the other node
            assertThat(apiKeyCrudService.storage())
                .singleElement()
                .satisfies(key ->
                    assertThat(key.getSubscriptions()).containsExactlyInAnyOrder("seed-subscription", SUBSCRIPTION_ID_2, SUBSCRIPTION_ID_1)
                );
            assertThat(result.getId()).isEqualTo("shared-key");
            assertThat(result.getSubscriptions()).containsExactlyInAnyOrder("seed-subscription", SUBSCRIPTION_ID_2, SUBSCRIPTION_ID_1);
        }

        @Test
        void move_the_updatedAt_of_the_shared_key_when_attaching_a_subscription() {
            // Given a shared key last changed long ago: gateways only fetch the keys whose updatedAt moved since their last sync
            var sharedKey = ApiKeyFixtures.anApiKey()
                .toBuilder()
                .id("shared-key")
                .applicationId(APPLICATION_ID)
                .subscriptions(List.of(SUBSCRIPTION_ID_2))
                .build();
            givenApiKeys(sharedKey);
            var subscription = SUBSCRIPTION_1.toBuilder().applicationId(APPLICATION_SHARED.getId()).build();

            // When
            var result = service.generate(subscription, AUDIT_INFO, null);

            // Then
            var now = INSTANT_NOW.atZone(ZoneId.systemDefault());
            assertThat(sharedKey.getUpdatedAt()).isBefore(now);
            assertThat(apiKeyCrudService.storage())
                .singleElement()
                .satisfies(key -> assertThat(key.getUpdatedAt()).isEqualTo(now));
            assertThat(result.getUpdatedAt()).isEqualTo(now);
        }

        @Test
        void attach_the_subscription_to_a_shared_key_stored_without_a_list_of_subscriptions() {
            // Given a shared key stored with no list of subscriptions, which the repositories treat as an empty one
            var sharedKey = ApiKeyFixtures.anApiKey()
                .toBuilder()
                .id("shared-key")
                .applicationId(APPLICATION_ID)
                .subscriptions(null)
                .build();
            givenApiKeys(sharedKey);
            var subscription = SUBSCRIPTION_1.toBuilder().applicationId(APPLICATION_SHARED.getId()).build();

            // When
            var result = service.generate(subscription, AUDIT_INFO, null);

            // Then
            assertThat(result.getId()).isEqualTo("shared-key");
            assertThat(apiKeyCrudService.storage())
                .singleElement()
                .satisfies(key -> assertThat(key.getSubscriptions()).containsExactly(SUBSCRIPTION_ID_1));
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
            assertThat(apiKeyCrudService.storage())
                .hasSize(3)
                .allSatisfy(key -> assertThat(key.getSubscriptions()).containsExactlyInAnyOrder(SUBSCRIPTION_ID_2, SUBSCRIPTION_ID_1));
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
                    .id(FIRST_SHARED_KEY_ID)
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
        void give_the_first_shared_key_an_id_derived_from_the_application_and_a_random_value() {
            var subscription = SUBSCRIPTION_1.toBuilder().applicationId(APPLICATION_SHARED.getId()).build();

            var result = service.generate(subscription, AUDIT_INFO, null);

            assertThat(result.getId()).isEqualTo(FIRST_SHARED_KEY_ID);
            assertThat(apiKeyCrudService.storage())
                .singleElement()
                .satisfies(key -> {
                    assertThat(key.getId()).isEqualTo(FIRST_SHARED_KEY_ID);
                    assertThat(key.getKey()).isEqualTo("generated-id");
                    assertThat(key.getSubscriptions()).containsExactly(SUBSCRIPTION_ID_1);
                });
        }

        @Test
        void link_the_subscription_to_the_shared_key_another_node_created_since_the_keys_were_read() {
            // Given another Management API node created the first shared key after this node found no key
            var createdByAnotherNode = ApiKeyFixtures.anApiKey()
                .toBuilder()
                .id(FIRST_SHARED_KEY_ID)
                .applicationId(APPLICATION_ID)
                .subscriptions(List.of(SUBSCRIPTION_ID_2))
                .key("value-from-the-other-node")
                .build();
            givenApiKeys(createdByAnotherNode);
            var subscription = SUBSCRIPTION_1.toBuilder().applicationId(APPLICATION_SHARED.getId()).build();

            // When
            var result = serviceThatFoundNoKey().generate(subscription, AUDIT_INFO, null);

            // Then the existing key is returned and linked, no other key is created
            assertThat(result.getId()).isEqualTo(FIRST_SHARED_KEY_ID);
            assertThat(result.getKey()).isEqualTo("value-from-the-other-node");
            assertThat(apiKeyCrudService.storage())
                .singleElement()
                .satisfies(key -> {
                    assertThat(key.getKey()).isEqualTo("value-from-the-other-node");
                    assertThat(key.getSubscriptions()).containsExactlyInAnyOrder(SUBSCRIPTION_ID_2, SUBSCRIPTION_ID_1);
                });
        }

        @Test
        void fail_when_the_first_shared_key_cannot_be_created_and_the_application_has_no_key() {
            // Given the id of the first shared key is taken, but not by a key of this application (should never happen)
            givenApiKeys(
                ApiKeyFixtures.anApiKey()
                    .toBuilder()
                    .id(FIRST_SHARED_KEY_ID)
                    .applicationId("another-application")
                    .subscriptions(List.of(SUBSCRIPTION_ID_2))
                    .build()
            );
            var subscription = SUBSCRIPTION_1.toBuilder().applicationId(APPLICATION_SHARED.getId()).build();

            // When
            var throwable = catchThrowable(() -> service.generate(subscription, AUDIT_INFO, null));

            // Then
            assertThat(throwable).isInstanceOf(TechnicalDomainException.class).hasMessageContaining(APPLICATION_ID);
            assertThat(apiKeyCrudService.storage())
                .singleElement()
                .satisfies(key -> assertThat(key.getSubscriptions()).containsExactly(SUBSCRIPTION_ID_2));
        }

        @Test
        @SneakyThrows
        void create_a_single_shared_key_for_first_subscriptions_handled_at_the_same_time_by_two_nodes() {
            // Given two Management API nodes sharing one store, receiving the first subscriptions of the application together
            var randomIds = new AtomicInteger();
            UuidString.overrideGenerator(() -> "random-" + randomIds.incrementAndGet());
            var subscriptions = IntStream.rangeClosed(1, 6)
                .mapToObj(i -> SUBSCRIPTION_1.toBuilder().id("subscription-" + i).applicationId(APPLICATION_SHARED.getId()).build())
                .toList();
            var everyoneReadTheKeys = new CountDownLatch(subscriptions.size());
            var audits = new SynchronizedAuditCrudService();
            var nodes = List.of(nodeReadingTogether(everyoneReadTheKeys, audits), nodeReadingTogether(everyoneReadTheKeys, audits));
            var executor = Executors.newFixedThreadPool(subscriptions.size());
            var returnedKeys = new ArrayList<ApiKeyEntity>();

            try {
                // When
                var results = new ArrayList<Future<ApiKeyEntity>>();
                for (int i = 0; i < subscriptions.size(); i++) {
                    var node = nodes.get(i % nodes.size());
                    var subscription = subscriptions.get(i);
                    results.add(executor.submit(() -> node.generate(subscription, AUDIT_INFO, null)));
                }
                for (var result : results) {
                    returnedKeys.add(result.get(30, TimeUnit.SECONDS));
                }
            } finally {
                executor.shutdownNow();
                UuidString.overrideGenerator(() -> "generated-id");
            }

            // Then a single key is created, linked to every subscription, and every request returns it
            assertThat(apiKeyCrudService.storage())
                .singleElement()
                .satisfies(key -> {
                    assertThat(key.getId()).isEqualTo(FIRST_SHARED_KEY_ID);
                    assertThat(key.getSubscriptions()).containsExactlyInAnyOrderElementsOf(
                        subscriptions.stream().map(SubscriptionEntity::getId).toList()
                    );
                    assertThat(returnedKeys)
                        .hasSize(subscriptions.size())
                        .allSatisfy(returned -> {
                            assertThat(returned.getId()).isEqualTo(FIRST_SHARED_KEY_ID);
                            assertThat(returned.getKey()).isEqualTo(key.getKey());
                        });
                });
            assertThat(audits.storage())
                .filteredOn(audit -> ApiKeyAuditEvent.APIKEY_CREATED.name().equals(audit.getEvent()))
                .hasSize(1);
        }

        /** A node that first read the keys of the application before another node created the first one. */
        private GenerateApiKeyDomainService serviceThatFoundNoKey() {
            var firstReadBeforeTheOtherNodeCreatedTheKey = new ApiKeyQueryServiceInMemory(apiKeyCrudService) {
                private final AtomicBoolean firstRead = new AtomicBoolean(true);

                @Override
                public Stream<ApiKeyEntity> findByApplication(String applicationId) {
                    return firstRead.getAndSet(false) ? Stream.empty() : super.findByApplication(applicationId);
                }
            };
            return new GenerateApiKeyDomainService(
                apiKeyCrudService,
                firstReadBeforeTheOtherNodeCreatedTheKey,
                applicationCrudService,
                new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor()),
                parametersQueryService
            );
        }

        /** A node of its own whose requests go on only once all of them have read the keys of the application; later reads don't wait. */
        private GenerateApiKeyDomainService nodeReadingTogether(CountDownLatch everyoneReadTheKeys, AuditCrudServiceInMemory audits) {
            var readingTogether = new ApiKeyQueryServiceInMemory(apiKeyCrudService) {
                @Override
                @SneakyThrows
                public Stream<ApiKeyEntity> findByApplication(String applicationId) {
                    var keys = super.findByApplication(applicationId).toList();
                    everyoneReadTheKeys.countDown();
                    // A request that missed the others would not race them: fail rather than pass without racing
                    assertThat(everyoneReadTheKeys.await(30, TimeUnit.SECONDS)).isTrue();
                    return keys.stream();
                }
            };
            return new GenerateApiKeyDomainService(
                apiKeyCrudService,
                readingTogether,
                applicationCrudService,
                new AuditDomainService(audits, userCrudService, new JacksonJsonDiffProcessor()),
                parametersQueryService
            );
        }
    }

    @Test
    void keep_random_ids_for_the_keys_of_an_application_not_in_shared_mode() {
        var randomIds = new AtomicInteger();
        UuidString.overrideGenerator(() -> "random-" + randomIds.incrementAndGet());
        try {
            var first = service.generate(SUBSCRIPTION_1, AUDIT_INFO, null);
            var second = service.generate(SUBSCRIPTION_1.toBuilder().id(SUBSCRIPTION_ID_2).build(), AUDIT_INFO, null);

            assertThat(first.getId()).startsWith("random-").isNotEqualTo(second.getId());
            assertThat(second.getId()).startsWith("random-");
            assertThat(apiKeyCrudService.storage())
                .hasSize(2)
                .noneMatch(key -> FIRST_SHARED_KEY_ID.equals(key.getId()));
        } finally {
            UuidString.overrideGenerator(() -> "generated-id");
        }
    }

    /** The audits are written by concurrent requests. */
    private static class SynchronizedAuditCrudService extends AuditCrudServiceInMemory {

        @Override
        public synchronized void create(AuditEntity auditEntity) {
            super.create(auditEntity);
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

    @Test
    void should_not_reuse_revoked_custom_key_when_flag_disabled() {
        // Given a revoked key with the same value, but the reuse flag is disabled (default)
        givenApiKey(
            ApiKeyFixtures.anApiKey()
                .toBuilder()
                .applicationId(APPLICATION_ID)
                .subscriptions(List.of(SUBSCRIPTION_ID_1))
                .key("reusable-key")
                .revoked(true)
                .build()
        );

        // When
        var throwable = catchThrowable(() ->
            service.generate(SUBSCRIPTION_1.toBuilder().id(SUBSCRIPTION_ID_2).build(), AUDIT_INFO, "reusable-key")
        );

        // Then
        assertThat(throwable).isInstanceOf(ApiKeyAlreadyExistingException.class);
    }

    @Nested
    class WhenCustomKeyReuseEnabled {

        private static final SubscriptionEntity NEW_SUBSCRIPTION = SUBSCRIPTION_1.toBuilder().id(SUBSCRIPTION_ID_2).build();

        @BeforeEach
        void setUp() {
            parametersQueryService.define(
                Parameter.builder().key(Key.PLAN_SECURITY_APIKEY_CUSTOM_REUSE_ALLOWED.key()).value("true").build()
            );
        }

        @Test
        void reactivate_and_repoint_existing_revoked_key() {
            // Given a revoked key (e.g. its subscription was closed)
            givenApiKey(
                ApiKeyFixtures.anApiKey()
                    .toBuilder()
                    .id("existing-key-id")
                    .applicationId(APPLICATION_ID)
                    .subscriptions(List.of(SUBSCRIPTION_ID_1))
                    .key("reusable-key")
                    .revoked(true)
                    .revokedAt(INSTANT_NOW.minusSeconds(3600).atZone(ZoneId.systemDefault()))
                    .build()
            );

            // When
            var result = service.generate(NEW_SUBSCRIPTION, AUDIT_INFO, "reusable-key");

            // Then: a single record is reactivated, repointed and its expiry aligned with the new subscription
            assertThat(apiKeyCrudService.storage()).hasSize(1);
            assertThat(result.getId()).isEqualTo("existing-key-id");
            assertThat(result.isRevoked()).isFalse();
            assertThat(result.getRevokedAt()).isNull();
            assertThat(result.getSubscriptions()).containsExactly(SUBSCRIPTION_ID_1, SUBSCRIPTION_ID_2);
            assertThat(result.getExpireAt()).isEqualTo(NEW_SUBSCRIPTION.getEndingAt());
        }

        @Test
        void keep_links_written_to_the_reused_key_after_it_was_read() {
            // Given a revoked key that got another link after this node read it
            var storedKey = ApiKeyFixtures.anApiKey()
                .toBuilder()
                .id("existing-key-id")
                .applicationId(APPLICATION_ID)
                .subscriptions(List.of(SUBSCRIPTION_ID_1, "subscription-linked-meanwhile"))
                .key("reusable-key")
                .revoked(true)
                .revokedAt(INSTANT_NOW.minusSeconds(3600).atZone(ZoneId.systemDefault()))
                .build();
            givenApiKey(storedKey);
            var keyAsRead = storedKey.toBuilder().subscriptions(List.of(SUBSCRIPTION_ID_1)).build();

            // When
            serviceReading(keyAsRead).generate(NEW_SUBSCRIPTION, AUDIT_INFO, "reusable-key");

            // Then the stored key is reactivated and keeps every link
            assertThat(apiKeyCrudService.storage())
                .singleElement()
                .satisfies(key -> {
                    assertThat(key.isRevoked()).isFalse();
                    assertThat(key.getRevokedAt()).isNull();
                    assertThat(key.getExpireAt()).isEqualTo(NEW_SUBSCRIPTION.getEndingAt());
                    assertThat(key.getSubscriptions()).containsExactlyInAnyOrder(
                        SUBSCRIPTION_ID_1,
                        "subscription-linked-meanwhile",
                        SUBSCRIPTION_ID_2
                    );
                });
        }

        @Test
        void reactivate_existing_expired_key() {
            // Given an expired (but not revoked) key
            givenApiKey(
                ApiKeyFixtures.anApiKey()
                    .toBuilder()
                    .applicationId(APPLICATION_ID)
                    .subscriptions(List.of(SUBSCRIPTION_ID_1))
                    .key("reusable-key")
                    .revoked(false)
                    .expireAt(INSTANT_NOW.minusSeconds(3600).atZone(ZoneId.systemDefault()))
                    .build()
            );

            // When
            var result = service.generate(NEW_SUBSCRIPTION, AUDIT_INFO, "reusable-key");

            // Then
            assertThat(apiKeyCrudService.storage()).hasSize(1);
            assertThat(result.isExpired()).isFalse();
            assertThat(result.getExpireAt()).isEqualTo(NEW_SUBSCRIPTION.getEndingAt());
        }

        @Test
        void emit_reactivated_audit_on_reuse() {
            // Given
            givenApiKey(
                ApiKeyFixtures.anApiKey()
                    .toBuilder()
                    .applicationId(APPLICATION_ID)
                    .subscriptions(List.of(SUBSCRIPTION_ID_1))
                    .key("reusable-key")
                    .revoked(true)
                    .build()
            );

            // When
            service.generate(NEW_SUBSCRIPTION, AUDIT_INFO, "reusable-key");

            // Then
            assertThat(auditCrudService.storage()).anyMatch(audit -> ApiKeyAuditEvent.APIKEY_REACTIVATED.name().equals(audit.getEvent()));
        }

        @Test
        void reject_reuse_when_existing_key_is_still_active() {
            // Given an active key (default fixture is not revoked and expires in 2051)
            givenApiKey(
                ApiKeyFixtures.anApiKey()
                    .toBuilder()
                    .applicationId(APPLICATION_ID)
                    .subscriptions(List.of(SUBSCRIPTION_ID_1))
                    .key("reusable-key")
                    .build()
            );

            // When
            var throwable = catchThrowable(() -> service.generate(NEW_SUBSCRIPTION, AUDIT_INFO, "reusable-key"));

            // Then
            assertThat(throwable).isInstanceOf(ApiKeyAlreadyExistingException.class);
        }

        @Test
        void reject_reuse_when_existing_key_is_only_paused() {
            // Given a paused key: its subscription is still active, so the key must not be reused
            givenApiKey(
                ApiKeyFixtures.anApiKey()
                    .toBuilder()
                    .applicationId(APPLICATION_ID)
                    .subscriptions(List.of(SUBSCRIPTION_ID_1))
                    .key("reusable-key")
                    .revoked(false)
                    .paused(true)
                    .build()
            );

            // When
            var throwable = catchThrowable(() -> service.generate(NEW_SUBSCRIPTION, AUDIT_INFO, "reusable-key"));

            // Then
            assertThat(throwable).isInstanceOf(ApiKeyAlreadyExistingException.class);
        }
    }

    /**
     * A service whose reads return the given (possibly outdated) copies while writes go to {@link #apiKeyCrudService}, like a
     * Management API node that read the keys before another node changed them.
     */
    private GenerateApiKeyDomainService serviceReading(ApiKeyEntity... keysAsRead) {
        var outdatedReads = new ApiKeyQueryServiceInMemory();
        outdatedReads.initWith(Arrays.asList(keysAsRead));
        return new GenerateApiKeyDomainService(
            apiKeyCrudService,
            outdatedReads,
            applicationCrudService,
            new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor()),
            parametersQueryService
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
