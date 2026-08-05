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
package io.gravitee.repository.redis.distributedsync;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableEmitter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reactive permit gate bounding the number of concurrent distributed writes. {@link #runGated(Completable)}
 * runs its write once a permit is available and otherwise parks until another write releases one. Because
 * every distributed write funnels through it, the bound is global — it caps the aggregate in-flight writes,
 * not just a single caller's fan-out — and keeps the Redis client waiting queue (max-waiting-handlers) from
 * overflowing during bulk syncs. It holds no unbounded buffer: only the writes actively contending for a
 * permit ever park.
 * <p>
 * The permit is booked to a per-run {@code held} flag the instant it is granted (initial grant or hand-off),
 * under the lock and <em>before</em> any {@code onComplete} is signalled. Release is driven by the run's
 * {@code doFinally} keyed on that flag, so a concurrent dispose that swallows the grant signal (e.g.
 * {@code flatMapCompletable(..., delayErrors=false)} cancelling siblings when one write errors) cannot orphan
 * a counted permit — {@code doFinally} still runs on dispose and releases it.
 * <p>
 * The bound is on the total in-flight writes, independent of how many Redis connections back them: even if
 * every permitted write landed on a single connection, {@code maxConcurrency} stays below that connection's
 * waiting-handler limit, so no connection's queue can overflow.
 */
class DistributedWriteGate {

    private final int maxConcurrency;
    private final Deque<Waiter> waiters = new ArrayDeque<>();
    private int inFlight;

    DistributedWriteGate(final int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    Completable runGated(final Completable write) {
        // defer so `held` is per-subscription: correct even if a retry()/repeat() is ever composed above.
        return Completable.defer(() -> {
            AtomicBoolean held = new AtomicBoolean();
            return acquire(held)
                .andThen(write)
                .doFinally(() -> {
                    if (held.compareAndSet(true, false)) {
                        release();
                    }
                });
        });
    }

    // Visible for tests: current number of granted (in-flight) permits.
    int inFlight() {
        synchronized (this) {
            return inFlight;
        }
    }

    private Completable acquire(final AtomicBoolean held) {
        return Completable.create(emitter -> {
            boolean granted = false;
            synchronized (this) {
                if (inFlight < maxConcurrency) {
                    inFlight++;
                    held.set(true);
                    granted = true;
                } else {
                    Waiter waiter = new Waiter(emitter, held);
                    waiters.add(waiter);
                    emitter.setCancellable(() -> {
                        synchronized (this) {
                            waiters.remove(waiter);
                        }
                    });
                }
            }
            // Signalled outside the lock; the permit is already booked to `held`, so a dispose racing this
            // point still releases via the run's doFinally.
            if (granted) {
                emitter.onComplete();
            }
        });
    }

    private void release() {
        // Hand the freed permit to the next live waiter — booking it under the lock before signalling so a
        // racing dispose cannot orphan it — otherwise return the permit to the pool. Disposed waiters are
        // skipped without decrementing (their run never held the permit).
        while (true) {
            Waiter next;
            synchronized (this) {
                next = waiters.poll();
                if (next == null) {
                    inFlight--;
                    return;
                }
                if (next.emitter().isDisposed()) {
                    continue;
                }
                next.held().set(true);
            }
            next.emitter().onComplete();
            return;
        }
    }

    private record Waiter(CompletableEmitter emitter, AtomicBoolean held) {}
}
