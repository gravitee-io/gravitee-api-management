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
package io.gravitee.gateway.services.sync;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactive.reactor.v4.subscription.SubscriptionDispatcher;
import io.gravitee.gateway.services.sync.process.deployer.NoOpSubscriptionDispatcher;
import io.gravitee.gateway.services.sync.process.repository.service.PlanService;
import io.reactivex.rxjava3.core.Completable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SyncConfigurationTest {

    private SyncConfiguration cut = new SyncConfiguration();

    @Test
    void should_provide_subscription_dispatcher() {
        final Supplier<SubscriptionDispatcher> supplier = cut.provideSubscriptionDispatcher(new DummySubscriptionDispatcher(false));
        Assertions.assertThat(supplier.get()).isInstanceOf(DummySubscriptionDispatcher.class);
    }

    @Test
    void should_provide_noop_subscription_dispatcher() {
        final Supplier<SubscriptionDispatcher> supplier = cut.provideSubscriptionDispatcher(new DummySubscriptionDispatcher(true));
        Assertions.assertThat(supplier.get()).isInstanceOf(NoOpSubscriptionDispatcher.class);
    }

    @Test
    void should_provide_plan_service() {
        Assertions.assertThat(cut.planService()).isInstanceOf(PlanService.class);
    }

    @ParameterizedTest
    @MethodSource("syncExecutors")
    void should_run_tasks_concurrently_on_a_sync_executor(String name, Function<Integer, ThreadPoolExecutor> factory)
        throws InterruptedException {
        final int poolSize = 3;
        final ThreadPoolExecutor executor = factory.apply(poolSize);
        try {
            // Behavioural, not structural: each task blocks until every other task has started, so the
            // latch can only reach zero if the pool really runs them in parallel. A pool that stays
            // single-threaded — as both did while corePoolSize was 1 behind an unbounded queue, which
            // never rejects and so never triggers growth — deadlocks here and the await times out.
            final CountDownLatch started = new CountDownLatch(poolSize);
            for (int i = 0; i < poolSize; i++) {
                executor.execute(() -> {
                    started.countDown();
                    try {
                        started.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            Assertions.assertThat(started.await(5, TimeUnit.SECONDS)).as("%s ran %d tasks concurrently", name, poolSize).isTrue();
            // Idle threads must still expire, so an idle gateway does not hold the extra threads.
            Assertions.assertThat(executor.allowsCoreThreadTimeOut()).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    private static Stream<Arguments> syncExecutors() {
        final SyncConfiguration configuration = new SyncConfiguration();
        return Stream.of(
            Arguments.of("syncFetcherExecutor", (Function<Integer, ThreadPoolExecutor>) configuration::syncFetcherExecutor),
            Arguments.of("syncDeployerExecutor", (Function<Integer, ThreadPoolExecutor>) configuration::syncDeployerExecutor)
        );
    }

    private static class DummySubscriptionDispatcher implements SubscriptionDispatcher {

        private final boolean shouldThrow;

        public DummySubscriptionDispatcher(boolean shouldThrow) {
            this.shouldThrow = shouldThrow;
        }

        private NoSuchBeanDefinitionException throwNoSuchBeanDefinitionException() {
            return new NoSuchBeanDefinitionException("subscriptionDispatcher");
        }

        @Override
        public Completable dispatch(Subscription subscription) {
            if (shouldThrow) {
                throw throwNoSuchBeanDefinitionException();
            }
            return Completable.complete();
        }

        @Override
        public Lifecycle.State lifecycleState() {
            if (shouldThrow) {
                throw throwNoSuchBeanDefinitionException();
            }
            return Lifecycle.State.STARTED;
        }

        @Override
        public SubscriptionDispatcher start() throws Exception {
            if (shouldThrow) {
                throw throwNoSuchBeanDefinitionException();
            }
            return this;
        }

        @Override
        public SubscriptionDispatcher stop() throws Exception {
            if (shouldThrow) {
                throw throwNoSuchBeanDefinitionException();
            }
            return this;
        }
    }
}
