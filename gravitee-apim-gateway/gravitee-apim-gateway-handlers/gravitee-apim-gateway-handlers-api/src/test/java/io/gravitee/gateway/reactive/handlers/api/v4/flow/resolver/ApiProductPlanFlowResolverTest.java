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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry.ApiProductPlanEntry;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessor;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.ArrayList;
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

    private static final String API_ID = "api-1";
    private static final String ENV_ID = "env-1";
    private static final String PRODUCT_ID = "product-1";
    private static final String PLAN_1 = "plan1";
    private static final String PLAN_2 = "plan2";

    @Mock
    private Api api;

    @Mock
    private ApiProductRegistry apiProductRegistry;

    @Mock
    private ConditionFilter<BaseExecutionContext, Flow> filter;

    @Mock
    private HttpPlainExecutionContext ctx;

    @Test
    void shouldProvideProductPlanFlowsForSelectedPlan() {
        final Plan plan1 = mock(Plan.class);
        when(plan1.getId()).thenReturn(PLAN_1);
        final Flow flow1 = mock(Flow.class);
        final Flow flow2 = mock(Flow.class);
        when(plan1.getFlows()).thenReturn(asList(flow1, flow2));
        when(flow1.isEnabled()).thenReturn(true);
        when(flow2.isEnabled()).thenReturn(true);

        final Plan plan2 = mock(Plan.class);
        when(plan2.getId()).thenReturn(PLAN_2);

        when(api.getId()).thenReturn(API_ID);
        when(apiProductRegistry.getApiProductPlanEntriesForApi(API_ID, ENV_ID)).thenReturn(
            List.of(new ApiProductPlanEntry(PRODUCT_ID, plan1), new ApiProductPlanEntry(PRODUCT_ID, plan2))
        );
        when(ctx.getAttribute(SubscriptionProcessor.ATTR_API_PRODUCT)).thenReturn(PRODUCT_ID);
        when(ctx.getAttribute(ContextAttributes.ATTR_PLAN)).thenReturn(PLAN_1);

        final ApiProductPlanFlowResolver cut = new ApiProductPlanFlowResolver(api, ENV_ID, apiProductRegistry, filter);
        final TestSubscriber<Flow> obs = cut.provideFlows(ctx).test();

        obs.assertResult(flow1, flow2);
    }

    @Test
    void shouldProvideEnabledProductPlanFlowsOnly() {
        final Plan plan = mock(Plan.class);
        when(plan.getId()).thenReturn(PLAN_1);
        final Flow enabledFlow = mock(Flow.class);
        final Flow disabledFlow = mock(Flow.class);
        when(plan.getFlows()).thenReturn(asList(enabledFlow, disabledFlow));
        when(enabledFlow.isEnabled()).thenReturn(true);
        when(disabledFlow.isEnabled()).thenReturn(false);

        when(api.getId()).thenReturn(API_ID);
        when(apiProductRegistry.getApiProductPlanEntriesForApi(API_ID, ENV_ID)).thenReturn(
            List.of(new ApiProductPlanEntry(PRODUCT_ID, plan))
        );
        when(ctx.getAttribute(SubscriptionProcessor.ATTR_API_PRODUCT)).thenReturn(PRODUCT_ID);
        when(ctx.getAttribute(ContextAttributes.ATTR_PLAN)).thenReturn(PLAN_1);

        final ApiProductPlanFlowResolver cut = new ApiProductPlanFlowResolver(api, ENV_ID, apiProductRegistry, filter);
        final TestSubscriber<Flow> obs = cut.provideFlows(ctx).test();

        obs.assertResult(enabledFlow);
    }

    @Test
    void shouldProvideEmptyFlowsWhenRegistryIsNull() {
        final ApiProductPlanFlowResolver cut = new ApiProductPlanFlowResolver(api, ENV_ID, null, filter);
        final TestSubscriber<Flow> obs = cut.provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    void shouldProvideEmptyFlowsWhenEnvironmentIdIsNull() {
        final ApiProductPlanFlowResolver cut = new ApiProductPlanFlowResolver(api, null, apiProductRegistry, filter);
        final TestSubscriber<Flow> obs = cut.provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    void shouldProvideEmptyFlowsWhenApiProductIsMissingFromContext() {
        when(ctx.getAttribute(SubscriptionProcessor.ATTR_API_PRODUCT)).thenReturn(null);

        final ApiProductPlanFlowResolver cut = new ApiProductPlanFlowResolver(api, ENV_ID, apiProductRegistry, filter);
        final TestSubscriber<Flow> obs = cut.provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    void shouldProvideEmptyFlowsWhenPlanIsMissingFromContext() {
        when(ctx.getAttribute(SubscriptionProcessor.ATTR_API_PRODUCT)).thenReturn(PRODUCT_ID);
        when(ctx.getAttribute(ContextAttributes.ATTR_PLAN)).thenReturn(null);

        final ApiProductPlanFlowResolver cut = new ApiProductPlanFlowResolver(api, ENV_ID, apiProductRegistry, filter);
        final TestSubscriber<Flow> obs = cut.provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    void shouldProvideEmptyFlowsWhenNoMatchingPlanInRegistry() {
        final Plan plan = new Plan();
        plan.setId(PLAN_2);
        plan.setFlows(new ArrayList<>());

        when(api.getId()).thenReturn(API_ID);
        when(apiProductRegistry.getApiProductPlanEntriesForApi(API_ID, ENV_ID)).thenReturn(
            List.of(new ApiProductPlanEntry(PRODUCT_ID, plan))
        );
        when(ctx.getAttribute(SubscriptionProcessor.ATTR_API_PRODUCT)).thenReturn(PRODUCT_ID);
        when(ctx.getAttribute(ContextAttributes.ATTR_PLAN)).thenReturn(PLAN_1);

        final ApiProductPlanFlowResolver cut = new ApiProductPlanFlowResolver(api, ENV_ID, apiProductRegistry, filter);
        final TestSubscriber<Flow> obs = cut.provideFlows(ctx).test();

        obs.assertResult();
    }
}
