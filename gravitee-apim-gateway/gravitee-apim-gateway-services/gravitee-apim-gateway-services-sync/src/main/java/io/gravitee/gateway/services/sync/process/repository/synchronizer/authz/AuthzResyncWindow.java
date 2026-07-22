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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks the {@code from} bound of a failed authz sync window so the synchronizer can swallow the
 * failure — keeping the shared sync cycle (and everything ordered after authz: API, subscription,
 * api-key) alive — and still re-fetch the missed window itself on the next cycle. The global cycle
 * cursor advances on swallowed failures, so without this the failed window's events would be lost
 * and the engine would drift from the control plane. A failed initial sync ({@code from = -1})
 * re-runs as an initial fetch next cycle.
 */
class AuthzResyncWindow {

    private static final long NONE = Long.MIN_VALUE;

    private final AtomicLong retryFrom = new AtomicLong(NONE);

    /** The {@code from} to fetch with this cycle: the caller's window widened to cover a previously
     *  failed window, {@code -1} (full initial fetch) when either is an initial sync. */
    Long effectiveFrom(Long from) {
        long pending = retryFrom.get();
        if (pending == NONE) {
            return from;
        }
        if (pending == -1L || from == null || from == -1L) {
            return -1L;
        }
        return Math.min(pending, from);
    }

    /** Record a failed window; returns {@code true} on the first failure after a success (log loudly once). */
    boolean markFailed(Long effectiveFrom) {
        long failedFrom = effectiveFrom == null ? -1L : effectiveFrom;
        long previous = retryFrom.getAndSet(failedFrom);
        return previous == NONE;
    }

    /** Clear any pending window; returns {@code true} when this success recovered from a failure. */
    boolean markSucceeded() {
        return retryFrom.getAndSet(NONE) != NONE;
    }
}
