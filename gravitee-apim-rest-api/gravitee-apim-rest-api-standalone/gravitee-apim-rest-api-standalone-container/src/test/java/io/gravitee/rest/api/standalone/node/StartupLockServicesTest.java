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
package io.gravitee.rest.api.standalone.node;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.exceptions.StartupLockTimeoutException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.StartupLock;
import io.gravitee.repository.management.api.StartupLockRepository;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class StartupLockServicesTest {

    private static final Duration LONG_WAIT = Duration.ofMinutes(1);

    private final InMemoryStartupLockRepository repository = new InMemoryStartupLockRepository();

    @Test
    void acquiring_waits_for_the_current_holder_to_release_the_lock() throws Exception {
        StartupLock otherNode = repository.acquire(Duration.ZERO);
        AcquireStartupLockService acquire = new AcquireStartupLockService(new NodeStartupLock(repository, LONG_WAIT));

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<AcquireStartupLockService> starting = executor.submit(acquire::start);
            assertThat(repository.contended.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(starting).isNotDone();

            otherNode.close();

            starting.get(10, TimeUnit.SECONDS);
        }
        assertThat(repository.isHeld()).isTrue();
    }

    @Test
    void acquiring_fails_when_the_lock_stays_held_past_the_timeout() throws Exception {
        repository.acquire(Duration.ZERO);
        AcquireStartupLockService acquire = new AcquireStartupLockService(new NodeStartupLock(repository, Duration.ZERO));

        assertThatThrownBy(acquire::start).isInstanceOf(StartupLockTimeoutException.class);
    }

    @Test
    void stopping_the_acquire_service_releases_the_lock() throws Exception {
        AcquireStartupLockService acquire = new AcquireStartupLockService(new NodeStartupLock(repository, LONG_WAIT));
        acquire.start();
        assertThat(repository.isHeld()).isTrue();

        acquire.stop();

        assertThat(repository.isHeld()).isFalse();
    }

    @Test
    void starting_the_release_service_releases_the_lock() throws Exception {
        NodeStartupLock lock = new NodeStartupLock(repository, LONG_WAIT);
        new AcquireStartupLockService(lock).start();
        assertThat(repository.isHeld()).isTrue();

        new ReleaseStartupLockService(lock).start();

        assertThat(repository.isHeld()).isFalse();
    }

    @Test
    void releasing_again_does_not_release_a_lock_taken_since_by_another_node() throws Exception {
        NodeStartupLock lock = new NodeStartupLock(repository, LONG_WAIT);
        AcquireStartupLockService acquire = new AcquireStartupLockService(lock);
        acquire.start();
        new ReleaseStartupLockService(lock).start();
        repository.acquire(Duration.ZERO);

        new ReleaseStartupLockService(lock).start();
        acquire.stop();

        assertThat(repository.isHeld()).isTrue();
    }

    @Test
    void two_nodes_never_run_their_startup_phase_at_the_same_time() throws Exception {
        AtomicInteger nodesInStartupPhase = new AtomicInteger();
        AtomicInteger mostNodesInStartupPhase = new AtomicInteger();
        Callable<Void> restartingNode = () -> {
            for (int restart = 0; restart < 200; restart++) {
                NodeStartupLock lock = new NodeStartupLock(repository, LONG_WAIT);
                new AcquireStartupLockService(lock).start();
                mostNodesInStartupPhase.accumulateAndGet(nodesInStartupPhase.incrementAndGet(), Math::max);
                Thread.yield();
                nodesInStartupPhase.decrementAndGet();
                new ReleaseStartupLockService(lock).start();
            }
            return null;
        };

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            for (Future<Void> node : executor.invokeAll(List.of(restartingNode, restartingNode), 30, TimeUnit.SECONDS)) {
                node.get();
            }
        }

        assertThat(mostNodesInStartupPhase).hasValue(1);
        assertThat(repository.isHeld()).isFalse();
    }

    private static final class InMemoryStartupLockRepository implements StartupLockRepository {

        private final Semaphore permit = new Semaphore(1);
        private final CountDownLatch contended = new CountDownLatch(1);

        @Override
        public StartupLock acquire(Duration timeout) throws TechnicalException {
            if (!permit.tryAcquire()) {
                contended.countDown();
                if (!tryAcquire(timeout)) {
                    throw new StartupLockTimeoutException("Startup lock not acquired within " + timeout);
                }
            }
            return permit::release;
        }

        private boolean tryAcquire(Duration timeout) throws TechnicalException {
            try {
                return permit.tryAcquire(timeout.toNanos(), TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TechnicalException("Interrupted while waiting for the startup lock", e);
            }
        }

        boolean isHeld() {
            return permit.availablePermits() == 0;
        }
    }
}
