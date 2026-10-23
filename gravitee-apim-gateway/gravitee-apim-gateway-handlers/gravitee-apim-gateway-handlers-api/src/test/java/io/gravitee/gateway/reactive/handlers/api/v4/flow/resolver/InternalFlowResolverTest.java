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
package io.gravitee.gateway.reactive.handlers.api.v4.flow.resolver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.reactivex.rxjava3.core.Maybe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalFlowResolverTest {

    @Mock
    private Api api;

    @Mock
    private ConditionFilter<BaseExecutionContext, Flow> filter;

    @Mock
    private HttpPlainExecutionContext ctx;

    @Test
    void should_provide_the_internal_flow() {
        final Flow internalFlow = mock(Flow.class);
        when(internalFlow.isEnabled()).thenReturn(true);
        when(api.getInternalFlow()).thenReturn(internalFlow);

        final InternalFlowResolver cut = new InternalFlowResolver(api, filter);

        cut.provideFlows(ctx).test().assertResult(internalFlow);
    }

    @Test
    void should_provide_no_flow_when_the_internal_flow_is_disabled() {
        final Flow internalFlow = mock(Flow.class);
        when(internalFlow.isEnabled()).thenReturn(false);
        when(api.getInternalFlow()).thenReturn(internalFlow);

        final InternalFlowResolver cut = new InternalFlowResolver(api, filter);

        cut.provideFlows(ctx).test().assertResult();
    }

    @Test
    void should_provide_no_flow_when_there_is_no_internal_flow() {
        when(api.getInternalFlow()).thenReturn(null);

        final InternalFlowResolver cut = new InternalFlowResolver(api, filter);

        cut.provideFlows(ctx).test().assertResult();
    }

    @Test
    void should_resolve_the_internal_flow_when_it_passes_the_filter() {
        final Flow internalFlow = mock(Flow.class);
        when(internalFlow.isEnabled()).thenReturn(true);
        when(api.getInternalFlow()).thenReturn(internalFlow);
        when(filter.filter(eq(ctx), any())).thenAnswer(i -> Maybe.just(i.getArgument(1)));

        final InternalFlowResolver cut = new InternalFlowResolver(api, filter);

        cut.resolve(ctx).test().assertResult(internalFlow);
    }

    @Test
    void should_resolve_no_flow_when_the_internal_flow_is_filtered_out() {
        final Flow internalFlow = mock(Flow.class);
        when(internalFlow.isEnabled()).thenReturn(true);
        when(api.getInternalFlow()).thenReturn(internalFlow);
        when(filter.filter(eq(ctx), any())).thenReturn(Maybe.empty());

        final InternalFlowResolver cut = new InternalFlowResolver(api, filter);

        cut.resolve(ctx).test().assertResult();
    }
}
