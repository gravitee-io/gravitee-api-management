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
package io.gravitee.gateway.jupiter.handlers.api.v4.flow.resolver;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.gateway.jupiter.api.context.ContextAttributes;
import io.gravitee.gateway.jupiter.api.context.GenericExecutionContext;
import io.gravitee.gateway.jupiter.core.condition.ConditionFilter;
import io.gravitee.gateway.jupiter.v4.flow.AbstractFlowResolver;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Allows resolving {@link Flow}s defined at plan level of a given {@link Api}.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@SuppressWarnings("common-java:DuplicatedBlocks") // Needed for v4 definition. Will replace the other one at the end.
class ApiPlanFlowResolver extends AbstractFlowResolver {

    private final Api api;

    public ApiPlanFlowResolver(Api api, ConditionFilter<Flow> filter) {
        super(filter);
        this.api = api;
    }

    @Override
    public Flowable<Flow> provideFlows(GenericExecutionContext ctx) {
        final List<Plan> plans = api.getPlans();
        if (plans == null || plans.isEmpty()) {
            return Flowable.empty();
        }

        final String planId = ctx.getAttribute(ContextAttributes.ATTR_PLAN);

        return Flowable.fromIterable(
            plans
                .stream()
                .filter(plan -> Objects.equals(plan.getId(), planId))
                .filter(plan -> Objects.nonNull(plan.getFlows()))
                .flatMap(plan -> plan.getFlows().stream())
                .filter(Flow::isEnabled)
                .collect(Collectors.toList())
        );
    }
}
