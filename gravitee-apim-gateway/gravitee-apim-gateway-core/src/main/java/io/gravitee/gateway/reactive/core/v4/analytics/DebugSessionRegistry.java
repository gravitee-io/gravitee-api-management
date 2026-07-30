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
package io.gravitee.gateway.reactive.core.v4.analytics;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import lombok.CustomLog;

/**
 * Debug sessions currently open on this node, keyed by API id.
 *
 * A session raises the analytics detail of a deployed API — verbose tracing and
 * payload capture — for a bounded window, without redeploying it. Both are
 * expensive enough that they cannot simply be left on, so a session carries its
 * own expiry and samples the traffic it captures.
 *
 * What a session cannot turn on is tracing itself: the tracer and the per-policy
 * hooks are wired when the reactor is built, so an API deployed without tracing
 * has nothing to raise the detail of. That split is deliberate — the cheap part
 * stays static, the expensive part becomes dynamic and bounded.
 */
@CustomLog
public class DebugSessionRegistry {

    private final Map<String, DebugSession> sessions = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public DebugSessionRegistry() {
        this(System::currentTimeMillis);
    }

    public DebugSessionRegistry(final LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * Opens (or replaces) the session of an API.
     *
     * @param samplingPercent share of requests to capture, clamped to 1..100.
     */
    public void open(final String apiId, final long expiresAt, final int samplingPercent) {
        sessions.put(apiId, new DebugSession(apiId, expiresAt, samplingPercent));
        log.debug("Debug session opened for API {} until {} at {}% sampling", apiId, expiresAt, samplingPercent);
    }

    public void close(final String apiId) {
        if (sessions.remove(apiId) != null) {
            log.debug("Debug session closed for API {}", apiId);
        }
    }

    /**
     * The session capturing this API right now, if any. Expired sessions are
     * dropped as they are read, so a node that stops receiving updates still
     * stops capturing on its own.
     */
    public Optional<DebugSession> activeFor(final String apiId) {
        final DebugSession session = sessions.get(apiId);
        if (session == null) {
            return Optional.empty();
        }
        if (session.hasExpired(clock.getAsLong())) {
            sessions.remove(apiId, session);
            log.debug("Debug session expired for API {}", apiId);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public boolean isEmpty() {
        return sessions.isEmpty();
    }

    /** One open session, with the sampling state of the traffic it has seen. */
    public static class DebugSession {

        private final String apiId;
        private final long expiresAt;
        private final int step;
        private final AtomicLong seen = new AtomicLong();

        DebugSession(final String apiId, final long expiresAt, final int samplingPercent) {
            this.apiId = apiId;
            this.expiresAt = expiresAt;
            // A percentage maps to "one request in N": 100% captures everything,
            // 10% captures one in ten. An out-of-range value falls back to
            // capturing everything — a session that silently records nothing
            // reads as a broken feature, and the expiry already bounds the cost.
            final boolean usable = samplingPercent >= 1 && samplingPercent <= 100;
            this.step = usable ? 100 / samplingPercent : 1;
        }

        public String apiId() {
            return apiId;
        }

        public long expiresAt() {
            return expiresAt;
        }

        boolean hasExpired(final long now) {
            return now >= expiresAt;
        }

        /**
         * Whether the request being prepared belongs to this session's sample.
         * Called once per request, so it counts as it decides.
         */
        public boolean shouldCapture() {
            return seen.getAndIncrement() % step == 0;
        }
    }
}
