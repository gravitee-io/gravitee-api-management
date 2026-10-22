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
package io.gravitee.apim.common.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LazyCloseableTest {

    private static final int TIMEOUT_SECONDS = 10;

    private final List<Object> closed = new CopyOnWriteArrayList<>();
    private final LazyCloseable<Object> cut = new LazyCloseable<>(closed::add);

    @Test
    void should_build_once_and_return_the_same_resource() {
        AtomicInteger builds = new AtomicInteger();

        Object first = cut.getOrBuild(() -> new Object[] { builds.incrementAndGet() });
        Object second = cut.getOrBuild(() -> new Object[] { builds.incrementAndGet() });

        assertThat(second).isSameAs(first);
        assertThat(builds).hasValue(1);
    }

    @Test
    void should_close_the_built_resource_once() {
        Object resource = cut.getOrBuild(Object::new);

        cut.close();
        cut.close();

        assertThat(closed).containsExactly(resource);
        assertThat(cut.isClosed()).isTrue();
    }

    @Test
    void should_fail_to_build_once_closed() {
        cut.getOrBuild(Object::new);
        cut.close();

        assertThatThrownBy(() -> cut.getOrBuild(Object::new)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_not_build_a_resource_after_a_close_that_came_first() {
        cut.close();

        assertThatThrownBy(() -> cut.getOrBuild(Object::new)).isInstanceOf(IllegalStateException.class);
        assertThat(closed).isEmpty();
    }

    @Test
    void should_close_the_resource_being_built_when_closed_from_another_thread() throws Exception {
        CountDownLatch building = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Object resource = new Object();

        Thread request = new Thread(() ->
            cut.getOrBuild(() -> {
                building.countDown();
                awaitQuietly(release);
                return resource;
            })
        );
        request.start();
        assertThat(building.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

        Thread stop = new Thread(cut::close);
        stop.start();
        // close() needs the lock held by the build: it can only wait for it, never run before it.
        waitUntilBlocked(stop);
        release.countDown();
        request.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        stop.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));

        assertThat(closed).containsExactly(resource);
    }

    @Test
    void should_build_a_single_resource_under_concurrent_first_use() throws Exception {
        int threads = 16;
        AtomicInteger builds = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        List<Object> results = new CopyOnWriteArrayList<>();
        List<Thread> workers = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threads; i++) {
            Thread worker = new Thread(() -> {
                awaitQuietly(start);
                results.add(
                    cut.getOrBuild(() -> {
                        builds.incrementAndGet();
                        return new Object();
                    })
                );
            });
            workers.add(worker);
            worker.start();
        }
        start.countDown();
        for (Thread worker : workers) {
            worker.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        }

        assertThat(builds).hasValue(1);
        assertThat(results)
            .hasSize(threads)
            .allSatisfy(result -> assertThat(result).isSameAs(results.get(0)));
    }

    private static void waitUntilBlocked(Thread thread) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (thread.getState() != Thread.State.BLOCKED) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Thread never blocked on the holder lock, state " + thread.getState());
            }
            Thread.sleep(5);
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
