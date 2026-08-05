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

import static org.assertj.core.api.Assertions.assertThat;

import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DistributedWriteGateTest {

    @Test
    void should_cap_concurrency_at_the_bound_and_actually_reach_it() {
        DistributedWriteGate gate = new DistributedWriteGate(8);
        AtomicInteger current = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();

        List<Completable> writes = IntStream.range(0, 100)
            .mapToObj(i ->
                gate.runGated(
                    // Count via doOnSubscribe/doOnTerminate (which run before the terminal is propagated) so the
                    // gauge reflects true concurrency; doFinally would over-count as the freed permit is handed
                    // to the next write before the finishing write's decrement runs.
                    Completable.timer(20, TimeUnit.MILLISECONDS)
                        .doOnSubscribe(disposable -> peak.accumulateAndGet(current.incrementAndGet(), Math::max))
                        .doOnTerminate(current::decrementAndGet)
                )
            )
            .toList();

        Completable.merge(writes).test().awaitDone(10, TimeUnit.SECONDS).assertComplete();

        // Exactly 8: the gate must both cap concurrency at 8 AND actually reach it (100 writes submitted at
        // once), so an over-serializing regression such as a permit leak is caught here.
        assertThat(peak.get()).isEqualTo(8);
    }

    @Test
    void should_not_drop_writes_when_the_redis_waiting_queue_would_overflow() {
        // Reproduces APIM-14672: the Redis client rejects writes with "Redis waiting queue is full" once too
        // many are in-flight, silently dropping events during a bulk sync. The bound must keep in-flight under
        // that ceiling so every write still runs.
        int waitingQueueCeiling = 8;
        DistributedWriteGate gate = new DistributedWriteGate(waitingQueueCeiling);
        AtomicInteger current = new AtomicInteger();

        List<Completable> writes = IntStream.range(0, 200)
            .mapToObj(i ->
                gate.runGated(
                    Completable.defer(() -> {
                        if (current.incrementAndGet() > waitingQueueCeiling) {
                            current.decrementAndGet();
                            return Completable.error(new RuntimeException("Redis waiting queue is full"));
                        }
                        return Completable.timer(20, TimeUnit.MILLISECONDS).doOnTerminate(current::decrementAndGet);
                    })
                )
            )
            .toList();

        Completable.merge(writes).test().awaitDone(10, TimeUnit.SECONDS).assertComplete();
    }

    @Test
    void should_recover_the_permit_when_a_holder_is_disposed() {
        DistributedWriteGate gate = new DistributedWriteGate(1);

        var holder = gate.runGated(Completable.never()).test();
        var parked = gate.runGated(Completable.complete()).test();
        parked.assertNotComplete(); // the single permit is held, so this run parks

        holder.dispose(); // frees the permit -> handed to the parked run
        parked.assertComplete();

        // permit fully returned to the pool afterwards: a fresh run completes immediately
        gate.runGated(Completable.complete()).test().assertComplete();
    }

    @Test
    void should_not_release_a_permit_when_a_parked_waiter_is_disposed() {
        DistributedWriteGate gate = new DistributedWriteGate(1);

        var holder = gate.runGated(Completable.never()).test(); // holds the only permit
        var parked = gate.runGated(Completable.never()).test(); // parks, never granted

        parked.dispose(); // disposing the WAITER must not free the holder's permit

        var next = gate.runGated(Completable.complete()).test();
        next.assertNotComplete(); // gate still full, holder keeps its permit

        holder.dispose(); // now a permit frees and is handed to next
        next.assertComplete();
    }

    @Test
    void should_return_every_permit_under_concurrent_completions_errors_and_disposals() throws InterruptedException {
        DistributedWriteGate gate = new DistributedWriteGate(4);
        int threads = 8;
        int opsPerThread = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            final int seed = t;
            pool.submit(() -> {
                try {
                    for (int i = 0; i < opsPerThread; i++) {
                        int kind = (seed + i) % 3;
                        Completable work = switch (kind) {
                            case 0 -> Completable.complete();
                            case 1 -> Completable.error(new RuntimeException("boom"));
                            default -> Completable.timer(2, TimeUnit.MILLISECONDS);
                        };
                        var observer = gate.runGated(work).test();
                        if (kind == 2 && i % 2 == 0) {
                            observer.dispose(); // race a disposal against the in-flight write
                        } else {
                            observer.awaitDone(5, TimeUnit.SECONDS);
                        }
                    }
                } finally {
                    done.countDown();
                }
            });
        }
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        // Every permit must have been returned, whatever the interleaving of completes/errors/disposals.
        int inFlight = gate.inFlight();
        for (int i = 0; i < 200 && inFlight != 0; i++) {
            Thread.sleep(10);
            inFlight = gate.inFlight();
        }
        assertThat(inFlight).isZero();
        // ...and the gate still grants afterwards.
        gate.runGated(Completable.complete()).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();
    }
}
