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
package io.gravitee.gateway.reactive.handlers.api.v4.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import appender.MemoryAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.component.LifecycleComponent;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class ApiProductRuntimeRegistryTest {

    private static final String PRODUCT_ID = "product-1";
    private static final String ENV_ID = "env-1";
    private static final long DRAIN_TIMEOUT_MS = 1_000;

    private final TestScheduler scheduler = new TestScheduler();
    private final List<RecordingComponent> built = new ArrayList<>();
    private final MemoryAppender memoryAppender = new MemoryAppender();
    private RuntimeException failNextBuild;
    private final ApiProductRuntimeFactory runtimeFactory = mock(ApiProductRuntimeFactory.class);
    private ApiProductRuntimeRegistry cut;

    @BeforeEach
    void setUpRegistry() {
        when(runtimeFactory.create(any(), any())).thenAnswer(invocation -> {
            if (failNextBuild != null) {
                RuntimeException failure = failNextBuild;
                failNextBuild = null;
                throw failure;
            }
            ReactableApiProduct product = invocation.getArgument(0);
            RecordingComponent component = new RecordingComponent();
            built.add(component);
            return new ApiProductRuntime(product, null, null, null, List.of(component), EmptyProductResourceLifecycleManager.INSTANCE);
        });
        cut = new ApiProductRuntimeRegistry(runtimeFactory, DRAIN_TIMEOUT_MS, scheduler);
    }

    @BeforeEach
    void captureLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(ApiProductRuntimeRegistry.class);
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(Level.DEBUG);
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }

    @AfterEach
    void releaseLogs() {
        ((Logger) LoggerFactory.getLogger(ApiProductRuntimeRegistry.class)).detachAppender(memoryAppender);
    }

    @Test
    void should_start_and_publish_the_runtime_of_a_deployed_product() {
        cut.deploy(product("v1"));

        ApiProductRuntime runtime = cut.acquire(PRODUCT_ID, ENV_ID);

        assertThat(runtime).isNotNull();
        assertThat(runtime.product().getVersion()).isEqualTo("v1");
        assertThat(built.get(0).lifecycleState()).isEqualTo(Lifecycle.State.STARTED);
    }

    @Test
    void should_return_no_runtime_for_an_unknown_product() {
        assertThat(cut.acquire(PRODUCT_ID, ENV_ID)).isNull();
        assertThat(cut.acquire(PRODUCT_ID, "other-env")).isNull();
    }

    @Nested
    class Swap {

        @Test
        void should_start_the_new_runtime_before_publishing_it() {
            cut.deploy(product("v1"));
            ApiProductRuntime old = cut.acquire(PRODUCT_ID, ENV_ID);

            cut.deploy(product("v2"));

            assertThat(built.get(1).startedWhileOldWas).isEqualTo(Lifecycle.State.STARTED);
            assertThat(built.get(1).versionServedWhileStarting).isEqualTo("v1");
            assertThat(cut.acquire(PRODUCT_ID, ENV_ID).product().getVersion()).isEqualTo("v2");
            old.release();
        }

        @Test
        void should_keep_serving_requests_pinned_to_the_old_runtime_until_they_end() {
            cut.deploy(product("v1"));
            ApiProductRuntime pinned = cut.acquire(PRODUCT_ID, ENV_ID);

            cut.deploy(product("v2"));
            scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);

            assertThat(built.get(0).lifecycleState()).isEqualTo(Lifecycle.State.STARTED);

            pinned.release();
            scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

            assertThat(built.get(0).lifecycleState()).isEqualTo(Lifecycle.State.STOPPED);
            assertThat(built.get(1).lifecycleState()).isEqualTo(Lifecycle.State.STARTED);
            assertThat(warnings()).isEmpty();
        }

        @Test
        void should_stop_an_idle_old_runtime_right_away() {
            cut.deploy(product("v1"));

            cut.deploy(product("v2"));
            scheduler.triggerActions();

            assertThat(built.get(0).lifecycleState()).isEqualTo(Lifecycle.State.STOPPED);
        }

        @Test
        void should_stop_the_old_runtime_on_timeout_and_log_it() {
            cut.deploy(product("v1"));
            cut.acquire(PRODUCT_ID, ENV_ID);

            cut.deploy(product("v2"));
            scheduler.advanceTimeBy(DRAIN_TIMEOUT_MS - 100, TimeUnit.MILLISECONDS);
            assertThat(built.get(0).lifecycleState()).isEqualTo(Lifecycle.State.STARTED);

            scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

            assertThat(built.get(0).lifecycleState()).isEqualTo(Lifecycle.State.STOPPED);
            assertThat(warnings()).singleElement().asString().contains(PRODUCT_ID).contains("1 request(s)");
        }

        @Test
        void should_never_hand_out_a_runtime_that_is_being_retired() {
            cut.deploy(product("v1"));
            ApiProductRuntime old = cut.acquire(PRODUCT_ID, ENV_ID);
            old.release();

            cut.deploy(product("v2"));

            assertThat(old.tryAcquire()).isFalse();
            assertThat(cut.acquire(PRODUCT_ID, ENV_ID).product().getVersion()).isEqualTo("v2");
        }

        @Test
        void should_refuse_a_runtime_that_cannot_be_built_and_keep_the_old_one() {
            cut.deploy(product("v1"));
            failNextBuild = new IllegalStateException("policy plugin missing");

            assertThatThrownBy(() -> cut.deploy(product("v2")))
                .hasMessageContaining(PRODUCT_ID)
                .hasRootCauseMessage("policy plugin missing");
            scheduler.advanceTimeBy(DRAIN_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            assertThat(cut.acquire(PRODUCT_ID, ENV_ID).product().getVersion()).isEqualTo("v1");
            assertThat(built.get(0).lifecycleState()).isEqualTo(Lifecycle.State.STARTED);
        }
    }

    @Nested
    class FailedStart {

        @Test
        void should_stop_what_a_runtime_started_before_failing_and_keep_the_old_one() {
            ApiProductRuntime old = deployWithComponents("v1", new RecordingComponent());
            RecordingComponent startedFirst = new RecordingComponent();

            assertThatThrownBy(() -> deployWithComponents("v2", startedFirst, new FailingComponent())).hasRootCauseMessage(
                "resource cannot connect"
            );
            scheduler.advanceTimeBy(DRAIN_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            assertThat(startedFirst.lifecycleState()).isEqualTo(Lifecycle.State.STOPPED);
            ApiProductRuntime served = registryWithComponents.acquire(PRODUCT_ID, ENV_ID);
            assertThat(served).isSameAs(old);
            served.release();
        }

        @Test
        void should_stop_every_component_of_a_replaced_runtime_even_when_one_fails_to_stop() {
            RecordingComponent policyManager = new RecordingComponent();
            deployWithComponents("v1", policyManager, new FailingToStopComponent());

            deployWithComponents("v2", new RecordingComponent());
            scheduler.triggerActions();

            assertThat(policyManager.lifecycleState()).isEqualTo(Lifecycle.State.STOPPED);
        }

        private final List<List<LifecycleComponent<?>>> nextComponents = new ArrayList<>();
        private final ApiProductRuntimeFactory componentsFactory = mock(ApiProductRuntimeFactory.class);
        private final ApiProductRuntimeRegistry registryWithComponents = new ApiProductRuntimeRegistry(
            componentsFactory,
            DRAIN_TIMEOUT_MS,
            scheduler
        );

        {
            when(componentsFactory.create(any(), any())).thenAnswer(invocation ->
                new ApiProductRuntime(
                    invocation.getArgument(0),
                    null,
                    null,
                    null,
                    nextComponents.remove(0),
                    EmptyProductResourceLifecycleManager.INSTANCE
                )
            );
        }

        private ApiProductRuntime deployWithComponents(String version, LifecycleComponent<?>... components) {
            nextComponents.add(List.of(components));
            registryWithComponents.deploy(product(version));
            ApiProductRuntime runtime = registryWithComponents.acquire(PRODUCT_ID, ENV_ID);
            runtime.release();
            return runtime;
        }
    }

    @Nested
    class ConcurrentUpdates {

        @Test
        void should_stop_every_replaced_runtime_exactly_once_and_serve_the_last_one() throws Exception {
            int updates = 50;
            ExecutorService executor = Executors.newFixedThreadPool(8);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> deploys = new ArrayList<>();
            for (int i = 0; i < updates; i++) {
                String version = "v" + i;
                deploys.add(
                    executor.submit(() -> {
                        start.await();
                        cut.deploy(product(version));
                        return null;
                    })
                );
            }
            start.countDown();
            for (Future<?> deploy : deploys) {
                deploy.get(10, TimeUnit.SECONDS);
            }
            executor.shutdown();
            scheduler.triggerActions();

            assertThat(built).hasSize(updates);
            assertThat(built.subList(0, updates - 1)).allSatisfy(replaced -> {
                assertThat(replaced.lifecycleState()).isEqualTo(Lifecycle.State.STOPPED);
                assertThat(replaced.stops).isEqualTo(1);
            });
            assertThat(built.get(updates - 1).lifecycleState()).isEqualTo(Lifecycle.State.STARTED);
            assertThat(cut.acquire(PRODUCT_ID, ENV_ID)).isNotNull();
        }
    }

    @Nested
    class Clear {

        @Test
        void should_stop_every_runtime_right_away_and_hand_out_none() {
            cut.deploy(product("v1"));
            cut.deploy(ReactableApiProduct.builder().id("product-2").environmentId(ENV_ID).build());
            cut.acquire(PRODUCT_ID, ENV_ID);

            cut.clear();

            assertThat(built).allSatisfy(component -> assertThat(component.lifecycleState()).isEqualTo(Lifecycle.State.STOPPED));
            assertThat(cut.acquire(PRODUCT_ID, ENV_ID)).isNull();
            assertThat(cut.acquire("product-2", ENV_ID)).isNull();
        }
    }

    @Nested
    class Remove {

        @Test
        void should_unpublish_then_stop_once_drained() {
            cut.deploy(product("v1"));
            ApiProductRuntime pinned = cut.acquire(PRODUCT_ID, ENV_ID);

            cut.remove(PRODUCT_ID, ENV_ID);

            assertThat(cut.acquire(PRODUCT_ID, ENV_ID)).isNull();
            scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);
            assertThat(built.get(0).lifecycleState()).isEqualTo(Lifecycle.State.STARTED);

            pinned.release();
            scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
            assertThat(built.get(0).lifecycleState()).isEqualTo(Lifecycle.State.STOPPED);
        }

        @Test
        void should_ignore_an_unknown_product() {
            cut.remove(PRODUCT_ID, ENV_ID);

            assertThat(cut.acquire(PRODUCT_ID, ENV_ID)).isNull();
        }
    }

    private List<String> warnings() {
        return memoryAppender
            .getLoggedEvents()
            .stream()
            .filter(event -> event.getLevel() == Level.WARN)
            .map(event -> event.getFormattedMessage())
            .toList();
    }

    private static ReactableApiProduct product(String version) {
        return ReactableApiProduct.builder().id(PRODUCT_ID).environmentId(ENV_ID).version(version).apiIds(Set.of("api-1")).build();
    }

    /** Records what the previous runtime was doing while this one started. */
    private class RecordingComponent extends AbstractLifecycleComponent<RecordingComponent> {

        private Lifecycle.State startedWhileOldWas;
        private String versionServedWhileStarting;

        @Override
        protected void doStart() {
            int index = built.indexOf(this);
            if (index > 0) {
                startedWhileOldWas = built.get(index - 1).lifecycleState();
            }
            ApiProductRuntime served = cut.acquire(PRODUCT_ID, ENV_ID);
            if (served != null) {
                versionServedWhileStarting = served.product().getVersion();
                served.release();
            }
        }

        private int stops;

        @Override
        protected void doStop() {
            stops++;
        }
    }

    private static class FailingToStopComponent extends AbstractLifecycleComponent<FailingToStopComponent> {

        @Override
        protected void doStart() {}

        @Override
        protected void doStop() {
            throw new IllegalStateException("pool already closed");
        }
    }

    private static class FailingComponent extends AbstractLifecycleComponent<FailingComponent> {

        @Override
        protected void doStart() {
            throw new IllegalStateException("resource cannot connect");
        }

        @Override
        protected void doStop() {}
    }
}
