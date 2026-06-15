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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessor;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ApiProductPlanFlowResolverTest {

    private static final String API_ID = "api-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String PRODUCT_1 = "product1";
    private static final String PRODUCT_2 = "product2";
    private static final String PLAN_1 = "plan1";
    private static final String PLAN_2 = "plan2";

    @Mock
    private ApiProductRegistry apiProductRegistry;

    @Mock
    private ConditionFilter<BaseExecutionContext, Flow> filter;

    @Mock
    private HttpPlainExecutionContext ctx;

    private ApiProductPlanFlowResolver newResolver() {
        return new ApiProductPlanFlowResolver(API_ID, ENVIRONMENT_ID, apiProductRegistry, filter);
    }

    private Plan planWithFlows(String planId, List<Flow> flows) {
        final Plan plan = mock(Plan.class);
        when(plan.getId()).thenReturn(planId);
        when(plan.getFlows()).thenReturn(flows);
        return plan;
    }

    @Test
    void shouldProvideMatchingProductPlanFlows() {
        final Flow flow1 = mock(Flow.class);
        final Flow flow2 = mock(Flow.class);
        when(flow1.isEnabled()).thenReturn(true);
        when(flow2.isEnabled()).thenReturn(true);

        final Plan plan1 = planWithFlows(PLAN_1, List.of(flow1, flow2));

        when(apiProductRegistry.getApiProductPlanEntriesForApi(API_ID, ENVIRONMENT_ID)).thenReturn(
            List.of(new ApiProductRegistry.ApiProductPlanEntry(PRODUCT_1, plan1))
        );
        when(ctx.getAttribute(SubscriptionProcessor.ATTR_API_PRODUCT)).thenReturn(PRODUCT_1);
        when(ctx.getAttribute(ContextAttributes.ATTR_PLAN)).thenReturn(PLAN_1);

        final TestSubscriber<Flow> obs = newResolver().provideFlows(ctx).test();

        obs.assertResult(flow1, flow2);
    }

    @Test
    void shouldProvideEnabledFlowsOnly() {
        final Flow flow1 = mock(Flow.class);
        final Flow flow2 = mock(Flow.class);
        when(flow1.isEnabled()).thenReturn(false);
        when(flow2.isEnabled()).thenReturn(true);

        final Plan plan1 = planWithFlows(PLAN_1, List.of(flow1, flow2));

        when(apiProductRegistry.getApiProductPlanEntriesForApi(API_ID, ENVIRONMENT_ID)).thenReturn(
            List.of(new ApiProductRegistry.ApiProductPlanEntry(PRODUCT_1, plan1))
        );
        when(ctx.getAttribute(SubscriptionProcessor.ATTR_API_PRODUCT)).thenReturn(PRODUCT_1);
        when(ctx.getAttribute(ContextAttributes.ATTR_PLAN)).thenReturn(PLAN_1);

        final TestSubscriber<Flow> obs = newResolver().provideFlows(ctx).test();

        obs.assertResult(flow2);
    }

    @Test
    void shouldProvideEmptyFlowsWhenNoApiProductAttribute() {
        when(ctx.getAttribute(SubscriptionProcessor.ATTR_API_PRODUCT)).thenReturn(null);

        final TestSubscriber<Flow> obs = newResolver().provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    void shouldProvideEmptyFlowsWhenNoPlanAttribute() {
        when(ctx.getAttribute(SubscriptionProcessor.ATTR_API_PRODUCT)).thenReturn(PRODUCT_1);
        when(ctx.getAttribute(ContextAttributes.ATTR_PLAN)).thenReturn(null);

        final TestSubscriber<Flow> obs = newResolver().provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    void shouldProvideEmptyFlowsWhenPlanIdDoesNotMatch() {
        final Plan plan1 = mock(Plan.class);
        when(plan1.getId()).thenReturn(PLAN_1);

        when(apiProductRegistry.getApiProductPlanEntriesForApi(API_ID, ENVIRONMENT_ID)).thenReturn(
            List.of(new ApiProductRegistry.ApiProductPlanEntry(PRODUCT_1, plan1))
        );
        when(ctx.getAttribute(SubscriptionProcessor.ATTR_API_PRODUCT)).thenReturn(PRODUCT_1);
        when(ctx.getAttribute(ContextAttributes.ATTR_PLAN)).thenReturn(PLAN_2);

        final TestSubscriber<Flow> obs = newResolver().provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    void shouldProvideEmptyFlowsWhenProductIdDoesNotMatch() {
        final Plan plan1 = mock(Plan.class);

        when(apiProductRegistry.getApiProductPlanEntriesForApi(API_ID, ENVIRONMENT_ID)).thenReturn(
            List.of(new ApiProductRegistry.ApiProductPlanEntry(PRODUCT_1, plan1))
        );
        when(ctx.getAttribute(SubscriptionProcessor.ATTR_API_PRODUCT)).thenReturn(PRODUCT_2);
        when(ctx.getAttribute(ContextAttributes.ATTR_PLAN)).thenReturn(PLAN_1);

        final TestSubscriber<Flow> obs = newResolver().provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    void shouldProvideEmptyFlowsWhenPlanHasNullFlows() {
        final Plan plan1 = planWithFlows(PLAN_1, null);

        when(apiProductRegistry.getApiProductPlanEntriesForApi(API_ID, ENVIRONMENT_ID)).thenReturn(
            List.of(new ApiProductRegistry.ApiProductPlanEntry(PRODUCT_1, plan1))
        );
        when(ctx.getAttribute(SubscriptionProcessor.ATTR_API_PRODUCT)).thenReturn(PRODUCT_1);
        when(ctx.getAttribute(ContextAttributes.ATTR_PLAN)).thenReturn(PLAN_1);

        final TestSubscriber<Flow> obs = newResolver().provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    void shouldReadRegistryOnEachResolution() {
        final Flow flow1 = mock(Flow.class);
        when(flow1.isEnabled()).thenReturn(true);
        final Plan plan1 = planWithFlows(PLAN_1, List.of(flow1));

        when(apiProductRegistry.getApiProductPlanEntriesForApi(API_ID, ENVIRONMENT_ID)).thenReturn(
            List.of(new ApiProductRegistry.ApiProductPlanEntry(PRODUCT_1, plan1))
        );
        when(ctx.getAttribute(SubscriptionProcessor.ATTR_API_PRODUCT)).thenReturn(PRODUCT_1);
        when(ctx.getAttribute(ContextAttributes.ATTR_PLAN)).thenReturn(PLAN_1);

        final ApiProductPlanFlowResolver cut = newResolver();
        cut.provideFlows(ctx).test().assertResult(flow1);
        cut.provideFlows(ctx).test().assertResult(flow1);

        verify(apiProductRegistry, times(2)).getApiProductPlanEntriesForApi(API_ID, ENVIRONMENT_ID);
    }

    @Test
    void shouldResolveWithConditionFilter() {
        final Flow flow1 = mock(Flow.class);
        when(flow1.isEnabled()).thenReturn(true);
        final Plan plan1 = planWithFlows(PLAN_1, List.of(flow1));

        when(apiProductRegistry.getApiProductPlanEntriesForApi(API_ID, ENVIRONMENT_ID)).thenReturn(
            List.of(new ApiProductRegistry.ApiProductPlanEntry(PRODUCT_1, plan1))
        );
        when(ctx.getAttribute(SubscriptionProcessor.ATTR_API_PRODUCT)).thenReturn(PRODUCT_1);
        when(ctx.getAttribute(ContextAttributes.ATTR_PLAN)).thenReturn(PLAN_1);
        when(filter.filter(eq(ctx), any())).thenAnswer(i -> Maybe.just(i.getArgument(1)));

        final TestSubscriber<Flow> obs = newResolver().resolve(ctx).test();

        obs.assertResult(flow1);
    }
}
