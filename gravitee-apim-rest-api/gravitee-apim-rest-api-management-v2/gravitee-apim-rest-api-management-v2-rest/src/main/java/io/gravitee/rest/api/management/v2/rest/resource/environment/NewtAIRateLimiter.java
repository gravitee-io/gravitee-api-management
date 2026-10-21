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

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;

/**
 * Per-user fixed-window limiter for Newt AI generate calls.
 * Each Management API process keeps its own counters; a cluster does not share them.
 * Idle users are dropped two minutes after their last call.
 */
public final class NewtAIRateLimiter {

    public static final int DEFAULT_MAX_REQUESTS_PER_MINUTE = 30;
    static final long WINDOW_NANOS = TimeUnit.SECONDS.toNanos(60);
    private static final long IDLE_EVICTION_MINUTES = 2L;

    private volatile int maxRequestsPerMinute;
    private final Ticker ticker;
    private final Cache<String, Window> windows;

    public NewtAIRateLimiter() {
        this(DEFAULT_MAX_REQUESTS_PER_MINUTE);
    }

    public NewtAIRateLimiter(int maxRequestsPerMinute) {
        this(maxRequestsPerMinute, Ticker.systemTicker());
    }

    NewtAIRateLimiter(int maxRequestsPerMinute, Ticker ticker) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.ticker = ticker;
        this.windows = CacheBuilder.newBuilder().expireAfterAccess(IDLE_EVICTION_MINUTES, TimeUnit.MINUTES).ticker(ticker).build();
    }

    public Decision tryAcquire(String userId) {
        long now = ticker.read();
        Window window = windows.asMap().computeIfAbsent(userId, ignored -> new Window(now));
        synchronized (window) {
            if (now - window.windowStart >= WINDOW_NANOS) {
                window.windowStart = now;
                window.count = 0;
            }
            if (window.count >= maxRequestsPerMinute) {
                long elapsed = now - window.windowStart;
                long retryAfterSeconds = Math.max(
                    1L,
                    (WINDOW_NANOS - elapsed + TimeUnit.SECONDS.toNanos(1) - 1) / TimeUnit.SECONDS.toNanos(1)
                );
                return Decision.rejected(retryAfterSeconds);
            }
            window.count++;
            return Decision.accepted();
        }
    }

    /** Clears all windows. Intended for tests. */
    void clear() {
        windows.invalidateAll();
    }

    /** Overrides the limit and clears windows. Intended for tests. */
    synchronized void overrideLimit(int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        windows.invalidateAll();
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {
        static Decision accepted() {
            return new Decision(true, 0);
        }

        static Decision rejected(long retryAfterSeconds) {
            return new Decision(false, retryAfterSeconds);
        }
    }

    private static final class Window {

        private long windowStart;
        private int count;

        private Window(long start) {
            this.windowStart = start;
        }
    }
}
