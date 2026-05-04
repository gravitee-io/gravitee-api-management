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
package io.gravitee.gateway.reactive.policy.adapter.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyException;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpMessageExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainRequest;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainResponse;
import io.gravitee.gateway.reactive.policy.adapter.context.ExecutionContextAdapter;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class PolicyAdapterTest {

    protected static final String MOCK_EXCEPTION_MESSAGE = "Mock exception";

    @Test
    public void shouldCompleteInErrorWhenOnMessageRequest() {
        final Policy policy = mock(Policy.class);
        final HttpMessageExecutionContext ctx = mock(HttpMessageExecutionContext.class);

        final PolicyAdapter cut = new PolicyAdapter(policy);

        final TestObserver<Void> obs = cut.onMessageRequest(ctx).test();

        obs.assertError(t -> "Cannot adapt v3 policy for message execution".equals(t.getMessage()));
    }

    @Test
    public void shouldCompleteInErrorWhenOnMessageResponse() {
        final Policy policy = mock(Policy.class);
        final HttpMessageExecutionContext ctx = mock(HttpMessageExecutionContext.class);

        final PolicyAdapter cut = new PolicyAdapter(policy);

        final TestObserver<Void> obs = cut.onMessageResponse(ctx).test();

        obs.assertError(t -> "Cannot adapt v3 policy for message execution".equals(t.getMessage()));
    }

    @Test
    public void shouldCompleteWhenExecuteRequest() throws PolicyException {
        final Policy policy = mock(Policy.class);
        final HttpPlainExecutionContext ctx = mock(HttpPlainExecutionContext.class);

        when(policy.isRunnable()).thenReturn(true);

        mockPolicyExecution(policy);

        final PolicyAdapter cut = new PolicyAdapter(policy);

        final TestObserver<Void> obs = cut.onRequest(ctx).test();

        obs.assertComplete();
    }

    @Test
    public void shouldErrorWhenExceptionOccursDuringRequest() throws PolicyException {
        final Policy policy = mock(Policy.class);
        final HttpPlainExecutionContext ctx = mock(HttpPlainExecutionContext.class);

        when(policy.isRunnable()).thenReturn(true);
        doThrow(new PolicyException(MOCK_EXCEPTION_MESSAGE))
            .when(policy)
            .execute(any(PolicyChainAdapter.class), any(ExecutionContext.class));

        final PolicyAdapter cut = new PolicyAdapter(policy);

        final TestObserver<Void> obs = cut.onRequest(ctx).test();

        obs.assertError(e -> e.getCause().getMessage().equals(MOCK_EXCEPTION_MESSAGE));
    }

    @Test
    public void shouldCompleteWhenNullStream() throws PolicyException {
        final Policy policy = mock(Policy.class);
        final HttpPlainExecutionContext ctx = mock(HttpPlainExecutionContext.class);

        when(policy.isStreamable()).thenReturn(true);
        when(policy.stream(any(PolicyChainAdapter.class), any(ExecutionContext.class))).thenReturn(null);

        final PolicyAdapter cut = new PolicyAdapter(policy);

        final TestObserver<Void> obs = cut.onRequest(ctx).test();

        obs.assertComplete();
    }

    @Test
    public void shouldCompleteWhenRequestStream() throws PolicyException {
        final Buffer requestBody = Buffer.buffer("body");
        final Buffer policyChunk1 = Buffer.buffer("policyChunk1");
        final Buffer policyChunk2 = Buffer.buffer("policyChunk2");
        final Policy policy = mock(Policy.class);
        final HttpPlainRequest request = mock(HttpPlainRequest.class);
        final HttpPlainExecutionContext ctx = mock(HttpPlainExecutionContext.class);
        final ReadWriteStream<Buffer> stream = mock(ReadWriteStream.class);
        final ArgumentCaptor<Maybe<Buffer>> requestBodyCaptor = ArgumentCaptor.forClass(Maybe.class);

        when(ctx.request()).thenReturn(request);
        when(request.body()).thenReturn(Maybe.just(requestBody));
        when(policy.isStreamable()).thenReturn(true);
        when(policy.stream(any(PolicyChainAdapter.class), any(ExecutionContext.class))).thenReturn(stream);

        // Simulate a policy that produces multiple buffers in the stream.
        doAnswer(invocation -> {
            Handler<Buffer> bodyHandler = invocation.getArgument(0);
            bodyHandler.handle(policyChunk1);
            bodyHandler.handle(policyChunk2);
            return null;
        })
            .when(stream)
            .bodyHandler(any(Handler.class));

        doAnswer(invocation -> {
            Handler<Void> endHandler = invocation.getArgument(0);
            endHandler.handle(null);
            return null;
        })
            .when(stream)
            .endHandler(any(Handler.class));

        final PolicyAdapter cut = new PolicyAdapter(policy);

        final TestObserver<Void> obs = cut.onRequest(ctx).test();

        obs.assertComplete();

        // Verify expected stream writes.
        verify(stream).write(requestBody);
        verify(stream).end();

        // Verify the new request body.
        verify(request).body(argThat(b -> b.toString().equals(policyChunk1.toString() + policyChunk2.toString())));
    }

    @Test
    public void shouldCompleteWhenResponseStream() throws PolicyException {
        final Buffer responseBody = Buffer.buffer("body");
        final Buffer policyChunk1 = Buffer.buffer("policyChunk1");
        final Buffer policyChunk2 = Buffer.buffer("policyChunk2");
        final Policy policy = mock(Policy.class);
        final HttpPlainResponse response = mock(HttpPlainResponse.class);
        final HttpPlainExecutionContext ctx = mock(HttpPlainExecutionContext.class);
        final ReadWriteStream<Buffer> stream = mock(ReadWriteStream.class);

        when(ctx.response()).thenReturn(response);
        when(response.body()).thenReturn(Maybe.just(responseBody));
        when(policy.isStreamable()).thenReturn(true);
        when(policy.stream(any(PolicyChainAdapter.class), any(ExecutionContext.class))).thenReturn(stream);

        // Simulate a policy that produces multiple buffers in the stream.
        doAnswer(invocation -> {
            Handler<Buffer> bodyHandler = invocation.getArgument(0);
            bodyHandler.handle(policyChunk1);
            bodyHandler.handle(policyChunk2);
            return null;
        })
            .when(stream)
            .bodyHandler(any(Handler.class));

        doAnswer(invocation -> {
            Handler<Void> endHandler = invocation.getArgument(0);
            endHandler.handle(null);
            return null;
        })
            .when(stream)
            .endHandler(any(Handler.class));

        final PolicyAdapter cut = new PolicyAdapter(policy);

        final TestObserver<Void> obs = cut.onResponse(ctx).test();

        obs.assertComplete();

        // Verify expected stream writes.
        verify(stream).write(responseBody);
        verify(stream).end();

        // Verify the new response body.
        verify(response).body(argThat(b -> b.toString().equals((policyChunk1.toString() + policyChunk2.toString()))));
    }

    @Test
    public void shouldErrorWhenExceptionOccursDuringStream() throws PolicyException {
        final Policy policy = mock(Policy.class);
        final HttpPlainRequest request = mock(HttpPlainRequest.class);
        final HttpPlainExecutionContext ctx = mock(HttpPlainExecutionContext.class);
        final ReadWriteStream<Buffer> stream = mock(ReadWriteStream.class);

        when(ctx.request()).thenReturn(request);
        when(policy.isStreamable()).thenReturn(true);
        when(policy.stream(any(PolicyChainAdapter.class), any(ExecutionContext.class))).thenReturn(stream);

        doThrow(new RuntimeException(MOCK_EXCEPTION_MESSAGE)).when(stream).endHandler(any(Handler.class));

        final PolicyAdapter cut = new PolicyAdapter(policy);
        final TestObserver<Void> obs = cut.onRequest(ctx).test();

        obs.assertError(e -> e.getCause().getMessage().equals(MOCK_EXCEPTION_MESSAGE));
    }

    @Test
    public void shouldRestoreContextWhenPolicyExecutionCompleted() throws PolicyException {
        final Policy policy = mock(Policy.class);
        final HttpPlainExecutionContext ctx = mock(HttpPlainExecutionContext.class);
        final ExecutionContextAdapter adaptedExecutionContext = mock(ExecutionContextAdapter.class);

        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ADAPTED_CONTEXT)).thenReturn(adaptedExecutionContext);
        when(policy.isRunnable()).thenReturn(true);
        mockPolicyExecution(policy);

        final PolicyAdapter cut = new PolicyAdapter(policy);

        final TestObserver<Void> obs = cut.onRequest(ctx).test();

        obs.assertComplete();
        verify(adaptedExecutionContext).restore();
    }

    @Test
    public void shouldRestoreContextWhenPolicyExecutionCancelled() throws PolicyException {
        final Policy policy = mock(Policy.class);
        final HttpPlainExecutionContext ctx = mock(HttpPlainExecutionContext.class);
        final ExecutionContextAdapter adaptedExecutionContext = mock(ExecutionContextAdapter.class);

        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ADAPTED_CONTEXT)).thenReturn(adaptedExecutionContext);
        when(policy.isRunnable()).thenReturn(true);

        final PolicyAdapter cut = new PolicyAdapter(policy);

        final TestObserver<Void> obs = cut.onRequest(ctx).test(true);

        obs.assertNotComplete();
        verify(adaptedExecutionContext).restore();
    }

    @Test
    public void shouldRestoreContextWhenPolicyExecutionError() throws PolicyException {
        final Policy policy = mock(Policy.class);
        final HttpPlainRequest request = mock(HttpPlainRequest.class);
        final HttpPlainExecutionContext ctx = mock(HttpPlainExecutionContext.class);
        final ExecutionContextAdapter adaptedExecutionContext = mock(ExecutionContextAdapter.class);
        final ReadWriteStream<Buffer> stream = mock(ReadWriteStream.class);

        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ADAPTED_CONTEXT)).thenReturn(adaptedExecutionContext);
        when(policy.isRunnable()).thenReturn(true);
        mockPolicyExecution(policy);

        when(ctx.request()).thenReturn(request);
        when(policy.isStreamable()).thenReturn(true);
        when(policy.stream(any(PolicyChainAdapter.class), any(ExecutionContext.class))).thenReturn(stream);

        // Error during stream.
        doThrow(new RuntimeException(MOCK_EXCEPTION_MESSAGE)).when(stream).endHandler(any(Handler.class));

        final PolicyAdapter cut = new PolicyAdapter(policy);

        final TestObserver<Void> obs = cut.onRequest(ctx).test();

        obs.assertError(e -> e.getCause().getMessage().equals(MOCK_EXCEPTION_MESSAGE));
        verify(adaptedExecutionContext).restore();
    }

    // -------------------------------------------------------------------------
    // PEN-88: Reproduce DoS — V3 policy blocks the subscriber (event loop) thread
    // -------------------------------------------------------------------------

    /**
     * Demonstrates the root cause of PEN-88:
     * policyExecute() runs policy.execute() synchronously on whatever thread
     * calls subscribe() — in production that is the Vert.x event loop thread.
     *
     * A blocking script (while(true){}) will pin the thread, preventing any
     * other I/O from being processed on that core.
     */
    @Test
    void pen88_shouldRunPolicyOnCallerThread_demonstratesEventLoopBlock() throws PolicyException {
        final Policy policy = mock(Policy.class);
        final HttpPlainExecutionContext ctx = mock(HttpPlainExecutionContext.class);
        final AtomicReference<String> policyThread = new AtomicReference<>();

        when(policy.isRunnable()).thenReturn(true);
        doAnswer(invocation -> {
            policyThread.set(Thread.currentThread().getName());
            PolicyChainAdapter chain = invocation.getArgument(0);
            chain.doNext(mock(Request.class), mock(Response.class));
            return null;
        })
            .when(policy)
            .execute(any(PolicyChainAdapter.class), any(ExecutionContext.class));

        final PolicyAdapter cut = new PolicyAdapter(policy);
        final String callerThread = Thread.currentThread().getName();

        // Subscribe synchronously — no subscribeOn(), just like the event loop does
        cut.onRequest(ctx).blockingAwait();

        // BUG: policy ran on the exact same thread that called subscribe().
        // In production this is the Vert.x I/O event loop thread.
        assertThat(policyThread.get())
            .as("PEN-88 root cause: policy.execute() blocks the caller (event loop) thread")
            .isEqualTo(callerThread);
    }

    /**
     * Demonstrates event loop starvation:
     * while the blocking V3 policy occupies the single-threaded scheduler,
     * no other task submitted to that scheduler can run.
     */
    @Test
    void pen88_shouldStarveEventLoopWhilePolicyBlocks() throws Exception {
        final Policy policy = mock(Policy.class);
        final HttpPlainExecutionContext ctx = mock(HttpPlainExecutionContext.class);

        final CountDownLatch policyStarted = new CountDownLatch(1);
        final CountDownLatch releasePolicy = new CountDownLatch(1);

        when(policy.isRunnable()).thenReturn(true);
        doAnswer(invocation -> {
            policyStarted.countDown();
            // Simulate an infinite / very long blocking script
            releasePolicy.await(10, TimeUnit.SECONDS);
            PolicyChainAdapter chain = invocation.getArgument(0);
            chain.doNext(mock(Request.class), mock(Response.class));
            return null;
        })
            .when(policy)
            .execute(any(PolicyChainAdapter.class), any(ExecutionContext.class));

        final PolicyAdapter cut = new PolicyAdapter(policy);

        // Use a single-threaded scheduler to simulate the Vert.x event loop
        var eventLoopSimulator = Schedulers.single();
        cut.onRequest(ctx).subscribeOn(eventLoopSimulator).subscribe();

        // Wait for the policy to start occupying the "event loop" thread
        assertThat(policyStarted.await(2, TimeUnit.SECONDS)).as("Policy should have started").isTrue();

        // BUG: try to submit another task to the same "event loop" — it is blocked
        CountDownLatch otherTaskLatch = new CountDownLatch(1);
        eventLoopSimulator.scheduleDirect(otherTaskLatch::countDown);

        assertThat(otherTaskLatch.await(200, TimeUnit.MILLISECONDS))
            .as("PEN-88: event loop is blocked — no other task can run while policy spins")
            .isFalse();

        // Cleanup: release the policy so the test thread can exit cleanly
        releasePolicy.countDown();
    }

    // -------------------------------------------------------------------------
    // PEN-88: Verify the fix — policy runs on worker thread, event loop is free
    // -------------------------------------------------------------------------

    @Test
    void pen88_shouldRunPolicyOnWorkerThread_whenWorkerSchedulerConfigured() throws PolicyException, InterruptedException {
        final Policy policy = mock(Policy.class);
        final HttpPlainExecutionContext ctx = mock(HttpPlainExecutionContext.class);
        final AtomicReference<String> policyThread = new AtomicReference<>();

        when(policy.isRunnable()).thenReturn(true);
        doAnswer(invocation -> {
            policyThread.set(Thread.currentThread().getName());
            PolicyChainAdapter chain = invocation.getArgument(0);
            chain.doNext(mock(Request.class), mock(Response.class));
            return null;
        })
            .when(policy)
            .execute(any(PolicyChainAdapter.class), any(ExecutionContext.class));

        // Use the production 3-arg constructor with a worker scheduler
        final PolicyAdapter cut = new PolicyAdapter(policy, Schedulers.io());
        final String callerThread = Thread.currentThread().getName();

        cut.onRequest(ctx).blockingAwait();

        // FIX: policy no longer runs on the event loop (caller) thread
        assertThat(policyThread.get())
            .as("PEN-88 fix: policy.execute() should run on a worker thread, not the event loop")
            .isNotEqualTo(callerThread);
    }

    @Test
    void pen88_shouldNotBlockEventLoop_whenWorkerSchedulerConfigured() throws Exception {
        final Policy policy = mock(Policy.class);
        final HttpPlainExecutionContext ctx = mock(HttpPlainExecutionContext.class);

        final CountDownLatch policyStarted = new CountDownLatch(1);
        final CountDownLatch releasePolicy = new CountDownLatch(1);

        when(policy.isRunnable()).thenReturn(true);
        doAnswer(invocation -> {
            policyStarted.countDown();
            releasePolicy.await(10, TimeUnit.SECONDS);
            PolicyChainAdapter chain = invocation.getArgument(0);
            chain.doNext(mock(Request.class), mock(Response.class));
            return null;
        })
            .when(policy)
            .execute(any(PolicyChainAdapter.class), any(ExecutionContext.class));

        var eventLoopSimulator = Schedulers.single();

        // FIX: PolicyAdapter uses its own worker scheduler — event loop is not blocked
        final PolicyAdapter cut = new PolicyAdapter(policy, Schedulers.io());
        cut.onRequest(ctx).subscribeOn(eventLoopSimulator).subscribe();

        assertThat(policyStarted.await(2, TimeUnit.SECONDS)).as("Policy should have started on worker thread").isTrue();

        // The event loop thread is now FREE — another task can run immediately
        CountDownLatch otherTaskLatch = new CountDownLatch(1);
        eventLoopSimulator.scheduleDirect(otherTaskLatch::countDown);

        assertThat(otherTaskLatch.await(2, TimeUnit.SECONDS))
            .as("PEN-88 fix: event loop is free while V3 policy runs on worker thread")
            .isTrue();

        releasePolicy.countDown();
    }

    private void mockPolicyExecution(Policy policy) throws PolicyException {
        doAnswer(invocation -> {
            PolicyChainAdapter policyChain = invocation.getArgument(0);
            policyChain.doNext(mock(Request.class), mock(Response.class));
            return null;
        })
            .when(policy)
            .execute(any(PolicyChainAdapter.class), any(ExecutionContext.class));
    }
}
