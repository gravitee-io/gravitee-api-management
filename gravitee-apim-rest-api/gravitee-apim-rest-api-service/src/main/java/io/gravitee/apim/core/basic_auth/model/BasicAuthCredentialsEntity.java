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
package io.gravitee.apim.core.basic_auth.model;

import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.SubscriptionClosedException;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class BasicAuthCredentialsEntity {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int PASSWORD_LENGTH = 32;

    private String id;
    private String username;
    private String password;
    private String applicationId;

    @Builder.Default
    private List<String> subscriptions = new ArrayList<>();

    private String environmentId;
    private ZonedDateTime expireAt;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private boolean revoked;
    private ZonedDateTime revokedAt;

    public static BasicAuthCredentialsEntity generateForSubscription(SubscriptionEntity subscription) {
        var now = TimeProvider.now();
        if (subscription.getEndingAt() != null && subscription.getEndingAt().isBefore(now)) {
            throw new SubscriptionClosedException(subscription.getId());
        }

        return BasicAuthCredentialsEntity.builder()
            .id(UuidString.generateRandom())
            .applicationId(subscription.getApplicationId())
            .createdAt(now)
            .updatedAt(now)
            .username(generateUsername())
            .password(generatePassword())
            .subscriptions(List.of(subscription.getId()))
            .environmentId(subscription.getEnvironmentId())
            .expireAt(subscription.getEndingAt())
            .build();
    }

    public BasicAuthCredentialsEntity addSubscription(String subscriptionId) {
        var list = new ArrayList<>(subscriptions);
        list.add(subscriptionId);
        return this.toBuilder().subscriptions(list).updatedAt(TimeProvider.now()).build();
    }

    public BasicAuthCredentialsEntity revoke() {
        var now = ZonedDateTime.now();
        return this.toBuilder().revoked(true).updatedAt(now).revokedAt(now).build();
    }

    public boolean isExpired() {
        return this.expireAt != null && ZonedDateTime.now().isAfter(this.getExpireAt());
    }

    public boolean canBeRevoked() {
        return !this.isRevoked() && !this.isExpired();
    }

    public boolean hasSubscription(String subscriptionId) {
        return subscriptions.contains(subscriptionId);
    }

    private static String generateUsername() {
        return "ba-" + UuidString.generateRandom().substring(0, 12);
    }

    private static String generatePassword() {
        byte[] bytes = new byte[PASSWORD_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
