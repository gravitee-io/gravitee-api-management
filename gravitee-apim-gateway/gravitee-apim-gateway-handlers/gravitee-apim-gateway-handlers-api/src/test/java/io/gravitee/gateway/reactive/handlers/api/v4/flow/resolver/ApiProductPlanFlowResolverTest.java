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

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessor;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ApiProductPlanFlowResolverTest {

    private static final String PLAN_1 = "plan1";
    private static final String PLAN_2 = "plan2";

    private final Flow plan1Flow1 = Flow.builder().name("plan1-flow1").enabled(true).build();
    private final Flow plan1Flow2 = Flow.builder().name("plan1-flow2").enabled(true).build();
    private final Flow plan2Flow = Flow.builder().name("plan2-flow").enabled(true).build();

    @Mock
    private ConditionFilter<BaseExecutionContext, Flow> filter;

    @Mock
    private HttpPlainExecutionContext ctx;

    private final ApiProductPlanFlowResolver cut = new ApiProductPlanFlowResolver(
        Map.of(PLAN_1, List.of(plan1Flow1, plan1Flow2), PLAN_2, List.of(plan2Flow)),
        filter
    );

    @Test
    void should_provide_the_flows_of_the_plan_of_the_internal_subscription() {
        givenInternalSubscriptionOnPlan(PLAN_1);

        cut.provideFlows(ctx).test().assertResult(plan1Flow1, plan1Flow2);
    }

    @Test
    void should_provide_enabled_flows_only() {
        Flow disabled = Flow.builder().name("disabled").enabled(false).build();
        ApiProductPlanFlowResolver resolver = new ApiProductPlanFlowResolver(Map.of(PLAN_1, List.of(plan1Flow1, disabled)), filter);
        givenInternalSubscriptionOnPlan(PLAN_1);

        resolver.provideFlows(ctx).test().assertResult(plan1Flow1);
    }

    @Test
    void should_provide_no_flow_without_internal_subscription() {
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION)).thenReturn(null);

        cut.provideFlows(ctx).test().assertResult();
    }

    @Test
    void should_provide_no_flow_when_the_subscription_plan_is_not_a_plan_of_the_product() {
        givenInternalSubscriptionOnPlan("api-plan");

        cut.provideFlows(ctx).test().assertResult();
    }

    @Test
    void should_ignore_public_api_product_and_plan_attributes_set_by_a_policy() {
        givenInternalSubscriptionOnPlan(PLAN_1);
        lenient().when(ctx.getAttribute(SubscriptionProcessor.ATTR_API_PRODUCT)).thenReturn("another-product");
        lenient().when(ctx.getAttribute(ContextAttributes.ATTR_PLAN)).thenReturn(PLAN_2);

        cut.provideFlows(ctx).test().assertResult(plan1Flow1, plan1Flow2);
    }

    private void givenInternalSubscriptionOnPlan(String planId) {
        Subscription subscription = new Subscription();
        subscription.setPlan(planId);
        subscription.setApiProductId("product-1");
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION)).thenReturn(subscription);
    }
}
