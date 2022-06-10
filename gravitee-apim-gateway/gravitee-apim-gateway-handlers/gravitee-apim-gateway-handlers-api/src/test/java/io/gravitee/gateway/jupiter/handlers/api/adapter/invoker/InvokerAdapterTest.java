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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Invoker;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.context.Response;
import io.reactivex.CompletableEmitter;
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

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertComplete();

        // Verify the response body has been set.
        verify(response).chunks(any(FlowableProxyResponse.class));
    }

    @Test
    public void shouldErrorWhenExceptionOccurs() {
        when(ctx.response()).thenReturn(response);

        doThrow(new RuntimeException(MOCK_EXCEPTION_MESSAGE))
            .when(invoker)
            .invoke(any(ExecutionContext.class), any(ReadWriteStream.class), any(Handler.class));

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertError(e -> e.getCause().getMessage().equals(MOCK_EXCEPTION_MESSAGE));
    }
}
