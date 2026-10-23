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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Key that the {@link SubscriptionRepository} implementations store with every active subscription, so that a unique
 * index can refuse a second active subscription of an application to the same plan, even when both are created at the
 * same time by different Management API nodes.
 * <p>
 * Two subscriptions share a key exactly when the subscription creation refuses the second one with
 * {@code plan.subscribed}:
 * <ul>
 *     <li>both are active, that is neither {@code REJECTED} nor {@code CLOSED} ({@code PENDING}, {@code ACCEPTED} and
 *     {@code PAUSED} subscriptions block a new one);</li>
 *     <li>same reference (API or API Product), application and plan: {@code referenceType:referenceId:application:plan};</li>
 *     <li>for {@code PUSH} subscriptions, where an application may subscribe a plan once per channel, the same channel
 *     token, read from the configuration the way the subscription creation reads it: no configuration, a
 *     configuration without channel, or the channel itself. A configuration that cannot be read never matches another
 *     subscription, so its token is the subscription id.</li>
 * </ul>
 * The channel is hashed to keep the key within {@link #MAX_LENGTH} characters, the size of the JDBC column.
 * <p>
 * The key is a storage detail: it is not part of the {@link Subscription} model, which the repository bridge
 * serialises for the gateways.
 *
 * @author GraviteeSource Team
 */
public final class SubscriptionUniqueActiveKey {

    /**
     * Longest key: an {@code API_PRODUCT} reference and identifiers of 64 characters, plus a channel token.
     */
    public static final int MAX_LENGTH = 320;

    private static final String SEPARATOR = ":";
    private static final String NO_CONFIGURATION = "no-configuration";
    private static final String NO_CHANNEL = "no-channel";
    private static final String CHANNEL = "channel";
    private static final String UNREADABLE_CONFIGURATION = "unreadable";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SubscriptionUniqueActiveKey() {}

    /**
     * @return the key of the subscription, or {@code null} when it does not block a new subscription: not active, or
     * missing its reference, application or plan.
     */
    public static String of(Subscription subscription) {
        if (subscription == null || !isActive(subscription.getStatus())) {
            return null;
        }
        String referenceId = subscription.getReferenceId() != null ? subscription.getReferenceId() : subscription.getApi();
        if (referenceId == null || subscription.getApplication() == null || subscription.getPlan() == null) {
            return null;
        }
        // Legacy subscriptions without reference type are API subscriptions.
        SubscriptionReferenceType referenceType = subscription.getReferenceType() != null
            ? subscription.getReferenceType()
            : SubscriptionReferenceType.API;

        String key = String.join(SEPARATOR, referenceType.name(), referenceId, subscription.getApplication(), subscription.getPlan());
        if (subscription.getType() == Subscription.Type.PUSH) {
            return key + SEPARATOR + channelToken(subscription);
        }
        return key;
    }

    private static boolean isActive(Subscription.Status status) {
        return status != Subscription.Status.REJECTED && status != Subscription.Status.CLOSED;
    }

    private static String channelToken(Subscription subscription) {
        if (subscription.getConfiguration() == null) {
            return NO_CONFIGURATION;
        }
        PushConfiguration configuration = read(subscription.getConfiguration());
        if (configuration == null) {
            return UNREADABLE_CONFIGURATION + SEPARATOR + subscription.getId();
        }
        if (configuration.channel() == null) {
            return NO_CHANNEL;
        }
        return CHANNEL + SEPARATOR + sha256(configuration.channel());
    }

    private static PushConfiguration read(String configuration) {
        try {
            return MAPPER.readValue(configuration, PushConfiguration.class);
        } catch (Exception e) {
            // The subscription creation ignores a configuration it cannot read: it never makes a duplicate.
            return null;
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /**
     * Same properties as the {@code SubscriptionConfigurationEntity} the subscription creation reads the configuration
     * into, so that the same configurations are readable.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PushConfiguration(String entrypointId, String channel, JsonNode entrypointConfiguration) {}
}
