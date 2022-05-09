/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactive.handlers.api.flow.resolver;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.RequestExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionEvaluator;
import io.gravitee.gateway.reactive.flow.AbstractFlowResolver;
import io.reactivex.Flowable;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Allows resolving {@link Flow}s defined at plan level of a given {@link Api}.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class ApiPlanFlowResolver extends AbstractFlowResolver {

    private final Api api;

    public ApiPlanFlowResolver(Api api, ConditionEvaluator<Flow> evaluator) {
        super(evaluator);
        this.api = api;
    }

    @Override
    public Flowable<Flow> provideFlows(RequestExecutionContext ctx) {
        if (api.getPlans() == null || api.getPlans().isEmpty()) {
            return Flowable.empty();
        }

        final String planId = ctx.getAttribute(ExecutionContext.ATTR_PLAN);

        return Flowable.fromIterable(
            api
                .getPlans()
                .stream()
                .filter(plan -> Objects.equals(plan.getId(), planId))
                .filter(plan -> Objects.nonNull(plan.getFlows()))
                .flatMap(plan -> plan.getFlows().stream())
                .filter(Flow::isEnabled)
                .collect(Collectors.toList())
        );
    }
}
