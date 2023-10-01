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

import java.time.ZonedDateTime;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionEntityTest {

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
}
