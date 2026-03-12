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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class BasicAuthCredentialsEntityTest {

    @Nested
    class GenerateForSubscription {

        @Test
        void should_generate_credentials_for_subscription() {
            SubscriptionEntity subscription = SubscriptionEntity.builder()
                .id("sub-1")
                .applicationId("app-1")
                .environmentId("env-1")
                .build();

            BasicAuthCredentialsEntity credentials = BasicAuthCredentialsEntity.generateForSubscription(subscription);

            assertThat(credentials.getId()).isNotNull();
            assertThat(credentials.getUsername()).isNotNull().startsWith("ba-");
            assertThat(credentials.getPassword()).isNotNull();
            assertThat(credentials.getApplicationId()).isEqualTo("app-1");
            assertThat(credentials.getSubscriptions()).containsExactly("sub-1");
            assertThat(credentials.getEnvironmentId()).isEqualTo("env-1");
        }
    }

    @Nested
    class AddSubscription {

        @Test
        void should_add_subscription() {
            BasicAuthCredentialsEntity credentials = BasicAuthCredentialsEntity.builder()
                .id("cred-1")
                .username("user-1")
                .password("pass-1")
                .applicationId("app-1")
                .subscriptions(List.of("sub-1"))
                .environmentId("env-1")
                .build();

            BasicAuthCredentialsEntity updated = credentials.addSubscription("sub-2");

            assertThat(updated).isNotSameAs(credentials);
            assertThat(updated.getSubscriptions()).containsExactly("sub-1", "sub-2");
        }
    }

    @Nested
    class Revoke {

        @Test
        void should_revoke() {
            BasicAuthCredentialsEntity credentials = BasicAuthCredentialsEntity.builder()
                .id("cred-1")
                .username("user-1")
                .password("pass-1")
                .applicationId("app-1")
                .subscriptions(List.of("sub-1"))
                .environmentId("env-1")
                .revoked(false)
                .revokedAt(null)
                .build();

            BasicAuthCredentialsEntity revoked = credentials.revoke();

            assertThat(revoked).isNotSameAs(credentials);
            assertThat(revoked.isRevoked()).isTrue();
            assertThat(revoked.getRevokedAt()).isNotNull();
        }
    }

    @Nested
    class IsExpired {

        @Test
        void should_detect_expired() {
            ZonedDateTime pastExpireAt = ZonedDateTime.now().minusDays(1);
            BasicAuthCredentialsEntity credentials = BasicAuthCredentialsEntity.builder()
                .id("cred-1")
                .username("user-1")
                .password("pass-1")
                .applicationId("app-1")
                .subscriptions(List.of("sub-1"))
                .environmentId("env-1")
                .expireAt(pastExpireAt)
                .build();

            assertThat(credentials.isExpired()).isTrue();
        }

        @Test
        void should_detect_not_expired() {
            ZonedDateTime futureExpireAt = ZonedDateTime.now().plusDays(1);
            BasicAuthCredentialsEntity credentials = BasicAuthCredentialsEntity.builder()
                .id("cred-1")
                .username("user-1")
                .password("pass-1")
                .applicationId("app-1")
                .subscriptions(List.of("sub-1"))
                .environmentId("env-1")
                .expireAt(futureExpireAt)
                .build();

            assertThat(credentials.isExpired()).isFalse();
        }
    }
}
