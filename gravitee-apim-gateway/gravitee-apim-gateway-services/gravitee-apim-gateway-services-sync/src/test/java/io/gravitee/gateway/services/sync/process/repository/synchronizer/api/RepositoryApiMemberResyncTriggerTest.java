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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.gateway.handlers.api.event.ApiProductChangedEvent;
import io.gravitee.gateway.handlers.api.event.ApiProductEventType;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RepositoryApiMemberResyncTriggerTest {

    @Mock
    private ApiSynchronizer apiSynchronizer;

    @Mock
    private ApiManager apiManager;

    @Mock
    private EventManager eventManager;

    @Captor
    private ArgumentCaptor<EventListener<ApiProductEventType, ApiProductChangedEvent>> listenerCaptor;

    private ThreadPoolExecutor syncDeployerExecutor;
    private RepositoryApiMemberResyncTrigger cut;

    @BeforeEach
    void setUp() {
        syncDeployerExecutor = new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        cut = new RepositoryApiMemberResyncTrigger(apiSynchronizer, apiManager, syncDeployerExecutor, eventManager);
        verify(eventManager).subscribeForEvents(listenerCaptor.capture(), eq(ApiProductEventType.DEPLOY), eq(ApiProductEventType.UPDATE));
    }

    @AfterEach
    void tearDown() {
        cut.dispose();
        syncDeployerExecutor.shutdown();
    }

    @Test
    void should_resync_then_re_evaluate_on_product_update_event() throws Exception {
        when(apiSynchronizer.resyncMemberApis(Set.of("api-1", "api-2"), Set.of("env-1"))).thenReturn(Completable.complete());

        listenerCaptor
            .getValue()
            .onEvent(
                new SimpleEvent<>(ApiProductEventType.UPDATE, new ApiProductChangedEvent("product-1", "env-1", Set.of("api-1", "api-2")))
            );

        awaitPipelineCompletion();
        verify(apiSynchronizer).resyncMemberApis(Set.of("api-1", "api-2"), Set.of("env-1"));
        verify(apiManager).reEvaluateAfterProductChange("product-1", Set.of("api-1", "api-2"));
    }

    @Test
    void should_resync_then_re_evaluate_on_product_deploy_event() throws Exception {
        when(apiSynchronizer.resyncMemberApis(Set.of("api-1"), Set.of("env-1"))).thenReturn(Completable.complete());

        listenerCaptor
            .getValue()
            .onEvent(new SimpleEvent<>(ApiProductEventType.DEPLOY, new ApiProductChangedEvent("product-1", "env-1", Set.of("api-1"))));

        awaitPipelineCompletion();
        verify(apiSynchronizer).resyncMemberApis(Set.of("api-1"), Set.of("env-1"));
        verify(apiManager).reEvaluateAfterProductChange("product-1", Set.of("api-1"));
    }

    @Test
    void should_execute_resync_before_re_evaluate() throws Exception {
        when(apiSynchronizer.resyncMemberApis(Set.of("api-1"), Set.of("env-1"))).thenReturn(Completable.complete());

        listenerCaptor
            .getValue()
            .onEvent(new SimpleEvent<>(ApiProductEventType.UPDATE, new ApiProductChangedEvent("product-1", "env-1", Set.of("api-1"))));

        awaitPipelineCompletion();
        InOrder inOrder = Mockito.inOrder(apiSynchronizer, apiManager);
        inOrder.verify(apiSynchronizer).resyncMemberApis(Set.of("api-1"), Set.of("env-1"));
        inOrder.verify(apiManager).reEvaluateAfterProductChange("product-1", Set.of("api-1"));
    }

    @Test
    void should_not_re_evaluate_when_resync_fails() throws Exception {
        when(apiSynchronizer.resyncMemberApis(Set.of("api-1"), Set.of("env-1"))).thenReturn(
            Completable.error(new RuntimeException("sync failure"))
        );

        listenerCaptor
            .getValue()
            .onEvent(new SimpleEvent<>(ApiProductEventType.UPDATE, new ApiProductChangedEvent("product-1", "env-1", Set.of("api-1"))));

        awaitPipelineCompletion();
        verify(apiManager, never()).reEvaluateAfterProductChange(any(), any());
    }

    @Test
    void should_not_resync_when_environment_or_api_ids_are_missing() {
        listenerCaptor
            .getValue()
            .onEvent(new SimpleEvent<>(ApiProductEventType.UPDATE, new ApiProductChangedEvent("product-1", null, Set.of("api-1"))));
        listenerCaptor
            .getValue()
            .onEvent(new SimpleEvent<>(ApiProductEventType.UPDATE, new ApiProductChangedEvent("product-1", "env-1", null)));
        listenerCaptor
            .getValue()
            .onEvent(new SimpleEvent<>(ApiProductEventType.UPDATE, new ApiProductChangedEvent("product-1", "env-1", Set.of())));

        verify(apiSynchronizer, never()).resyncMemberApis(any(), any());
        verify(apiManager, never()).reEvaluateAfterProductChange(any(), any());
    }

    @Test
    void should_not_block_event_thread_while_pipeline_runs_on_background_executor() throws Exception {
        CountDownLatch resyncStarted = new CountDownLatch(1);
        CountDownLatch releaseResync = new CountDownLatch(1);
        AtomicReference<String> resyncThreadName = new AtomicReference<>();

        when(apiSynchronizer.resyncMemberApis(Set.of("api-1"), Set.of("env-1"))).thenReturn(
            Completable.fromAction(() -> {
                resyncThreadName.set(Thread.currentThread().getName());
                resyncStarted.countDown();
                releaseResync.await(5, TimeUnit.SECONDS);
            })
        );

        String eventThreadName = Thread.currentThread().getName();
        long startNanos = System.nanoTime();
        listenerCaptor
            .getValue()
            .onEvent(new SimpleEvent<>(ApiProductEventType.UPDATE, new ApiProductChangedEvent("product-1", "env-1", Set.of("api-1"))));
        long eventHandlerDurationNanos = System.nanoTime() - startNanos;

        assertThat(eventHandlerDurationNanos).isLessThan(TimeUnit.MILLISECONDS.toNanos(200));
        assertThat(resyncStarted.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(resyncThreadName.get()).isNotEqualTo(eventThreadName);

        releaseResync.countDown();
        awaitPipelineCompletion();
        verify(apiManager).reEvaluateAfterProductChange("product-1", Set.of("api-1"));
    }

    @Test
    void should_remove_completed_disposable_after_pipeline() throws Exception {
        when(apiSynchronizer.resyncMemberApis(Set.of("api-1"), Set.of("env-1"))).thenReturn(Completable.complete());

        listenerCaptor
            .getValue()
            .onEvent(new SimpleEvent<>(ApiProductEventType.UPDATE, new ApiProductChangedEvent("product-1", "env-1", Set.of("api-1"))));

        awaitPipelineCompletion();
        awaitDisposableCleanup();
        assertThat(readDisposableCount()).isZero();
    }

    private void awaitPipelineCompletion() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (syncDeployerExecutor.getCompletedTaskCount() < 1 && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        Thread.sleep(50);
    }

    private void awaitDisposableCleanup() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (readDisposableCount() > 0 && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
    }

    private int readDisposableCount() throws Exception {
        Field disposablesField = RepositoryApiMemberResyncTrigger.class.getDeclaredField("disposables");
        disposablesField.setAccessible(true);
        CompositeDisposable disposables = (CompositeDisposable) disposablesField.get(cut);
        return disposables.size();
    }
}
