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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Ticker;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class NewtAIRateLimiterTest {

    @Test
    void should_allow_requests_until_limit_then_reject() {
        var limiter = new NewtAIRateLimiter(3);

        assertThat(limiter.tryAcquire("user-1").allowed()).isTrue();
        assertThat(limiter.tryAcquire("user-1").allowed()).isTrue();
        assertThat(limiter.tryAcquire("user-1").allowed()).isTrue();
        var rejected = limiter.tryAcquire("user-1");
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfterSeconds()).isBetween(1L, TimeUnit.NANOSECONDS.toSeconds(NewtAIRateLimiter.WINDOW_NANOS));
    }

    @Test
    void should_isolate_limits_per_user() {
        var limiter = new NewtAIRateLimiter(1);

        assertThat(limiter.tryAcquire("user-a").allowed()).isTrue();
        assertThat(limiter.tryAcquire("user-a").allowed()).isFalse();
        assertThat(limiter.tryAcquire("user-b").allowed()).isTrue();
    }

    @Test
    void should_clear_windows() {
        var limiter = new NewtAIRateLimiter(1);
        assertThat(limiter.tryAcquire("user-1").allowed()).isTrue();
        assertThat(limiter.tryAcquire("user-1").allowed()).isFalse();

        limiter.clear();

        assertThat(limiter.tryAcquire("user-1").allowed()).isTrue();
    }

    @Test
    void should_reset_window_after_sixty_seconds() {
        var nanos = new AtomicLong();
        Ticker ticker = new Ticker() {
            @Override
            public long read() {
                return nanos.get();
            }
        };
        var limiter = new NewtAIRateLimiter(1, ticker);

        assertThat(limiter.tryAcquire("user-1").allowed()).isTrue();
        assertThat(limiter.tryAcquire("user-1").allowed()).isFalse();

        nanos.addAndGet(NewtAIRateLimiter.WINDOW_NANOS);

        assertThat(limiter.tryAcquire("user-1").allowed()).isTrue();
    }

    @Test
    void should_drop_an_idle_user_so_the_next_call_starts_a_new_window() {
        var nanos = new AtomicLong();
        Ticker ticker = new Ticker() {
            @Override
            public long read() {
                return nanos.get();
            }
        };
        var limiter = new NewtAIRateLimiter(1, ticker);

        assertThat(limiter.tryAcquire("user-1").allowed()).isTrue();
        assertThat(limiter.tryAcquire("user-1").allowed()).isFalse();

        nanos.addAndGet(TimeUnit.MINUTES.toNanos(2) + 1);

        assertThat(limiter.tryAcquire("user-1").allowed()).isTrue();
    }
}
