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
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessor;
import io.gravitee.gateway.reactive.v4.flow.AbstractFlowResolver;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Objects;

/**
 * Allows resolving {@link Flow}s defined at API Product plan level for a given API.
 *
 * <p>Flows are resolved only when the request has been authorized through an API Product
 * subscription (i.e. {@link SubscriptionProcessor#ATTR_API_PRODUCT} is set) and match the
 * resolved plan ({@link ContextAttributes#ATTR_PLAN}). The {@link ApiProductRegistry} is read
 * per request so API Product redeployments are picked up without rebuilding the flow chain.
 *
 * @author GraviteeSource Team
 */
class ApiProductPlanFlowResolver extends AbstractFlowResolver {

    private final String apiId;
    private final String environmentId;
    private final ApiProductRegistry apiProductRegistry;

    public ApiProductPlanFlowResolver(
        String apiId,
        String environmentId,
        ApiProductRegistry apiProductRegistry,
        ConditionFilter<BaseExecutionContext, Flow> filter
    ) {
        super(filter);
        this.apiId = apiId;
        this.environmentId = environmentId;
        this.apiProductRegistry = apiProductRegistry;
    }

    @Override
    public Flowable<Flow> provideFlows(BaseExecutionContext ctx) {
        final String apiProductId = ctx.getAttribute(SubscriptionProcessor.ATTR_API_PRODUCT);
        final String planId = ctx.getAttribute(ContextAttributes.ATTR_PLAN);
        if (apiProductId == null || planId == null) {
            return Flowable.empty();
        }

        return Flowable.fromIterable(
            apiProductRegistry
                .getApiProductPlanEntriesForApi(apiId, environmentId)
                .stream()
                .filter(entry -> Objects.equals(entry.apiProductId(), apiProductId))
                .map(ApiProductRegistry.ApiProductPlanEntry::plan)
                .filter(plan -> Objects.equals(plan.getId(), planId))
                .filter(Plan.class::isInstance)
                .map(Plan.class::cast)
                .filter(plan -> Objects.nonNull(plan.getFlows()))
                .flatMap(plan -> plan.getFlows().stream())
                .filter(Flow::isEnabled)
                .toList()
        );
    }
}
