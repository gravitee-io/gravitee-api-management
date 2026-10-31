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
package io.gravitee.gateway.reactive.policy.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.base.BasePolicy;
import io.gravitee.gateway.reactive.api.tracing.Tracer;
import io.gravitee.gateway.reactive.core.context.ComponentScope;
import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.node.api.opentelemetry.Span;
import io.gravitee.node.api.opentelemetry.internal.InternalRequest;
import io.reactivex.rxjava3.core.Completable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PolicyActionExecutorTest {

    private final BasePolicy first = aPolicy("first");
    private final BasePolicy second = aPolicy("second");
    private final List<String> executed = new ArrayList<>();
    private DefaultExecutionContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new DefaultExecutionContext(mock(MutableRequest.class), mock(MutableResponse.class));
    }

    @Test
    void should_execute_the_actions_in_the_given_order() {
        var cut = new PolicyActionExecutor(PolicyActionType.ENDPOINT_REQUEST, false);

        cut.executeAll(ctx, actions(first, recording("first"), second, recording("second"))).test().assertComplete();

        assertThat(executed).containsExactly("first", "second");
    }

    @Test
    void should_stop_at_the_first_failing_action() {
        var cut = new PolicyActionExecutor(PolicyActionType.ENTRYPOINT_RESPONSE, false);
        var failure = new RuntimeException("failed");

        cut.executeAll(ctx, actions(first, c -> Completable.error(failure), second, recording("second"))).test().assertError(failure);

        assertThat(executed).isEmpty();
    }

    @Test
    void should_execute_the_action_in_the_scope_of_its_policy() {
        var cut = new PolicyActionExecutor(PolicyActionType.TERMINATE, false);

        cut
            .execute(ctx, first, c -> Completable.fromRunnable(() -> executed.add(ComponentScope.peek(c).name())))
            .test()
            .assertComplete();

        assertThat(executed).containsExactly("first");
        assertThat(ComponentScope.peek(ctx)).isNull();
    }

    @Test
    void should_trace_each_action_when_tracing_is_enabled() {
        var tracer = mock(Tracer.class);
        var span = mock(Span.class);
        when(tracer.startSpanFrom(any(InternalRequest.class))).thenReturn(span);
        ctx.tracer(tracer);
        var cut = new PolicyActionExecutor(PolicyActionType.ENDPOINT_REQUEST, true);

        cut.executeAll(ctx, actions(first, recording("first"), second, recording("second"))).test().assertComplete();

        verify(tracer, org.mockito.Mockito.times(2)).startSpanFrom(any(InternalRequest.class));
        verify(tracer, org.mockito.Mockito.times(2)).end(span);
    }

    @Test
    void should_not_trace_when_tracing_is_disabled() {
        var tracer = mock(Tracer.class);
        ctx.tracer(tracer);
        var cut = new PolicyActionExecutor(PolicyActionType.ENDPOINT_REQUEST, false);

        cut.executeAll(ctx, actions(first, recording("first"), second, recording("second"))).test().assertComplete();

        verify(tracer, never()).startSpanFrom(any());
    }

    private Function<HttpExecutionContext, Completable> recording(String name) {
        return c -> Completable.fromRunnable(() -> executed.add(name));
    }

    private static Map<BasePolicy, Function<HttpExecutionContext, Completable>> actions(
        BasePolicy firstPolicy,
        Function<HttpExecutionContext, Completable> firstAction,
        BasePolicy secondPolicy,
        Function<HttpExecutionContext, Completable> secondAction
    ) {
        var actions = new LinkedHashMap<BasePolicy, Function<HttpExecutionContext, Completable>>();
        actions.put(firstPolicy, firstAction);
        actions.put(secondPolicy, secondAction);
        return actions;
    }

    private static BasePolicy aPolicy(String id) {
        var policy = mock(BasePolicy.class);
        lenient().when(policy.id()).thenReturn(id);
        return policy;
    }
}
