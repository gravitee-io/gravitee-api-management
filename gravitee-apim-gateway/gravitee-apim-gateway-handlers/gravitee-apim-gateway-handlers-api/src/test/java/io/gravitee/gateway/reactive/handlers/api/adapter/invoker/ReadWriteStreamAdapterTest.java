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
package io.gravitee.gateway.reactive.handlers.api.adapter.invoker;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainRequest;
import io.gravitee.gateway.reactive.policy.adapter.context.ExecutionContextAdapter;
import io.gravitee.gateway.reactive.policy.adapter.context.RequestAdapter;
import io.reactivex.rxjava3.core.CompletableEmitter;
import io.reactivex.rxjava3.core.Flowable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ReadWriteStreamAdapterTest {

    protected static final String MOCK_EXCEPTION_MESSAGE = "Mock exception";

    @Mock
    private ExecutionContextAdapter ctx;

    @Mock
    private RequestAdapter requestAdapter;

    @Mock
    private HttpPlainExecutionContext syncCtx;

    @Mock
    private HttpPlainRequest request;

    @Mock
    private CompletableEmitter nexEmitter;

    @Captor
    private ArgumentCaptor<Runnable> onResumeCaptor;

    @Test
    public void shouldSubscribeWhenInvokingResumeHandler() throws InterruptedException {
        final Handler<Buffer> bodyHandler = mock(Handler.class);
        final Handler<Void> endHandler = mock(Handler.class);

        final Buffer requestChunk1 = Buffer.buffer("chunk1");
        final Buffer requestChunk2 = Buffer.buffer("chunk2");

        when(ctx.request()).thenReturn(requestAdapter);
        when(ctx.getDelegate()).thenReturn(syncCtx);
        when(syncCtx.request()).thenReturn(request);
        when(requestAdapter.getBodyHandler()).thenReturn(bodyHandler);
        when(requestAdapter.getEndHandler()).thenReturn(endHandler);

        new ReadWriteStreamAdapter(ctx, nexEmitter);

        // Verify the onResume handler has been set.
        verify(requestAdapter).onResume(onResumeCaptor.capture());

        final CountDownLatch latch = new CountDownLatch(1);
        final Flowable<Buffer> chunks = Flowable.just(requestChunk1, requestChunk2).doOnTerminate(latch::countDown);
        when(request.chunks()).thenReturn(chunks);

        // Force call of onResume handler.
        final Runnable onResume = onResumeCaptor.getValue();
        onResume.run();

        // Wait for subscription to complete and check no error happened on the emitter.
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        verifyNoInteractions(nexEmitter);
        verify(bodyHandler).handle(requestChunk1);
        verify(bodyHandler).handle(requestChunk2);
        verify(endHandler).handle(null);
    }

    @Test
    public void shouldErrorWhenErrorOccursWhileReadingChunks() throws InterruptedException {
        final Handler<Buffer> bodyHandler = mock(Handler.class);
        final Handler<Void> endHandler = mock(Handler.class);

        when(ctx.request()).thenReturn(requestAdapter);
        when(ctx.getDelegate()).thenReturn(syncCtx);
        when(syncCtx.request()).thenReturn(request);
        when(requestAdapter.getBodyHandler()).thenReturn(bodyHandler);
        when(requestAdapter.getEndHandler()).thenReturn(endHandler);

        new ReadWriteStreamAdapter(ctx, nexEmitter);

        // Verify the onResume handler has been set.
        verify(requestAdapter).onResume(onResumeCaptor.capture());

        final CountDownLatch latch = new CountDownLatch(1);
        final Flowable<Buffer> chunks = Flowable
            .<Buffer>error(new RuntimeException(MOCK_EXCEPTION_MESSAGE))
            .doOnTerminate(latch::countDown);
        when(request.chunks()).thenReturn(chunks);

        // Force call of onResume handler.
        final Runnable onResume = onResumeCaptor.getValue();
        onResume.run();

        // Wait for subscription to complete and check no error happened on the emitter.
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        verify(nexEmitter).tryOnError(argThat(t -> t.getMessage().equals(MOCK_EXCEPTION_MESSAGE)));
        verifyNoInteractions(bodyHandler);
        verifyNoInteractions(endHandler);
    }
}
