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

import static fixtures.core.model.ApiKeyFixtures.anApiKey;
import static fixtures.core.model.SubscriptionFixtures.aSubscription;
import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.ApiKeyFixtures;
import inmemory.ApiKeyQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.SubscriptionCrudServiceInMemory;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.common.utils.TimeProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomApiKeyAvailabilityDomainServiceTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String APPLICATION_ID = "application-id";
    private static final String OTHER_APPLICATION_ID = "other-application-id";
    private static final String API_ID = "api-id";
    private static final String API_PRODUCT_ID = "product-id";
    private static final String CUSTOM_KEY = "custom-key";

    ApiKeyQueryServiceInMemory apiKeyQueryService;
    SubscriptionCrudServiceInMemory subscriptionCrudService;
    CustomApiKeyAvailabilityDomainService service;

    @BeforeAll
    static void beforeAll() {
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        apiKeyQueryService = new ApiKeyQueryServiceInMemory();
        subscriptionCrudService = new SubscriptionCrudServiceInMemory();
        service = new CustomApiKeyAvailabilityDomainService(apiKeyQueryService, subscriptionCrudService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(apiKeyQueryService, subscriptionCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_allow_reuse_when_prior_key_is_revoked_and_subscription_closed() {
        var closedSubscription = subscription(
            "subscription-closed",
            API_ID,
            SubscriptionReferenceType.API,
            SubscriptionEntity.Status.CLOSED
        );
        subscriptionCrudService.initWith(List.of(closedSubscription));
        apiKeyQueryService.initWith(
            List.of(inactiveApiKey("key-id-old", CUSTOM_KEY, APPLICATION_ID, "subscription-closed").toBuilder().revoked(true).build())
        );

        assertThat(canUseCustomKey(CUSTOM_KEY, API_ID, SubscriptionReferenceType.API.name(), APPLICATION_ID)).isTrue();
    }

    @Test
    void should_block_when_active_key_exists_for_same_application_and_api() {
        var activeSubscription = subscription(
            "subscription-active",
            API_ID,
            SubscriptionReferenceType.API,
            SubscriptionEntity.Status.ACCEPTED
        );
        subscriptionCrudService.initWith(List.of(activeSubscription));
        apiKeyQueryService.initWith(List.of(activeApiKey("key-id-active", CUSTOM_KEY, APPLICATION_ID, "subscription-active")));

        assertThat(canUseCustomKey(CUSTOM_KEY, API_ID, SubscriptionReferenceType.API.name(), APPLICATION_ID)).isFalse();
    }

    @Test
    void should_block_when_key_belongs_to_another_application() {
        var activeSubscription = subscription(
            "subscription-other-app",
            API_ID,
            SubscriptionReferenceType.API,
            SubscriptionEntity.Status.ACCEPTED
        );
        subscriptionCrudService.initWith(List.of(activeSubscription));
        apiKeyQueryService.initWith(List.of(activeApiKey("key-id-other-app", CUSTOM_KEY, OTHER_APPLICATION_ID, "subscription-other-app")));

        assertThat(canUseCustomKey(CUSTOM_KEY, API_ID, SubscriptionReferenceType.API.name(), APPLICATION_ID)).isFalse();
    }

    @Test
    void should_block_when_active_key_exists_for_same_api_product() {
        var activeSubscription = subscription(
            "subscription-product",
            API_PRODUCT_ID,
            SubscriptionReferenceType.API_PRODUCT,
            SubscriptionEntity.Status.ACCEPTED
        );
        subscriptionCrudService.initWith(List.of(activeSubscription));
        apiKeyQueryService.initWith(List.of(activeApiKey("key-id-product", CUSTOM_KEY, APPLICATION_ID, "subscription-product")));

        assertThat(canUseCustomKey(CUSTOM_KEY, API_PRODUCT_ID, SubscriptionReferenceType.API_PRODUCT.name(), APPLICATION_ID)).isFalse();
    }

    @Test
    void should_allow_reuse_on_other_api_for_same_application() {
        var activeSubscription = subscription(
            "subscription-other-api",
            "other-api",
            SubscriptionReferenceType.API,
            SubscriptionEntity.Status.ACCEPTED
        );
        subscriptionCrudService.initWith(List.of(activeSubscription));
        apiKeyQueryService.initWith(List.of(activeApiKey("key-id-other-api", CUSTOM_KEY, APPLICATION_ID, "subscription-other-api")));

        assertThat(canUseCustomKey(CUSTOM_KEY, API_ID, SubscriptionReferenceType.API.name(), APPLICATION_ID)).isTrue();
    }

    @Test
    void should_allow_reuse_when_prior_key_is_expired_and_subscription_closed() {
        var closedSubscription = subscription(
            "subscription-closed",
            API_ID,
            SubscriptionReferenceType.API,
            SubscriptionEntity.Status.CLOSED
        );
        subscriptionCrudService.initWith(List.of(closedSubscription));
        var expiredKey = inactiveApiKey("key-id-expired", CUSTOM_KEY, APPLICATION_ID, "subscription-closed")
            .toBuilder()
            .expireAt(INSTANT_NOW.minusSeconds(3600).atZone(ZoneId.systemDefault()))
            .build();
        apiKeyQueryService.initWith(List.of(expiredKey));

        assertThat(canUseCustomKey(CUSTOM_KEY, API_ID, SubscriptionReferenceType.API.name(), APPLICATION_ID)).isTrue();
    }

    @Test
    void should_allow_reuse_when_prior_key_is_paused_and_subscription_closed() {
        var closedSubscription = subscription(
            "subscription-closed",
            API_ID,
            SubscriptionReferenceType.API,
            SubscriptionEntity.Status.CLOSED
        );
        subscriptionCrudService.initWith(List.of(closedSubscription));
        var pausedKey = inactiveApiKey("key-id-paused", CUSTOM_KEY, APPLICATION_ID, "subscription-closed").toBuilder().paused(true).build();
        apiKeyQueryService.initWith(List.of(pausedKey));

        assertThat(canUseCustomKey(CUSTOM_KEY, API_ID, SubscriptionReferenceType.API.name(), APPLICATION_ID)).isTrue();
    }

    @Test
    void should_block_when_active_key_exists_among_mixed_state_candidates() {
        var closedSubscription = subscription(
            "subscription-closed",
            API_ID,
            SubscriptionReferenceType.API,
            SubscriptionEntity.Status.CLOSED
        );
        var activeSubscription = subscription(
            "subscription-active",
            API_ID,
            SubscriptionReferenceType.API,
            SubscriptionEntity.Status.ACCEPTED
        );
        subscriptionCrudService.initWith(List.of(closedSubscription, activeSubscription));
        apiKeyQueryService.initWith(
            List.of(
                inactiveApiKey("key-id-old", CUSTOM_KEY, APPLICATION_ID, "subscription-closed").toBuilder().revoked(true).build(),
                activeApiKey("key-id-active", CUSTOM_KEY, APPLICATION_ID, "subscription-active")
            )
        );

        assertThat(canUseCustomKey(CUSTOM_KEY, API_ID, SubscriptionReferenceType.API.name(), APPLICATION_ID)).isFalse();
    }

    @Test
    void should_allow_reuse_when_active_subscription_exists_but_key_is_revoked() {
        var activeSubscription = subscription(
            "subscription-active",
            API_ID,
            SubscriptionReferenceType.API,
            SubscriptionEntity.Status.ACCEPTED
        );
        subscriptionCrudService.initWith(List.of(activeSubscription));
        apiKeyQueryService.initWith(
            List.of(inactiveApiKey("key-id-revoked", CUSTOM_KEY, APPLICATION_ID, "subscription-active").toBuilder().revoked(true).build())
        );

        assertThat(canUseCustomKey(CUSTOM_KEY, API_ID, SubscriptionReferenceType.API.name(), APPLICATION_ID)).isTrue();
    }

    @Test
    void should_allow_reuse_when_active_subscription_exists_but_key_is_expired() {
        var activeSubscription = subscription(
            "subscription-active",
            API_ID,
            SubscriptionReferenceType.API,
            SubscriptionEntity.Status.ACCEPTED
        );
        subscriptionCrudService.initWith(List.of(activeSubscription));
        var expiredKey = inactiveApiKey("key-id-expired", CUSTOM_KEY, APPLICATION_ID, "subscription-active")
            .toBuilder()
            .expireAt(INSTANT_NOW.minusSeconds(3600).atZone(ZoneId.systemDefault()))
            .build();
        apiKeyQueryService.initWith(List.of(expiredKey));

        assertThat(canUseCustomKey(CUSTOM_KEY, API_ID, SubscriptionReferenceType.API.name(), APPLICATION_ID)).isTrue();
    }

    @Test
    void should_allow_reuse_when_active_subscription_exists_but_key_is_paused() {
        var activeSubscription = subscription(
            "subscription-active",
            API_ID,
            SubscriptionReferenceType.API,
            SubscriptionEntity.Status.ACCEPTED
        );
        subscriptionCrudService.initWith(List.of(activeSubscription));
        apiKeyQueryService.initWith(
            List.of(inactiveApiKey("key-id-paused", CUSTOM_KEY, APPLICATION_ID, "subscription-active").toBuilder().paused(true).build())
        );

        assertThat(canUseCustomKey(CUSTOM_KEY, API_ID, SubscriptionReferenceType.API.name(), APPLICATION_ID)).isTrue();
    }

    @Test
    void should_allow_reuse_when_subscription_is_closed_even_if_key_is_not_revoked() {
        var closedSubscription = subscription(
            "subscription-closed",
            API_ID,
            SubscriptionReferenceType.API,
            SubscriptionEntity.Status.CLOSED
        );
        subscriptionCrudService.initWith(List.of(closedSubscription));
        apiKeyQueryService.initWith(List.of(activeApiKey("key-id-active", CUSTOM_KEY, APPLICATION_ID, "subscription-closed")));

        assertThat(canUseCustomKey(CUSTOM_KEY, API_ID, SubscriptionReferenceType.API.name(), APPLICATION_ID)).isTrue();
    }

    private boolean canUseCustomKey(String keyValue, String referenceId, String referenceType, String applicationId) {
        return service.canUseCustomKey(keyValue, referenceId, referenceType, applicationId, ENVIRONMENT_ID);
    }

    private static SubscriptionEntity subscription(
        String id,
        String referenceId,
        SubscriptionReferenceType referenceType,
        SubscriptionEntity.Status status
    ) {
        return aSubscription()
            .toBuilder()
            .id(id)
            .applicationId(APPLICATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .apiId(referenceType == SubscriptionReferenceType.API ? referenceId : null)
            .referenceId(referenceId)
            .referenceType(referenceType)
            .status(status)
            .build();
    }

    private static ApiKeyEntity activeApiKey(String id, String key, String applicationId, String subscriptionId) {
        return anApiKey()
            .toBuilder()
            .id(id)
            .key(key)
            .applicationId(applicationId)
            .environmentId(ENVIRONMENT_ID)
            .subscriptions(List.of(subscriptionId))
            .revoked(false)
            .paused(false)
            .build();
    }

    private static ApiKeyEntity inactiveApiKey(String id, String key, String applicationId, String subscriptionId) {
        return ApiKeyFixtures.anApiKey()
            .toBuilder()
            .id(id)
            .key(key)
            .applicationId(applicationId)
            .environmentId(ENVIRONMENT_ID)
            .subscriptions(List.of(subscriptionId))
            .build();
    }
}
