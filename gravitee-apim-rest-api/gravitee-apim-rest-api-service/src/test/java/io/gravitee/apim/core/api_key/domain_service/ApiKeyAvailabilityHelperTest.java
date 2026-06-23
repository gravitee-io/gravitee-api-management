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

import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.common.utils.TimeProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class ApiKeyAvailabilityHelperTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");

    @BeforeAll
    static void beforeAll() {
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Test
    void isActiveForSubscription_should_be_true_when_key_and_subscription_are_active() {
        ApiKeyEntity apiKey = anApiKey().toBuilder().revoked(false).paused(false).expireAt(null).build();
        SubscriptionEntity subscription = aSubscription().toBuilder().status(SubscriptionEntity.Status.ACCEPTED).build();

        assertThat(ApiKeyAvailabilityHelper.isActiveForSubscription(apiKey, subscription)).isTrue();
    }

    @Test
    void isActiveForSubscription_should_be_false_when_key_is_revoked() {
        ApiKeyEntity apiKey = anApiKey().toBuilder().revoked(true).paused(false).expireAt(null).build();
        SubscriptionEntity subscription = aSubscription().toBuilder().status(SubscriptionEntity.Status.ACCEPTED).build();

        assertThat(ApiKeyAvailabilityHelper.isActiveForSubscription(apiKey, subscription)).isFalse();
    }

    @Test
    void isActiveForSubscription_should_be_false_when_subscription_is_closed() {
        ApiKeyEntity apiKey = anApiKey().toBuilder().revoked(false).paused(false).expireAt(null).build();
        SubscriptionEntity subscription = aSubscription().toBuilder().status(SubscriptionEntity.Status.CLOSED).build();

        assertThat(ApiKeyAvailabilityHelper.isActiveForSubscription(apiKey, subscription)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("activeKeyStateCases")
    void isActiveKeyState_should_reflect_key_state(boolean revoked, boolean paused, boolean expired, boolean expected) {
        assertThat(ApiKeyAvailabilityHelper.isActiveKeyState(revoked, paused, expired)).isEqualTo(expected);
    }

    private static Stream<Arguments> activeKeyStateCases() {
        return Stream.of(
            Arguments.of(false, false, false, true),
            Arguments.of(true, false, false, false),
            Arguments.of(false, true, false, false),
            Arguments.of(false, false, true, false)
        );
    }

    @Test
    void matchesReference_with_entity_should_match_api_product_reference() {
        SubscriptionEntity subscription = aSubscription()
            .toBuilder()
            .referenceId("product-id")
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .apiId(null)
            .build();

        assertThat(ApiKeyAvailabilityHelper.matchesReference(subscription, "product-id", "API_PRODUCT")).isTrue();
        assertThat(ApiKeyAvailabilityHelper.matchesReference(subscription, "other-product", "API_PRODUCT")).isFalse();
    }

    @Test
    void matchesReference_with_entity_should_fallback_to_api_id_when_reference_fields_are_null() {
        SubscriptionEntity subscription = aSubscription().toBuilder().referenceId(null).referenceType(null).apiId("api-id").build();

        assertThat(ApiKeyAvailabilityHelper.matchesReference(subscription, "api-id", "API")).isTrue();
        assertThat(ApiKeyAvailabilityHelper.matchesReference(subscription, "api-id", "API_PRODUCT")).isFalse();
    }

    @Test
    void matchesReference_with_values_should_match_explicit_reference() {
        assertThat(ApiKeyAvailabilityHelper.matchesReference("ref-id", "API", "legacy-api", "ref-id", "API")).isTrue();
        assertThat(ApiKeyAvailabilityHelper.matchesReference("ref-id", "API_PRODUCT", "legacy-api", "ref-id", "API")).isFalse();
    }

    @Test
    void matchesReference_with_values_should_fallback_to_api_id_for_api_reference_type() {
        assertThat(ApiKeyAvailabilityHelper.matchesReference(null, null, "api-id", "api-id", "API")).isTrue();
        assertThat(ApiKeyAvailabilityHelper.matchesReference(null, null, "api-id", "other-api", "API")).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = SubscriptionEntity.Status.class, names = { "ACCEPTED", "PAUSED" })
    void isActiveSubscriptionStatus_should_accept_active_statuses(SubscriptionEntity.Status status) {
        assertThat(ApiKeyAvailabilityHelper.isActiveSubscriptionStatus(status)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = SubscriptionEntity.Status.class, names = { "ACCEPTED", "PAUSED" }, mode = EnumSource.Mode.EXCLUDE)
    void isActiveSubscriptionStatus_should_reject_inactive_statuses(SubscriptionEntity.Status status) {
        assertThat(ApiKeyAvailabilityHelper.isActiveSubscriptionStatus(status)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("activeSubscriptionStatusNameCases")
    void isActiveSubscriptionStatusName_should_match_status_names(String status, boolean expected) {
        assertThat(ApiKeyAvailabilityHelper.isActiveSubscriptionStatusName(status)).isEqualTo(expected);
    }

    private static Stream<Arguments> activeSubscriptionStatusNameCases() {
        return Stream.of(
            Arguments.of("ACCEPTED", true),
            Arguments.of("PAUSED", true),
            Arguments.of("CLOSED", false),
            Arguments.of("PENDING", false),
            Arguments.of("UNKNOWN", false)
        );
    }

    @Test
    void isActiveForSubscription_should_be_false_when_key_is_expired() {
        ApiKeyEntity apiKey = anApiKey()
            .toBuilder()
            .revoked(false)
            .paused(false)
            .expireAt(ZonedDateTime.ofInstant(INSTANT_NOW.minusSeconds(60), ZoneId.systemDefault()))
            .build();
        SubscriptionEntity subscription = aSubscription().toBuilder().status(SubscriptionEntity.Status.ACCEPTED).build();

        assertThat(ApiKeyAvailabilityHelper.isActiveForSubscription(apiKey, subscription)).isFalse();
    }
}
