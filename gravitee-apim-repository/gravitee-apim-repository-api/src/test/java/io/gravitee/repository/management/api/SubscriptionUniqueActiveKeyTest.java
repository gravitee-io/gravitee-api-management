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
package io.gravitee.repository.management.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Two subscriptions get the same key exactly when the subscription creation would reject the second one with
 * {@code plan.subscribed}: same reference, application and plan, the subscription still being active, and for PUSH
 * plans the same channel.
 */
class SubscriptionUniqueActiveKeyTest {

    @Test
    void should_identify_an_active_subscription_by_its_reference_application_and_plan() {
        var subscription = aSubscription("sub-1").build();

        assertThat(SubscriptionUniqueActiveKey.of(subscription)).isEqualTo("API:api-1:app-1:plan-1");
    }

    @Test
    void should_identify_an_api_product_subscription_by_its_api_product() {
        var subscription = aSubscription("sub-1")
            .api(null)
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .referenceId("product-1")
            .build();

        assertThat(SubscriptionUniqueActiveKey.of(subscription)).isEqualTo("API_PRODUCT:product-1:app-1:plan-1");
    }

    @ParameterizedTest
    @EnumSource(value = Subscription.Status.class, names = { "PENDING", "ACCEPTED", "PAUSED" })
    void should_give_a_key_to_every_subscription_blocking_a_new_one(Subscription.Status status) {
        var subscription = aSubscription("sub-1").status(status).build();

        assertThat(SubscriptionUniqueActiveKey.of(subscription)).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(value = Subscription.Status.class, names = { "REJECTED", "CLOSED" })
    void should_not_give_a_key_to_a_rejected_or_closed_subscription(Subscription.Status status) {
        var subscription = aSubscription("sub-1").status(status).build();

        assertThat(SubscriptionUniqueActiveKey.of(subscription)).isNull();
    }

    @Test
    void should_hash_the_key_into_64_lowercase_hexadecimal_characters() {
        var subscription = aSubscription("sub-1").build();

        // sha256("API:api-1:app-1:plan-1")
        assertThat(SubscriptionUniqueActiveKey.hashOf(subscription))
            .hasSize(64)
            .isEqualTo("939db7ee18b7ecaae934b931891f40ec681f32f5e96cb2ebc7f2621a015caa4c");
    }

    @Test
    void should_not_hash_anything_for_a_subscription_without_key() {
        var closed = aSubscription("sub-1").status(Subscription.Status.CLOSED).build();

        assertThat(SubscriptionUniqueActiveKey.hashOf(closed)).isNull();
    }

    @Test
    void should_give_the_same_key_to_two_active_subscriptions_of_an_application_to_the_same_plan() {
        var accepted = aSubscription("sub-1").status(Subscription.Status.ACCEPTED).build();
        var pending = aSubscription("sub-2").status(Subscription.Status.PENDING).build();

        assertThat(SubscriptionUniqueActiveKey.of(pending)).isEqualTo(SubscriptionUniqueActiveKey.of(accepted));
    }

    @Test
    void should_give_different_keys_to_subscriptions_of_other_applications_or_plans() {
        var subscription = SubscriptionUniqueActiveKey.of(aSubscription("sub-1").build());

        assertThat(SubscriptionUniqueActiveKey.of(aSubscription("sub-2").application("app-2").build())).isNotEqualTo(subscription);
        assertThat(SubscriptionUniqueActiveKey.of(aSubscription("sub-3").plan("plan-2").build())).isNotEqualTo(subscription);
    }

    @Test
    void should_use_the_api_of_a_legacy_subscription_without_reference() {
        var legacy = aSubscription("sub-1").referenceType(null).referenceId(null).api("api-1").build();

        assertThat(SubscriptionUniqueActiveKey.of(legacy)).isEqualTo(SubscriptionUniqueActiveKey.of(aSubscription("sub-2").build()));
    }

    @Test
    void should_not_give_a_key_to_a_subscription_without_reference_application_or_plan() {
        assertThat(SubscriptionUniqueActiveKey.of(aSubscription("sub-1").referenceId(null).api(null).build())).isNull();
        assertThat(SubscriptionUniqueActiveKey.of(aSubscription("sub-2").application(null).build())).isNull();
        assertThat(SubscriptionUniqueActiveKey.of(aSubscription("sub-3").plan(null).build())).isNull();
    }

    @Test
    void should_ignore_the_configuration_of_a_standard_subscription() {
        var onChannelA = aSubscription("sub-1").configuration("{\"channel\":\"a\"}").build();
        var onChannelB = aSubscription("sub-2").configuration("{\"channel\":\"b\"}").build();
        var withoutConfiguration = aSubscription("sub-3").configuration(null).build();

        assertThat(SubscriptionUniqueActiveKey.of(onChannelB)).isEqualTo(SubscriptionUniqueActiveKey.of(onChannelA));
        assertThat(SubscriptionUniqueActiveKey.of(withoutConfiguration)).isEqualTo(SubscriptionUniqueActiveKey.of(onChannelA));
    }

    @Nested
    class PushSubscription {

        @Test
        void should_give_the_same_key_to_two_push_subscriptions_without_configuration() {
            assertThat(keyOfPush("sub-1", null)).isEqualTo(keyOfPush("sub-2", null));
        }

        @Test
        void should_give_the_same_key_to_two_push_subscriptions_on_the_same_channel() {
            assertThat(keyOfPush("sub-1", "{\"entrypointId\":\"webhook\",\"channel\":\"orders\"}")).isEqualTo(
                keyOfPush("sub-2", "{\"channel\":\"orders\",\"entrypointConfiguration\":{\"callbackUrl\":\"https://x\"}}")
            );
        }

        @Test
        void should_give_the_same_key_to_two_push_subscriptions_without_channel() {
            assertThat(keyOfPush("sub-1", "{\"entrypointId\":\"webhook\"}")).isEqualTo(keyOfPush("sub-2", "{\"channel\":null}"));
        }

        @Test
        void should_give_different_keys_to_push_subscriptions_on_different_channels() {
            assertThat(keyOfPush("sub-1", "{\"channel\":\"orders\"}")).isNotEqualTo(keyOfPush("sub-2", "{\"channel\":\"invoices\"}"));
        }

        @Test
        void should_give_different_keys_when_only_one_push_subscription_has_a_configuration() {
            assertThat(keyOfPush("sub-1", null)).isNotEqualTo(keyOfPush("sub-2", "{\"entrypointId\":\"webhook\"}"));
        }

        @Test
        void should_give_different_keys_to_a_channel_and_no_channel() {
            assertThat(keyOfPush("sub-1", "{\"channel\":\"orders\"}")).isNotEqualTo(keyOfPush("sub-2", "{\"entrypointId\":\"webhook\"}"));
        }

        @Test
        void should_compare_channels_the_way_the_configuration_is_read() {
            // The configuration is read into SubscriptionConfigurationEntity: a scalar channel becomes a string.
            assertThat(keyOfPush("sub-1", "{\"channel\":1}")).isEqualTo(keyOfPush("sub-2", "{\"channel\":\"1\"}"));
            // Unknown properties are ignored.
            assertThat(keyOfPush("sub-3", "{\"channel\":\"a\",\"unknown\":true}")).isEqualTo(keyOfPush("sub-4", "{\"channel\":\"a\"}"));
        }

        @Test
        void should_never_give_an_unreadable_configuration_the_key_of_another_subscription() {
            assertThat(keyOfPush("sub-1", "not json")).isNotNull().isNotEqualTo(keyOfPush("sub-2", "not json"));
            assertThat(keyOfPush("sub-3", "")).isNotEqualTo(keyOfPush("sub-4", ""));
            assertThat(keyOfPush("sub-5", "null")).isNotEqualTo(keyOfPush("sub-6", "null"));
            assertThat(keyOfPush("sub-7", "{\"channel\":{\"name\":\"a\"}}")).isNotEqualTo(
                keyOfPush("sub-8", "{\"channel\":{\"name\":\"a\"}}")
            );
            assertThat(keyOfPush("sub-9", "{\"entrypointId\":[\"a\"]}")).isNotEqualTo(keyOfPush("sub-10", "{\"entrypointId\":[\"a\"]}"));
        }

        @Test
        void should_keep_the_key_within_the_column_size_whatever_the_channel_length() {
            var longChannel = aSubscriptionWithLongIds().configuration("{\"channel\":\"" + "c".repeat(5000) + "\"}").build();
            var unreadable = aSubscriptionWithLongIds().configuration("not json").build();

            assertThat(SubscriptionUniqueActiveKey.of(longChannel)).hasSizeLessThanOrEqualTo(SubscriptionUniqueActiveKey.MAX_LENGTH);
            assertThat(SubscriptionUniqueActiveKey.of(unreadable)).hasSizeLessThanOrEqualTo(SubscriptionUniqueActiveKey.MAX_LENGTH);
        }

        private Subscription.SubscriptionBuilder aSubscriptionWithLongIds() {
            // Identifiers are nvarchar(64) columns.
            var longId = "x".repeat(64);
            return aSubscription(longId)
                .type(Subscription.Type.PUSH)
                .referenceType(SubscriptionReferenceType.API_PRODUCT)
                .referenceId(longId)
                .application(longId)
                .plan(longId);
        }

        private String keyOfPush(String id, String configuration) {
            return SubscriptionUniqueActiveKey.of(aSubscription(id).type(Subscription.Type.PUSH).configuration(configuration).build());
        }
    }

    private static Subscription.SubscriptionBuilder aSubscription(String id) {
        return Subscription.builder()
            .id(id)
            .api("api-1")
            .referenceType(SubscriptionReferenceType.API)
            .referenceId("api-1")
            .application("app-1")
            .plan("plan-1")
            .status(Subscription.Status.ACCEPTED)
            .type(Subscription.Type.STANDARD);
    }
}
