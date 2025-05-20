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

import static io.gravitee.common.http.HttpStatusCode.INTERNAL_SERVER_ERROR_500;
import static io.gravitee.gateway.reactive.handlers.api.adapter.invoker.InvokerAdapter.CLIENT_ABORTED_DURING_RESPONSE_ERROR;
import static io.gravitee.gateway.reactive.handlers.api.adapter.invoker.InvokerAdapter.CLIENT_ABORTED_DURING_RESPONSE_ERROR_MESSAGE;
import static io.gravitee.gateway.reactive.handlers.api.adapter.invoker.InvokerAdapter.GATEWAY_CLIENT_CONNECTION_ERROR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.Invoker;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.gateway.reactive.policy.adapter.context.ExecutionContextAdapter;
import io.gravitee.gateway.reactive.policy.adapter.context.RequestAdapter;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableEmitter;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class InvokerAdapterTest {

    protected static final String MOCK_EXCEPTION_MESSAGE = "Mock exception";

    @Mock
    private Invoker invoker;

    @Mock
    private RequestAdapter adaptedRequest;

    @Mock
    private Response response;

    @Mock
    private ExecutionContext ctx;

    @Mock
    private Metrics metrics;

    private InvokerAdapter cut;

    @BeforeEach
    public void init() {
        cut = new InvokerAdapter(invoker);
    }

    @Test
    void shouldCompleteAndSetChunkedBody() {
        when(ctx.response()).thenReturn(response);

        // Simulate the ConnectionHandlerAdapter behavior by completing the nextEmitter (this will complete the InvokerAdapter execution).
        mockComplete();

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertComplete();

        // Verify the response body has been set.
        verify(response).chunks(any(FlowableProxyResponse.class));
    }

    @Test
    void shouldGetIdFromInvokerClassName() {
        final String id = cut.getId();

        assertNotNull(id);
        assertTrue(id.startsWith("invoker"));
    }

    @Test
    void shouldInterruptWith502WhenExceptionOccurs() {
        when(ctx.response()).thenReturn(response);
        when(ctx.interruptWith(any(ExecutionFailure.class)))
            .thenAnswer(i -> Completable.error(new InterruptionFailureException(i.getArgument(0))));

        doThrow(new RuntimeException(MOCK_EXCEPTION_MESSAGE))
            .when(invoker)
            .invoke(any(io.gravitee.gateway.api.ExecutionContext.class), any(ReadWriteStream.class), any(Handler.class));

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertError(e -> {
            assertTrue(e instanceof InterruptionFailureException);
            assertEquals(HttpStatusCode.BAD_GATEWAY_502, ((InterruptionFailureException) e).getExecutionFailure().statusCode());
            assertEquals(GATEWAY_CLIENT_CONNECTION_ERROR, ((InterruptionFailureException) e).getExecutionFailure().key());
            return true;
        });
        verify(response).chunks(Flowable.empty());
    }

    @Test
    void shouldInterruptAndPropagateFailureWhenInterruptionFailureExceptionOccurs() {
        when(ctx.response()).thenReturn(response);
        when(ctx.interruptWith(any(ExecutionFailure.class)))
            .thenAnswer(i -> Completable.error(new InterruptionFailureException(i.getArgument(0))));

        final String failureContentType = "text/plain";
        final String failureKey = "INTERNAL_ERROR";

        final InterruptionFailureException interruptionFailureException = new InterruptionFailureException(
            new ExecutionFailure(INTERNAL_SERVER_ERROR_500).contentType(failureContentType).message(MOCK_EXCEPTION_MESSAGE).key(failureKey)
        );

        final ArgumentCaptor<ConnectionHandlerAdapter> connectionHandlerAdapterCaptor = ArgumentCaptor.forClass(
            ConnectionHandlerAdapter.class
        );

        doNothing()
            .when(invoker)
            .invoke(
                any(io.gravitee.gateway.api.ExecutionContext.class),
                any(ReadWriteStream.class),
                connectionHandlerAdapterCaptor.capture()
            );

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        // Simulate an InterruptionFailureException from the connection handler.
        final ConnectionHandlerAdapter connectionHandlerAdapter = connectionHandlerAdapterCaptor.getValue();
        final CompletableEmitter nextEmitter = (CompletableEmitter) ReflectionTestUtils.getField(connectionHandlerAdapter, "nextEmitter");
        nextEmitter.tryOnError(interruptionFailureException);

        obs.assertError(e -> {
            assertTrue(e instanceof InterruptionFailureException);
            final ExecutionFailure executionFailure = ((InterruptionFailureException) e).getExecutionFailure();
            assertEquals(INTERNAL_SERVER_ERROR_500, executionFailure.statusCode());
            assertEquals(failureKey, executionFailure.key());
            assertEquals(failureContentType, executionFailure.contentType());
            assertEquals(MOCK_EXCEPTION_MESSAGE, executionFailure.message());
            return true;
        });
        verify(response).chunks(Flowable.empty());
    }

    @Test
    void shouldRestoreContextWhenInvokerExecutionCompleted() {
        final ExecutionContextAdapter adaptedExecutionContext = mock(ExecutionContextAdapter.class);

        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ADAPTED_CONTEXT)).thenReturn(adaptedExecutionContext);
        when(adaptedExecutionContext.getDelegate()).thenReturn(ctx);
        when(adaptedExecutionContext.request()).thenReturn(adaptedRequest);
        when(ctx.response()).thenReturn(response);

        mockComplete();

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertComplete();

        // Verify the response body has been set.
        verify(response).chunks(any(FlowableProxyResponse.class));
        verify(adaptedExecutionContext).restore();
    }

    @Test
    void shouldRestoreContextWhenInvokerExecutionCancelled() {
        final ExecutionContextAdapter adaptedExecutionContext = mock(ExecutionContextAdapter.class);

        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ADAPTED_CONTEXT)).thenReturn(adaptedExecutionContext);
        when(adaptedExecutionContext.getDelegate()).thenReturn(ctx);
        when(adaptedExecutionContext.request()).thenReturn(adaptedRequest);
        when(ctx.response()).thenReturn(response);
        when(ctx.metrics()).thenReturn(metrics);

        final TestObserver<Void> obs = cut.invoke(ctx).test(true);

        obs.assertNotComplete();

        verify(adaptedExecutionContext).restore();
        verify(metrics).setErrorKey(CLIENT_ABORTED_DURING_RESPONSE_ERROR);
        verify(metrics).setErrorMessage(CLIENT_ABORTED_DURING_RESPONSE_ERROR_MESSAGE);
    }

    @Test
    void shouldRestoreContextWhenInvokerExecutionError() {
        final ExecutionContextAdapter adaptedExecutionContext = mock(ExecutionContextAdapter.class);

        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ADAPTED_CONTEXT)).thenReturn(adaptedExecutionContext);
        when(adaptedExecutionContext.getDelegate()).thenReturn(ctx);
        when(adaptedExecutionContext.request()).thenReturn(adaptedRequest);
        when(ctx.response()).thenReturn(response);
        when(ctx.interruptWith(any(ExecutionFailure.class)))
            .thenAnswer(i -> Completable.error(new InterruptionFailureException(i.getArgument(0))));

        doThrow(new RuntimeException(MOCK_EXCEPTION_MESSAGE))
            .when(invoker)
            .invoke(any(io.gravitee.gateway.api.ExecutionContext.class), any(ReadWriteStream.class), any(Handler.class));

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertError(e -> {
            assertTrue(e instanceof InterruptionFailureException);
            assertEquals(HttpStatusCode.BAD_GATEWAY_502, ((InterruptionFailureException) e).getExecutionFailure().statusCode());
            return true;
        });
        verify(adaptedExecutionContext).restore();
        verify(response).chunks(Flowable.empty());
    }

    private void mockComplete() {
        doAnswer(invocation -> {
                ConnectionHandlerAdapter connectionHandlerAdapter = invocation.getArgument(2);
                final Try<Object> nextEmitter = ReflectionUtils.tryToReadFieldValue(
                    ConnectionHandlerAdapter.class,
                    "nextEmitter",
                    connectionHandlerAdapter
                );
                ((CompletableEmitter) nextEmitter.get()).onComplete();
                return null;
            })
            .when(invoker)
            .invoke(any(io.gravitee.gateway.api.ExecutionContext.class), any(ReadWriteStream.class), any(Handler.class));
    }
}
