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
package io.gravitee.gateway.reactive.handlers.api.flow.resolver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.flow.FlowV2Impl;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ApiFlowResolverTest {

    @Mock
    private Api api;

    @Mock
    private ConditionFilter<HttpBaseExecutionContext, FlowV2Impl> filter;

    @Mock
    private HttpPlainExecutionContext ctx;

    @Test
    public void shouldProvideApiFlows() {
        final List<FlowV2Impl> flows = new ArrayList<>();
        final FlowV2Impl flow1 = mock(FlowV2Impl.class);
        final FlowV2Impl flow2 = mock(FlowV2Impl.class);

        flows.add(flow1);
        flows.add(flow2);

        when(flow1.isEnabled()).thenReturn(true);
        when(flow2.isEnabled()).thenReturn(true);
        when(api.getFlows()).thenReturn(flows);

        final ApiFlowResolver cut = new ApiFlowResolver(api, filter);
        final TestSubscriber<FlowV2Impl> obs = cut.provideFlows(ctx).test();

        obs.assertResult(flow1, flow2);
    }

    @Test
    public void shouldProvideEnabledApiFlowsOnly() {
        final List<FlowV2Impl> flows = new ArrayList<>();
        final FlowV2Impl flow1 = mock(FlowV2Impl.class);
        final FlowV2Impl flow2 = mock(FlowV2Impl.class);

        flows.add(flow1);
        flows.add(flow2);

        when(flow1.isEnabled()).thenReturn(false);
        when(flow2.isEnabled()).thenReturn(true);
        when(api.getFlows()).thenReturn(flows);

        final ApiFlowResolver cut = new ApiFlowResolver(api, filter);
        final TestSubscriber<FlowV2Impl> obs = cut.provideFlows(ctx).test();

        obs.assertResult(flow2);
    }

    @Test
    public void shouldProvideEmptyFlowsWhenNullApiFlows() {
        when(api.getFlows()).thenReturn(null);

        final ApiFlowResolver cut = new ApiFlowResolver(api, filter);
        final TestSubscriber<FlowV2Impl> obs = cut.provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    public void shouldProvideEmptyFlowsWhenEmptyApiFlows() {
        when(api.getFlows()).thenReturn(Collections.emptyList());

        final ApiFlowResolver cut = new ApiFlowResolver(api, filter);
        final TestSubscriber<FlowV2Impl> obs = cut.provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    public void shouldResolve() {
        final List<FlowV2Impl> flows = new ArrayList<>();
        final FlowV2Impl flow1 = mock(FlowV2Impl.class);
        final FlowV2Impl flow2 = mock(FlowV2Impl.class);

        flows.add(flow1);
        flows.add(flow2);

        when(flow1.isEnabled()).thenReturn(true);
        when(flow2.isEnabled()).thenReturn(true);
        when(api.getFlows()).thenReturn(flows);
        when(filter.filter(eq(ctx), any())).thenAnswer(i -> Maybe.just(i.getArgument(1)));

        final ApiFlowResolver cut = new ApiFlowResolver(api, filter);
        final TestSubscriber<FlowV2Impl> obs = cut.resolve(ctx).test();

        obs.assertResult(flow1, flow2);
    }

    @Test
    public void shouldResolveEmptyFlowsWhenAllFlowFiltered() {
        final List<FlowV2Impl> flows = new ArrayList<>();
        final FlowV2Impl flow1 = mock(FlowV2Impl.class);
        final FlowV2Impl flow2 = mock(FlowV2Impl.class);

        flows.add(flow1);
        flows.add(flow2);

        when(flow1.isEnabled()).thenReturn(true);
        when(flow2.isEnabled()).thenReturn(true);
        when(api.getFlows()).thenReturn(flows);
        when(filter.filter(eq(ctx), any())).thenReturn(Maybe.empty());

        final ApiFlowResolver cut = new ApiFlowResolver(api, filter);
        final TestSubscriber<FlowV2Impl> obs = cut.resolve(ctx).test();

        obs.assertResult();
    }
}
