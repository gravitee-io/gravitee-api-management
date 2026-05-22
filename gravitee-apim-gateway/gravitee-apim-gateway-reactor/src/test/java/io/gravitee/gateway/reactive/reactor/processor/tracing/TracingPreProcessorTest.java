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
package io.gravitee.gateway.reactive.reactor.processor.tracing;

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_TRACING_ROOT_SPAN;
import static io.gravitee.gateway.reactive.core.tracing.AbstractTracingHook.SPAN_REQUEST_ID_ATTR;
import static io.gravitee.gateway.reactive.core.tracing.AbstractTracingHook.SPAN_TRANSACTION_ID_ATTR;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.reactor.processor.AbstractProcessorTest;
import io.gravitee.node.api.opentelemetry.Span;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class TracingPreProcessorTest extends AbstractProcessorTest {

    private TracingPreProcessor processor;
    private Span rootSpan;

    @BeforeEach
    public void setUp() {
        processor = new TracingPreProcessor();
        rootSpan = mock(Span.class);
    }

    @Test
    void should_set_request_id_and_transaction_id_on_root_span() {
        when(mockRequest.id()).thenReturn("req-id");
        when(mockRequest.transactionId()).thenReturn("tx-id");
        when(rootSpan.withAttribute(SPAN_REQUEST_ID_ATTR, "req-id")).thenReturn(rootSpan);
        when(rootSpan.withAttribute(SPAN_TRANSACTION_ID_ATTR, "tx-id")).thenReturn(rootSpan);
        ctx.putInternalAttribute(ATTR_INTERNAL_TRACING_ROOT_SPAN, rootSpan);

        processor.execute(ctx).test().assertComplete();

        verify(rootSpan).withAttribute(SPAN_REQUEST_ID_ATTR, "req-id");
        verify(rootSpan).withAttribute(SPAN_TRANSACTION_ID_ATTR, "tx-id");
    }

    @Test
    void should_set_only_request_id_when_transaction_id_is_null() {
        when(mockRequest.id()).thenReturn("req-id");
        when(mockRequest.transactionId()).thenReturn(null);
        when(rootSpan.withAttribute(SPAN_REQUEST_ID_ATTR, "req-id")).thenReturn(rootSpan);
        ctx.putInternalAttribute(ATTR_INTERNAL_TRACING_ROOT_SPAN, rootSpan);

        processor.execute(ctx).test().assertComplete();

        verify(rootSpan).withAttribute(SPAN_REQUEST_ID_ATTR, "req-id");
        verify(rootSpan, never()).withAttribute(SPAN_TRANSACTION_ID_ATTR, null);
    }

    @Test
    void should_do_nothing_when_no_root_span_in_context() {
        processor.execute(ctx).test().assertComplete();

        verify(rootSpan, never()).withAttribute(SPAN_REQUEST_ID_ATTR, "req-id");
    }
}
