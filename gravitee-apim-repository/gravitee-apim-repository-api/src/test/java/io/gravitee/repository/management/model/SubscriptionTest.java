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
package io.gravitee.repository.management.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
class SubscriptionTest {

    private Subscription cut;

    @BeforeEach
    void setUp() {
        cut = new Subscription();
    }

    @ParameterizedTest
    @EnumSource(value = Subscription.ConsumerStatus.class, names = { "STARTED", "FAILURE" })
    void shouldBeStoppableByConsumer(Subscription.ConsumerStatus consumerStatus) {
        final Subscription cut = this.cut;
        cut.setConsumerStatus(consumerStatus);
        assertThat(cut.canBeStoppedByConsumer()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = Subscription.ConsumerStatus.class, names = { "STARTED", "FAILURE" }, mode = EnumSource.Mode.EXCLUDE)
    void shouldNotBeStoppableByConsumer(Subscription.ConsumerStatus consumerStatus) {
        final Subscription cut = this.cut;
        cut.setConsumerStatus(consumerStatus);
        assertThat(cut.canBeStoppedByConsumer()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = Subscription.ConsumerStatus.class, names = { "STOPPED", "FAILURE" })
    void shouldBeStartableByConsumer(Subscription.ConsumerStatus consumerStatus) {
        final Subscription cut = this.cut;
        cut.setConsumerStatus(consumerStatus);
        assertThat(cut.canBeStartedByConsumer()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = Subscription.ConsumerStatus.class, names = { "STOPPED", "FAILURE" }, mode = EnumSource.Mode.EXCLUDE)
    void shouldNotBeStartableByConsumer(Subscription.ConsumerStatus consumerStatus) {
        final Subscription cut = this.cut;
        cut.setConsumerStatus(consumerStatus);
        assertThat(cut.canBeStartedByConsumer()).isFalse();
    }
}
