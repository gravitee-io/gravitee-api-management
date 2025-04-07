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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.FlowV4Impl;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.ArrayList;
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
class ApiPlanFlowResolverTest {

    public static final String PLAN_2 = "plan2";
    public static final String PLAN_1 = "plan1";

    @Mock
    private Api api;

    @Mock
    private ConditionFilter<BaseExecutionContext, FlowV4Impl> filter;

    @Mock
    private HttpPlainExecutionContext ctx;

    @Test
    public void shouldProvideApiPlanFlowsOrdered() {
        final Plan plan1 = mock(Plan.class);
        when(plan1.getId()).thenReturn(PLAN_1);
        final List<FlowV4Impl> planFlows1 = new ArrayList<>();
        final FlowV4Impl flow1 = mock(FlowV4Impl.class);
        final FlowV4Impl flow2 = mock(FlowV4Impl.class);

        planFlows1.add(flow1);
        planFlows1.add(flow2);

        final Plan plan2 = mock(Plan.class);
        when(plan2.getId()).thenReturn(PLAN_2);
        final List<FlowV4Impl> planFlows2 = new ArrayList<>();
        final FlowV4Impl flow3 = mock(FlowV4Impl.class);
        final FlowV4Impl flow4 = mock(FlowV4Impl.class);

        planFlows2.add(flow3);
        planFlows2.add(flow4);

        final List<Plan> plans = asList(plan1, plan2);

        when(flow1.isEnabled()).thenReturn(true);
        when(flow2.isEnabled()).thenReturn(true);

        when(api.getPlans()).thenReturn(plans);
        when(plan1.getFlows()).thenReturn(planFlows1);

        when(ctx.getAttribute(ContextAttributes.ATTR_PLAN)).thenReturn(PLAN_1);

        final ApiPlanFlowResolver cut = new ApiPlanFlowResolver(api, filter);
        final TestSubscriber<FlowV4Impl> obs = cut.provideFlows(ctx).test();

        obs.assertResult(flow1, flow2);
    }

    @Test
    public void shouldProvideEnabledApiPlanFlowsOnly() {
        final Plan plan1 = mock(Plan.class);
        when(plan1.getId()).thenReturn(PLAN_1);
        final List<FlowV4Impl> planFlows1 = new ArrayList<>();
        final FlowV4Impl flow1 = mock(FlowV4Impl.class);
        final FlowV4Impl flow2 = mock(FlowV4Impl.class);

        planFlows1.add(flow1);
        planFlows1.add(flow2);

        final Plan plan2 = mock(Plan.class);
        when(plan2.getId()).thenReturn(PLAN_2);
        final List<FlowV4Impl> planFlows2 = new ArrayList<>();
        final FlowV4Impl flow3 = mock(FlowV4Impl.class);
        final FlowV4Impl flow4 = mock(FlowV4Impl.class);

        planFlows2.add(flow3);
        planFlows2.add(flow4);

        final List<Plan> plans = asList(plan1, plan2);

        when(flow3.isEnabled()).thenReturn(false);
        when(flow4.isEnabled()).thenReturn(true);

        when(api.getPlans()).thenReturn(plans);
        when(plan2.getFlows()).thenReturn(planFlows2);

        when(ctx.getAttribute(ContextAttributes.ATTR_PLAN)).thenReturn(PLAN_2);
        final ApiPlanFlowResolver cut = new ApiPlanFlowResolver(api, filter);
        final TestSubscriber<FlowV4Impl> obs = cut.provideFlows(ctx).test();

        obs.assertResult(flow4);
    }

    @Test
    public void shouldProvideEmptyFlowsWhenNullApiPlans() {
        when(api.getPlans()).thenReturn(null);

        final ApiPlanFlowResolver cut = new ApiPlanFlowResolver(api, filter);
        final TestSubscriber<FlowV4Impl> obs = cut.provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    public void shouldProvideEmptyFlowsWhenEmptyApiPlans() {
        when(api.getPlans()).thenReturn(emptyList());

        final ApiPlanFlowResolver cut = new ApiPlanFlowResolver(api, filter);
        final TestSubscriber<FlowV4Impl> obs = cut.provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    public void shouldProvideEmptyFlowsWhenNullApiPlanFlows() {
        when(api.getPlans()).thenReturn(List.of(new Plan()));

        final ApiPlanFlowResolver cut = new ApiPlanFlowResolver(api, filter);
        final TestSubscriber<FlowV4Impl> obs = cut.provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    public void shouldProvideEmptyFlowsWhenEmptyApiPlanFlows() {
        final Plan plan = new Plan();
        plan.setFlows(emptyList());
        when(api.getPlans()).thenReturn(List.of(plan));

        final ApiPlanFlowResolver cut = new ApiPlanFlowResolver(api, filter);
        final TestSubscriber<FlowV4Impl> obs = cut.provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    public void shouldResolve() {
        final Plan plan1 = mock(Plan.class);
        when(plan1.getId()).thenReturn(PLAN_1);
        final List<FlowV4Impl> planFlows1 = new ArrayList<>();
        final FlowV4Impl flow1 = mock(FlowV4Impl.class);
        final FlowV4Impl flow2 = mock(FlowV4Impl.class);

        planFlows1.add(flow1);
        planFlows1.add(flow2);

        final Plan plan2 = mock(Plan.class);
        when(plan2.getId()).thenReturn(PLAN_2);
        final List<FlowV4Impl> planFlows2 = new ArrayList<>();
        final FlowV4Impl flow3 = mock(FlowV4Impl.class);
        final FlowV4Impl flow4 = mock(FlowV4Impl.class);

        planFlows2.add(flow3);
        planFlows2.add(flow4);

        final List<Plan> plans = asList(plan1, plan2);

        when(flow1.isEnabled()).thenReturn(true);
        when(flow2.isEnabled()).thenReturn(true);

        when(api.getPlans()).thenReturn(plans);
        when(plan1.getFlows()).thenReturn(planFlows1);
        when(filter.filter(eq(ctx), any())).thenAnswer(i -> Maybe.just(i.getArgument(1)));

        when(ctx.getAttribute(ContextAttributes.ATTR_PLAN)).thenReturn(PLAN_1);
        final ApiPlanFlowResolver cut = new ApiPlanFlowResolver(api, filter);
        final TestSubscriber<FlowV4Impl> obs = cut.resolve(ctx).test();

        obs.assertResult(flow1, flow2);
    }

    @Test
    public void shouldResolveEmptyFlowsWhenAllFlowFiltered() {
        final Plan plan1 = mock(Plan.class);
        when(plan1.getId()).thenReturn(PLAN_1);
        final List<FlowV4Impl> planFlows1 = new ArrayList<>();
        final FlowV4Impl flow1 = mock(FlowV4Impl.class);
        final FlowV4Impl flow2 = mock(FlowV4Impl.class);

        planFlows1.add(flow1);
        planFlows1.add(flow2);

        final Plan plan2 = mock(Plan.class);
        when(plan2.getId()).thenReturn(PLAN_2);
        final List<FlowV4Impl> planFlows2 = new ArrayList<>();
        final FlowV4Impl flow3 = mock(FlowV4Impl.class);
        final FlowV4Impl flow4 = mock(FlowV4Impl.class);

        planFlows2.add(flow3);
        planFlows2.add(flow4);

        final List<Plan> plans = asList(plan1, plan2);

        when(flow3.isEnabled()).thenReturn(true);
        when(flow4.isEnabled()).thenReturn(true);

        when(api.getPlans()).thenReturn(plans);
        when(plan2.getFlows()).thenReturn(planFlows2);
        when(filter.filter(eq(ctx), any())).thenReturn(Maybe.empty());
        when(ctx.getAttribute(ContextAttributes.ATTR_PLAN)).thenReturn(PLAN_2);

        final ApiPlanFlowResolver cut = new ApiPlanFlowResolver(api, filter);
        final TestSubscriber<FlowV4Impl> obs = cut.resolve(ctx).test();

        obs.assertResult();
    }
}
