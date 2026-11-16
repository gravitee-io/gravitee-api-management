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

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.v4.flow.AbstractFlowResolver;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.Map;

/**
 * Resolves the flows of the product plan that accepted the request, among the plans of one deployed version of a
 * product. The plan is read from the internal subscription, never from the public {@code plan} or {@code apiProduct}
 * attributes, which a policy can rewrite.
 *
 * @author GraviteeSource Team
 */
class ApiProductPlanFlowResolver extends AbstractFlowResolver {

    private final Map<String, List<Flow>> flowsByPlanId;

    ApiProductPlanFlowResolver(Map<String, List<Flow>> flowsByPlanId, ConditionFilter<BaseExecutionContext, Flow> filter) {
        super(filter);
        this.flowsByPlanId = flowsByPlanId;
    }

    @Override
    public Flowable<Flow> provideFlows(BaseExecutionContext ctx) {
        final Subscription subscription = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION);
        if (subscription == null || subscription.getPlan() == null) {
            return Flowable.empty();
        }
        final List<Flow> flows = flowsByPlanId.getOrDefault(subscription.getPlan(), List.of());
        return Flowable.fromIterable(flows).filter(Flow::isEnabled);
    }
}
