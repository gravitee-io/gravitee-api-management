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
package io.gravitee.gateway.reactive.core.tracing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.tracing.Tracer;
import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.node.api.opentelemetry.Span;
import io.gravitee.node.opentelemetry.tracer.noop.NoOpSpan;
import io.gravitee.node.opentelemetry.tracer.noop.NoOpTracer;
import io.gravitee.reporter.api.v4.metric.Metrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class TracingHookTest {

    private DefaultExecutionContext ctx;
    private NoOpTracer spyNoopTracer;
    private TracingHook tracingHook;

    @BeforeEach
    public void beforeEach() {
        ctx = new DefaultExecutionContext(null, null);
        ctx.metrics(mock(Metrics.class));
        spyNoopTracer = spy(new NoOpTracer());
        ctx.tracer(new Tracer(null, spyNoopTracer));
        tracingHook = new TracingHook("test");
    }

    @Test
    public void should_start_span_on_pre_step() {
        tracingHook.pre("any", ctx, ExecutionPhase.REQUEST).test().assertComplete();

        verify(spyNoopTracer).startSpanFrom(any(), any());
        assertThat(ctx.<Span>getInternalAttribute("tracing-span-any")).isNotNull();
    }

    @Test
    public void should_end_span_on_post_step() {
        ctx.putInternalAttribute("tracing-span-any", NoOpSpan.asDefault());
        tracingHook.post("any", ctx, ExecutionPhase.REQUEST).test().assertComplete();

        verify(spyNoopTracer).end(any(), any());
        assertThat(ctx.<Span>getInternalAttribute("tracing-span-any")).isNull();
    }

    @Test
    public void should_do_nothing_without_span_on_post_step() {
        tracingHook.post("any", ctx, ExecutionPhase.REQUEST).test().assertComplete();

        verifyNoInteractions(spyNoopTracer);
    }

    @Test
    public void should_end_span_on_error_step() {
        ctx.putInternalAttribute("tracing-span-any", NoOpSpan.asDefault());
        tracingHook.error("any", ctx, ExecutionPhase.REQUEST, new RuntimeException()).test().assertComplete();

        verify(spyNoopTracer).endOnError(any(), any(), any(Throwable.class));
        assertThat(ctx.<Span>getInternalAttribute("tracing-span-any")).isNull();
    }

    @Test
    public void should_do_nothing_without_span_on_error_step() {
        tracingHook.error("any", ctx, ExecutionPhase.REQUEST, new RuntimeException()).test().assertComplete();

        verifyNoInteractions(spyNoopTracer);
    }

    @Test
    public void should_end_span_on_interrupt_step() {
        ctx.putInternalAttribute("tracing-span-any", NoOpSpan.asDefault());
        tracingHook.interrupt("any", ctx, ExecutionPhase.REQUEST).test().assertComplete();

        verify(spyNoopTracer).end(any(), any());
        assertThat(ctx.<Span>getInternalAttribute("tracing-span-any")).isNull();
    }

    @Test
    public void should_do_nothing_without_span_on_interrupt_step() {
        tracingHook.interrupt("any", ctx, ExecutionPhase.REQUEST).test().assertComplete();

        verifyNoInteractions(spyNoopTracer);
    }

    @Test
    public void should_end_span_on_interrupt_with_step() {
        ctx.putInternalAttribute("tracing-span-any", NoOpSpan.asDefault());
        ExecutionFailure failure = new ExecutionFailure().message("failure").key("key").contentType("contentType");
        tracingHook.interruptWith("any", ctx, ExecutionPhase.REQUEST, failure).test().assertComplete();

        verify(spyNoopTracer).endOnError(any(), any(), anyString());
        assertThat(ctx.<Span>getInternalAttribute("tracing-span-any")).isNull();
    }

    @Test
    public void should_do_nothing_without_span_on_interrupt_with_step() {
        tracingHook.interruptWith("any", ctx, ExecutionPhase.REQUEST, new ExecutionFailure()).test().assertComplete();

        verifyNoInteractions(spyNoopTracer);
    }
}
