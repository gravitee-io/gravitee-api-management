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
package io.gravitee.gateway.reactive.policy.tracing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.tracing.Tracer;
import io.gravitee.gateway.reactive.policy.action.PolicyActionType;
import io.gravitee.node.api.opentelemetry.Span;
import io.gravitee.node.api.opentelemetry.internal.InternalRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TracingPolicyActionHookTest {

    private static final String SPAN_KEY = "tracing-span-guardian-endpoint-request-action";

    @Mock
    private HttpExecutionContext ctx;

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    private TracingPolicyActionHook hook;

    @BeforeEach
    void setUp() {
        hook = new TracingPolicyActionHook(PolicyActionType.ENDPOINT_REQUEST);
        lenient().when(ctx.getTracer()).thenReturn(tracer);
    }

    @Test
    void should_have_its_own_id() {
        assertThat(hook.id()).isEqualTo("hook-tracing-policy-action");
    }

    @Test
    void should_start_a_span_named_after_the_policy_and_the_action() {
        when(tracer.startSpanFrom(any(InternalRequest.class))).thenReturn(span);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_TRACING_VERBOSE_ENABLED)).thenReturn(true);
        lenient().when(ctx.getInternalAttribute(TracingPolicyHook.ATTR_CURRENT_POLICY_DESCRIPTION)).thenReturn("another policy");

        hook.pre("guardian", ctx, ExecutionPhase.REQUEST).test().assertResult();

        ArgumentCaptor<InternalRequest> request = ArgumentCaptor.forClass(InternalRequest.class);
        verify(tracer).startSpanFrom(request.capture());
        assertThat(request.getValue().name()).isEqualTo("REQUEST policy-guardian (endpoint-request action)");
        assertThat(request.getValue().attributes())
            .containsEntry(TracingPolicyHook.SPAN_POLICY_ATTR, "guardian")
            .containsEntry(TracingPolicyActionHook.SPAN_POLICY_ACTION_ATTR, "endpoint-request")
            .doesNotContainKey(TracingPolicyHook.SPAN_POLICY_DESCRIPTION_ATTR);
        verify(ctx).putInternalAttribute(SPAN_KEY, span);
    }

    @Test
    void should_end_the_span_without_trigger_attributes() {
        when(ctx.getInternalAttribute(SPAN_KEY)).thenReturn(span);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_TRACING_VERBOSE_ENABLED)).thenReturn(false);

        hook.post("guardian", ctx, ExecutionPhase.REQUEST).test().assertResult();

        verify(span, never()).withAttribute(anyString(), anyString());
        verify(tracer).end(span);
        verify(ctx).removeInternalAttribute(SPAN_KEY);
    }

    @Test
    void should_end_the_span_on_error_when_the_action_interrupts_with_a_failure() {
        when(ctx.getInternalAttribute(SPAN_KEY)).thenReturn(span);
        when(span.withAttribute(anyString(), any(String.class))).thenReturn(span);
        when(span.withAttribute(anyString(), any(Integer.class))).thenReturn(span);

        hook
            .interruptWith("guardian", ctx, ExecutionPhase.REQUEST, new ExecutionFailure(403).key("BLOCKED").message("blocked"))
            .test()
            .assertResult();

        verify(span).withAttribute("gravitee.execution-failure.key", "BLOCKED");
        verify(span).withAttribute("gravitee.execution-failure.status-code", 403);
        verify(tracer).endOnError(span, "blocked");
    }
}
