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

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry.ApiProductPlanEntry;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessor;
import io.gravitee.gateway.reactive.v4.flow.AbstractFlowResolver;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Resolves {@link Flow}s defined on the product plan selected for the current request. Product plans
 * are not part of the {@link Api} definition; they are held in the {@link ApiProductRegistry} and may
 * change on a product redeploy, so flows are read fresh from the registry rather than memoized.
 *
 * @author GraviteeSource Team
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
class ApiProductPlanFlowResolver extends AbstractFlowResolver {

    private final Api api;
    private final String environmentId;
    private final ApiProductRegistry apiProductRegistry;

    ApiProductPlanFlowResolver(
        Api api,
        String environmentId,
        ApiProductRegistry apiProductRegistry,
        ConditionFilter<BaseExecutionContext, Flow> filter
    ) {
        super(filter);
        this.api = api;
        this.environmentId = environmentId;
        this.apiProductRegistry = apiProductRegistry;
    }

    @Override
    public Flowable<Flow> provideFlows(BaseExecutionContext ctx) {
        if (apiProductRegistry == null || environmentId == null) {
            return Flowable.empty();
        }

        final String apiProductId = ctx.getAttribute(SubscriptionProcessor.ATTR_API_PRODUCT);
        if (apiProductId == null) {
            return Flowable.empty();
        }

        final String planId = ctx.getAttribute(ContextAttributes.ATTR_PLAN);
        if (planId == null) {
            return Flowable.empty();
        }

        return Flowable.fromIterable(resolveFlows(planId));
    }

    private List<Flow> resolveFlows(String planId) {
        return apiProductRegistry
            .getApiProductPlanEntriesForApi(api.getId(), environmentId)
            .stream()
            .map(ApiProductPlanEntry::plan)
            .filter(plan -> Objects.equals(plan.getId(), planId))
            .filter(Plan.class::isInstance)
            .map(Plan.class::cast)
            .filter(plan -> Objects.nonNull(plan.getFlows()))
            .flatMap(plan -> plan.getFlows().stream())
            .filter(Flow::isEnabled)
            .collect(Collectors.toList());
    }
}
