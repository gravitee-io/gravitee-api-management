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

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.v4.flow.AbstractFlowResolver;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Allows resolving {@link Flow}s defined at plan level of a given {@link Api}.
 * For API Product subscriptions, when planId is a product plan (not in api.getPlans()),
 * returns flows from all API plans so ConditionFilter can evaluate conditions
 * including #context.attributes['plan'] for product-plan-specific behavior (PRD #3).
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@SuppressWarnings("common-java:DuplicatedBlocks") // Needed for v4 definition. Will replace the other one at the end.
class ApiPlanFlowResolver extends AbstractFlowResolver {

    private static final String METADATA_REFERENCE_TYPE = "referenceType";
    private static final String REFERENCE_TYPE_API_PRODUCT = "API_PRODUCT";

    private final Api api;

    private final Map<String, Flowable<Flow>> flowsByPlanId = new ConcurrentHashMap<>();

    public ApiPlanFlowResolver(Api api, ConditionFilter<BaseExecutionContext, Flow> filter) {
        super(filter);
        this.api = api;
    }

    @Override
    public Flowable<Flow> provideFlows(BaseExecutionContext ctx) {
        final List<Plan> plans = api.getPlans();
        if (plans == null || plans.isEmpty()) {
            return Flowable.empty();
        }

        final String planId = ctx.getAttribute(ContextAttributes.ATTR_PLAN);
        if (planId == null) {
            return Flowable.empty();
        }

        boolean planMatchesApiPlan = plans.stream().anyMatch(plan -> Objects.equals(plan.getId(), planId));
        if (planMatchesApiPlan) {
            return getFlows(plans, planId);
        }

        if (isApiProductSubscription(ctx)) {
            return getFlowsFromAllPlans(plans);
        }

        return Flowable.empty();
    }

    private boolean isApiProductSubscription(BaseExecutionContext ctx) {
        Subscription subscription = ctx.getInternalAttribute(ATTR_INTERNAL_SUBSCRIPTION);
        if (subscription == null || subscription.getMetadata() == null) {
            return false;
        }
        return REFERENCE_TYPE_API_PRODUCT.equals(subscription.getMetadata().get(METADATA_REFERENCE_TYPE));
    }

    private Flowable<Flow> getFlows(List<Plan> plans, String planId) {
        return this.flowsByPlanId.computeIfAbsent(planId, id ->
            Flowable.fromIterable(
                plans
                    .stream()
                    .filter(plan -> Objects.equals(plan.getId(), id))
                    .filter(plan -> Objects.nonNull(plan.getFlows()))
                    .flatMap(plan -> plan.getFlows().stream())
                    .filter(Flow::isEnabled)
                    .collect(Collectors.toList())
            )
        );
    }

    private Flowable<Flow> getFlowsFromAllPlans(List<Plan> plans) {
        List<Flow> allFlows = plans
            .stream()
            .filter(plan -> Objects.nonNull(plan.getFlows()))
            .flatMap(plan -> plan.getFlows().stream())
            .filter(Flow::isEnabled)
            .collect(Collectors.toList());
        return Flowable.fromIterable(allFlows);
    }
}
