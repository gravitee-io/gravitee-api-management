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
package io.gravitee.gateway.jupiter.handlers.api.adapter.invoker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Invoker;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.context.Response;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionFailureException;
import io.gravitee.gateway.jupiter.policy.adapter.context.ExecutionContextAdapter;
import io.gravitee.gateway.jupiter.policy.adapter.context.RequestAdapter;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.Flowable;
import io.reactivex.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private RequestExecutionContext ctx;

    private InvokerAdapter cut;

    @BeforeEach
    public void init() {
        cut = new InvokerAdapter(invoker);
    }

    @Test
    public void shouldCompleteAndSetChunkedBody() {
        when(ctx.response()).thenReturn(response);

        // Simulate the ConnectionHandlerAdapter behavior by completing the nextEmitter (this will complete the InvokerAdapter execution).
        mockComplete();

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertComplete();

        // Verify the response body has been set.
        verify(response).chunks(any(FlowableProxyResponse.class));
    }

    @Test
    public void shouldErrorWhenExceptionOccurs() {
        when(ctx.response()).thenReturn(response);
        when(ctx.interruptWith(any(ExecutionFailure.class)))
            .thenAnswer(i -> Completable.error(new InterruptionFailureException(i.getArgument(0))));

        doThrow(new RuntimeException(MOCK_EXCEPTION_MESSAGE))
            .when(invoker)
            .invoke(any(ExecutionContext.class), any(ReadWriteStream.class), any(Handler.class));

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertError(
            e -> {
                assertTrue(e instanceof InterruptionFailureException);
                assertEquals(HttpStatusCode.BAD_GATEWAY_502, ((InterruptionFailureException) e).getExecutionFailure().statusCode());
                return true;
            }
        );
        verify(response).chunks(Flowable.empty());
    }

    @Test
    public void shouldRestoreContextWhenInvokerExecutionCompleted() {
        final ExecutionContextAdapter adaptedExecutionContext = mock(ExecutionContextAdapter.class);

        when(ctx.getInternalAttribute(io.gravitee.gateway.jupiter.api.context.ExecutionContext.ATTR_ADAPTED_CONTEXT))
            .thenReturn(adaptedExecutionContext);
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
    public void shouldRestoreContextWhenInvokerExecutionCancelled() {
        final ExecutionContextAdapter adaptedExecutionContext = mock(ExecutionContextAdapter.class);

        when(ctx.getInternalAttribute(io.gravitee.gateway.jupiter.api.context.ExecutionContext.ATTR_ADAPTED_CONTEXT))
            .thenReturn(adaptedExecutionContext);
        when(adaptedExecutionContext.getDelegate()).thenReturn(ctx);
        when(adaptedExecutionContext.request()).thenReturn(adaptedRequest);
        when(ctx.response()).thenReturn(response);

        final TestObserver<Void> obs = cut.invoke(ctx).test(true);

        obs.assertNotComplete();

        verify(adaptedExecutionContext).restore();
    }

    @Test
    public void shouldRestoreContextWhenInvokerExecutionError() {
        final ExecutionContextAdapter adaptedExecutionContext = mock(ExecutionContextAdapter.class);

        when(ctx.getInternalAttribute(io.gravitee.gateway.jupiter.api.context.ExecutionContext.ATTR_ADAPTED_CONTEXT))
            .thenReturn(adaptedExecutionContext);
        when(adaptedExecutionContext.getDelegate()).thenReturn(ctx);
        when(adaptedExecutionContext.request()).thenReturn(adaptedRequest);
        when(ctx.response()).thenReturn(response);
        when(ctx.interruptWith(any(ExecutionFailure.class)))
            .thenAnswer(i -> Completable.error(new InterruptionFailureException(i.getArgument(0))));

        doThrow(new RuntimeException(MOCK_EXCEPTION_MESSAGE))
            .when(invoker)
            .invoke(any(ExecutionContext.class), any(ReadWriteStream.class), any(Handler.class));

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertError(
            e -> {
                assertTrue(e instanceof InterruptionFailureException);
                assertEquals(HttpStatusCode.BAD_GATEWAY_502, ((InterruptionFailureException) e).getExecutionFailure().statusCode());
                return true;
            }
        );
        verify(adaptedExecutionContext).restore();
        verify(response).chunks(Flowable.empty());
    }

    private void mockComplete() {
        doAnswer(
                invocation -> {
                    ConnectionHandlerAdapter connectionHandlerAdapter = invocation.getArgument(2);
                    final Try<Object> nextEmitter = ReflectionUtils.tryToReadFieldValue(
                        ConnectionHandlerAdapter.class,
                        "nextEmitter",
                        connectionHandlerAdapter
                    );
                    ((CompletableEmitter) nextEmitter.get()).onComplete();
                    return null;
                }
            )
            .when(invoker)
            .invoke(any(ExecutionContext.class), any(ReadWriteStream.class), any(Handler.class));
    }
}
