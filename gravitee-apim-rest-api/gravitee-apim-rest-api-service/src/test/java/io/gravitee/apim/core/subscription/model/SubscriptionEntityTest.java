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
package io.gravitee.apim.core.subscription.model;

import static fixtures.core.model.SubscriptionFixtures.aSubscription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import io.gravitee.common.utils.TimeProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionEntityTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final ZonedDateTime STARTING_AT = Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault());
    private static final ZonedDateTime ENDING_AT = Instant.parse("2024-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault());

    @BeforeAll
    static void beforeAll() {
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Nested
    class Close {

        @ParameterizedTest
        @EnumSource(value = SubscriptionEntity.Status.class, names = { "ACCEPTED", "PAUSED" })
        void should_close_subscription(SubscriptionEntity.Status status) {
            ZonedDateTime now = ZonedDateTime.now();
            var subscription = aSubscription().toBuilder().status(status).build();
            var closedSubscription = subscription.close();

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(closedSubscription.getStatus()).isEqualTo(SubscriptionEntity.Status.CLOSED);
                soft.assertThat(closedSubscription.getClosedAt()).isEqualTo(closedSubscription.getUpdatedAt()).isAfterOrEqualTo(now);
            });
        }

        @Test
        void should_throw_exception_when_closing_a_pending_subscription() {
            var pending = aSubscription().toBuilder().status(SubscriptionEntity.Status.PENDING).build();

            Throwable throwable = catchThrowable(pending::close);

            assertThat(throwable).isInstanceOf(IllegalStateException.class);
        }

        @ParameterizedTest
        @EnumSource(value = SubscriptionEntity.Status.class, names = { "REJECTED", "CLOSED" })
        void should_do_nothing_when_closing_a_rejected_or_closed_subscription(SubscriptionEntity.Status status) {
            var subscription = aSubscription().toBuilder().status(status).build();

            var closedSubscription = subscription.close();

            assertThat(closedSubscription).isSameAs(subscription).isEqualTo(subscription);
        }
    }

    @Nested
    class Accept {

        @Test
        void should_accept_subscription_with_no_startingAt() {
            var subscription = aSubscription().toBuilder().startingAt(null).status(SubscriptionEntity.Status.PENDING).build();

            var acceptedSubscription = subscription.acceptBy("user-id", null, ENDING_AT, "my reason");

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(acceptedSubscription.getStatus()).isEqualTo(SubscriptionEntity.Status.ACCEPTED);
                soft
                    .assertThat(acceptedSubscription.getProcessedAt())
                    .isEqualTo(acceptedSubscription.getUpdatedAt())
                    .isEqualTo(acceptedSubscription.getStartingAt())
                    .isEqualTo(INSTANT_NOW.atZone(ZoneId.systemDefault()));
                soft.assertThat(acceptedSubscription.getEndingAt()).isEqualTo(ENDING_AT);
                soft.assertThat(acceptedSubscription.getReasonMessage()).isEqualTo("my reason");
                soft.assertThat(acceptedSubscription.getProcessedBy()).isEqualTo("user-id");
            });
        }

        @Test
        void should_accept_subscription_with_startingAt() {
            var subscription = aSubscription().toBuilder().status(SubscriptionEntity.Status.PENDING).build();

            var acceptedSubscription = subscription.acceptBy("user-id", STARTING_AT, ENDING_AT, "my reason");

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(acceptedSubscription.getStartingAt()).isEqualTo(STARTING_AT);
                soft.assertThat(acceptedSubscription.getEndingAt()).isEqualTo(ENDING_AT);
                soft
                    .assertThat(acceptedSubscription.getProcessedAt())
                    .isEqualTo(acceptedSubscription.getUpdatedAt())
                    .isEqualTo(INSTANT_NOW.atZone(ZoneId.systemDefault()));
            });
        }

        @ParameterizedTest
        @EnumSource(value = SubscriptionEntity.Status.class, mode = EnumSource.Mode.EXCLUDE, names = { "PENDING", "ACCEPTED" })
        void should_throw_exception_when_accepting_a_non_pending_subscription(SubscriptionEntity.Status status) {
            var pending = aSubscription().toBuilder().status(status).build();

            Throwable throwable = catchThrowable(() -> pending.acceptBy("user-id", STARTING_AT, ENDING_AT, "my reason"));

            assertThat(throwable).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void should_do_nothing_when_accepting_an_accepted_subscription() {
            var subscription = aSubscription().toBuilder().status(SubscriptionEntity.Status.ACCEPTED).build();

            var closedSubscription = subscription.acceptBy("user-id", STARTING_AT, ENDING_AT, "my reason");

            assertThat(closedSubscription).isSameAs(subscription).isEqualTo(subscription);
        }
    }
}
