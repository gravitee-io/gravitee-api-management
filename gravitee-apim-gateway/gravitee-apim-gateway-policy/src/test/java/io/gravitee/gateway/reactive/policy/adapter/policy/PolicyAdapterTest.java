/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactive.policy.adapter.policy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyException;
import io.gravitee.gateway.reactive.api.context.async.AsyncExecutionContext;
import io.gravitee.gateway.reactive.api.context.sync.SyncExecutionContext;
import io.gravitee.gateway.reactive.api.context.sync.SyncRequest;
import io.gravitee.gateway.reactive.api.context.sync.SyncResponse;
import io.gravitee.gateway.reactive.policy.adapter.policy.PolicyAdapter;
import io.gravitee.gateway.reactive.policy.adapter.policy.PolicyChainAdapter;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.observers.TestObserver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class PolicyAdapterTest {

    protected static final String MOCK_EXCEPTION_MESSAGE = "Mock exception";

    @Test
    public void shouldCompleteInErrorWhenAsyncRequest() {
        final Policy policy = mock(Policy.class);
        final AsyncExecutionContext ctx = mock(AsyncExecutionContext.class);

        final PolicyAdapter cut = new PolicyAdapter(policy);

        final TestObserver<Void> obs = cut.onAsyncRequest(ctx).test();

        obs.assertErrorMessage("Cannot adapt v3 policy for async request execution");
    }

    @Test
    public void shouldCompleteInErrorWhenAsyncResponse() {
        final Policy policy = mock(Policy.class);
        final AsyncExecutionContext ctx = mock(AsyncExecutionContext.class);

        final PolicyAdapter cut = new PolicyAdapter(policy);

        final TestObserver<Void> obs = cut.onAsyncResponse(ctx).test();

        obs.assertErrorMessage("Cannot adapt v3 policy for async response execution");
    }

    @Test
    public void shouldCompleteWhenExecuteRequest() throws PolicyException {
        final Policy policy = mock(Policy.class);
        final SyncExecutionContext ctx = mock(SyncExecutionContext.class);

        when(policy.isRunnable()).thenReturn(true);

        doAnswer(
                invocation -> {
                    PolicyChainAdapter policyChain = invocation.getArgument(0);
                    policyChain.doNext(mock(Request.class), mock(Response.class));
                    return null;
                }
            )
            .when(policy)
            .execute(any(PolicyChainAdapter.class), any(ExecutionContext.class));

        final PolicyAdapter cut = new PolicyAdapter(policy);

        final TestObserver<Void> obs = cut.onRequest(ctx).test();

        obs.assertComplete();
    }

    @Test
    public void shouldErrorWhenExceptionOccursDuringRequest() throws PolicyException {
        final Policy policy = mock(Policy.class);
        final SyncExecutionContext ctx = mock(SyncExecutionContext.class);

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
        final SyncExecutionContext ctx = mock(SyncExecutionContext.class);

        when(policy.isStreamable()).thenReturn(true);
        when(policy.stream(any(PolicyChainAdapter.class), any(ExecutionContext.class))).thenReturn(null);

        final PolicyAdapter cut = new PolicyAdapter(policy);

        final TestObserver<Void> obs = cut.onRequest(ctx).test();

        obs.assertComplete();
    }

    @Test
    public void shouldCompleteWhenRequestStream() throws PolicyException {
        final Buffer requestChunk1 = Buffer.buffer("chunk1");
        final Buffer requestChunk2 = Buffer.buffer("chunk2");
        final Buffer policyChunk1 = Buffer.buffer("policyChunk1");
        final Buffer policyChunk2 = Buffer.buffer("policyChunk2");
        final Policy policy = mock(Policy.class);
        final SyncRequest request = mock(SyncRequest.class);
        final SyncExecutionContext ctx = mock(SyncExecutionContext.class);
        final ReadWriteStream<Buffer> stream = mock(ReadWriteStream.class);
        final ArgumentCaptor<Maybe<Buffer>> requestBodyCaptor = ArgumentCaptor.forClass(Maybe.class);

        when(ctx.request()).thenReturn(request);
        when(request.getChunkedBody()).thenReturn(Flowable.just(requestChunk1, requestChunk2));
        when(policy.isStreamable()).thenReturn(true);
        when(policy.stream(any(PolicyChainAdapter.class), any(ExecutionContext.class))).thenReturn(stream);
        when(request.setBody(requestBodyCaptor.capture())).thenReturn(Completable.complete());

        // Simulate a policy that produces multiple buffers in the stream.
        doAnswer(
                invocation -> {
                    Handler<Buffer> bodyHandler = invocation.getArgument(0);
                    bodyHandler.handle(policyChunk1);
                    bodyHandler.handle(policyChunk2);
                    return null;
                }
            )
            .when(stream)
            .bodyHandler(any(Handler.class));

        doAnswer(
                invocation -> {
                    Handler<Void> endHandler = invocation.getArgument(0);
                    endHandler.handle(null);
                    return null;
                }
            )
            .when(stream)
            .endHandler(any(Handler.class));

        final PolicyAdapter cut = new PolicyAdapter(policy);

        final TestObserver<Void> obs = cut.onRequest(ctx).test();

        obs.assertComplete();

        // Verify expected stream writes.
        verify(stream).write(requestChunk1);
        verify(stream).write(requestChunk2);
        verify(stream).end();

        // Verify the new request body.
        requestBodyCaptor.getValue().test().assertValue(b -> b.toString().equals(policyChunk1.toString() + policyChunk2.toString()));
    }

    @Test
    public void shouldCompleteWhenResponseStream() throws PolicyException {
        final Buffer responseChunk1 = Buffer.buffer("chunk1");
        final Buffer responseChunk2 = Buffer.buffer("chunk2");
        final Buffer policyChunk1 = Buffer.buffer("policyChunk1");
        final Buffer policyChunk2 = Buffer.buffer("policyChunk2");
        final Policy policy = mock(Policy.class);
        final SyncResponse response = mock(SyncResponse.class);
        final SyncExecutionContext ctx = mock(SyncExecutionContext.class);
        final ReadWriteStream<Buffer> stream = mock(ReadWriteStream.class);
        final ArgumentCaptor<Maybe<Buffer>> responseBodyCaptor = ArgumentCaptor.forClass(Maybe.class);

        when(ctx.response()).thenReturn(response);
        when(response.getChunkedBody()).thenReturn(Flowable.just(responseChunk1, responseChunk2));
        when(policy.isStreamable()).thenReturn(true);
        when(policy.stream(any(PolicyChainAdapter.class), any(ExecutionContext.class))).thenReturn(stream);
        when(response.setBody(responseBodyCaptor.capture())).thenReturn(Completable.complete());

        // Simulate a policy that produces multiple buffers in the stream.
        doAnswer(
                invocation -> {
                    Handler<Buffer> bodyHandler = invocation.getArgument(0);
                    bodyHandler.handle(policyChunk1);
                    bodyHandler.handle(policyChunk2);
                    return null;
                }
            )
            .when(stream)
            .bodyHandler(any(Handler.class));

        doAnswer(
                invocation -> {
                    Handler<Void> endHandler = invocation.getArgument(0);
                    endHandler.handle(null);
                    return null;
                }
            )
            .when(stream)
            .endHandler(any(Handler.class));

        final PolicyAdapter cut = new PolicyAdapter(policy);

        final TestObserver<Void> obs = cut.onResponse(ctx).test();

        obs.assertComplete();

        // Verify expected stream writes.
        verify(stream).write(responseChunk1);
        verify(stream).write(responseChunk2);
        verify(stream).end();

        // Verify the new response body.
        responseBodyCaptor.getValue().test().assertValue(b -> b.toString().equals(policyChunk1.toString() + policyChunk2.toString()));
    }

    @Test
    public void shouldCompleteWithoutBodyWhenInterrupted() throws PolicyException {
        final Policy policy = mock(Policy.class);
        final SyncRequest request = mock(SyncRequest.class);
        final SyncExecutionContext ctx = mock(SyncExecutionContext.class);
        final ReadWriteStream<Buffer> stream = mock(ReadWriteStream.class);

        when(ctx.request()).thenReturn(request);
        when(policy.isStreamable()).thenReturn(true);
        when(policy.stream(any(PolicyChainAdapter.class), any(ExecutionContext.class))).thenReturn(stream);
        when(request.setBody(any(Maybe.class))).thenReturn(Completable.complete());
        when(ctx.isInterrupted()).thenReturn(true);

        doAnswer(
                invocation -> {
                    Handler<Void> endHandler = invocation.getArgument(0);
                    endHandler.handle(null);
                    return null;
                }
            )
            .when(stream)
            .endHandler(any(Handler.class));

        final PolicyAdapter cut = new PolicyAdapter(policy);
        final TestObserver<Void> obs = cut.onRequest(ctx).test();

        obs.assertComplete();

        verify(request, times(0)).setBody(any(Maybe.class));
    }

    @Test
    public void shouldErrorWhenExceptionOccursDuringStream() throws PolicyException {
        final Policy policy = mock(Policy.class);
        final SyncRequest request = mock(SyncRequest.class);
        final SyncExecutionContext ctx = mock(SyncExecutionContext.class);
        final ReadWriteStream<Buffer> stream = mock(ReadWriteStream.class);

        when(ctx.request()).thenReturn(request);
        when(policy.isStreamable()).thenReturn(true);
        when(policy.stream(any(PolicyChainAdapter.class), any(ExecutionContext.class))).thenReturn(stream);
        when(request.setBody(any(Maybe.class))).thenReturn(Completable.complete());

        doThrow(new RuntimeException(MOCK_EXCEPTION_MESSAGE)).when(stream).endHandler(any(Handler.class));

        final PolicyAdapter cut = new PolicyAdapter(policy);
        final TestObserver<Void> obs = cut.onRequest(ctx).test();

        obs.assertError(e -> e.getCause().getMessage().equals(MOCK_EXCEPTION_MESSAGE));
    }
}
