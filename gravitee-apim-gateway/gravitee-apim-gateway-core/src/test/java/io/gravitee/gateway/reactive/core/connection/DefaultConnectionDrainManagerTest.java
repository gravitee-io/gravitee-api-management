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
package io.gravitee.gateway.reactive.core.connection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class DefaultConnectionDrainManagerTest {

    DefaultConnectionDrainManager cut;

    @BeforeEach
    void setUp() {
        cut = new DefaultConnectionDrainManager();
    }

    @Test
    void should_not_return_date_when_never_requested_connection_drain() {
        assertThat(cut.drainRequestedAt()).isEqualTo(-1);
    }

    @Test
    void should_change_date_when_request_connection_drain() {
        long now = System.currentTimeMillis();
        cut.requestDrain();
        assertThat(cut.drainRequestedAt()).isBetween(now, System.currentTimeMillis());
    }

    @Test
    void should_notify_listeners_when_request_connection_drain() {
        AtomicBoolean listenerCalled = new AtomicBoolean(false);
        cut.registerListener(aLong -> listenerCalled.set(true));

        cut.requestDrain();
        assertThat(listenerCalled.get()).isTrue();
    }

    @Test
    void should_not_notify_unregistered_listener_when_request_connection_drain() {
        AtomicInteger listener1CallCount = new AtomicInteger(0);
        AtomicInteger listener2CallCount = new AtomicInteger(0);

        String listener1Id = cut.registerListener(aLong -> listener1CallCount.incrementAndGet());
        String listener2Id = cut.registerListener(aLong -> listener2CallCount.incrementAndGet());

        cut.requestDrain();
        cut.unregisterListener(listener1Id);
        cut.requestDrain();
        cut.unregisterListener(listener2Id);
        cut.requestDrain();

        assertThat(listener1CallCount.get()).isEqualTo(1);
        assertThat(listener2CallCount.get()).isEqualTo(2);
    }

    @Test
    @SneakyThrows
    void should_unregistered_listener_when_stopping_manager() {
        AtomicInteger listener1CallCount = new AtomicInteger(0);
        AtomicInteger listener2CallCount = new AtomicInteger(0);

        cut.registerListener(aLong -> listener1CallCount.incrementAndGet());
        cut.registerListener(aLong -> listener2CallCount.incrementAndGet());

        cut.doStop();
        cut.requestDrain();

        assertThat(listener1CallCount.get()).isEqualTo(0);
        assertThat(listener2CallCount.get()).isEqualTo(0);
    }
}
