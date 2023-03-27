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
package io.gravitee.gateway.jupiter.handlers.api.flow.resolver;

import io.gravitee.definition.model.flow.Flow;
<<<<<<< HEAD:gravitee-apim-gateway/gravitee-apim-gateway-handlers/gravitee-apim-gateway-handlers-api/src/main/java/io/gravitee/gateway/jupiter/handlers/api/flow/resolver/ApiPlanFlowResolver.java
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.core.condition.ConditionEvaluator;
import io.gravitee.gateway.jupiter.flow.AbstractFlowResolver;
import io.reactivex.Flowable;
=======
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.GenericExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.flow.AbstractFlowResolver;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
>>>>>>> d5a816621b (fix(gateway): fix pathParameter for jupiter/v4):gravitee-apim-gateway/gravitee-apim-gateway-handlers/gravitee-apim-gateway-handlers-api/src/main/java/io/gravitee/gateway/reactive/handlers/api/flow/resolver/ApiPlanFlowResolver.java
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

<<<<<<< HEAD:gravitee-apim-gateway/gravitee-apim-gateway-handlers/gravitee-apim-gateway-handlers-api/src/main/java/io/gravitee/gateway/jupiter/handlers/api/flow/resolver/ApiPlanFlowResolver.java
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
=======
        final String planId = ctx.getAttribute(ContextAttributes.ATTR_PLAN);
        List<Flow> flows = api
            .getPlans()
            .stream()
            .filter(plan -> Objects.equals(plan.getId(), planId))
            .filter(plan -> Objects.nonNull(plan.getFlows()))
            .flatMap(plan -> plan.getFlows().stream())
            .filter(Flow::isEnabled)
            .collect(Collectors.toList());
        addContextRequestPathParameters(ctx, flows);
        return Flowable.fromIterable(flows);
>>>>>>> d5a816621b (fix(gateway): fix pathParameter for jupiter/v4):gravitee-apim-gateway/gravitee-apim-gateway-handlers/gravitee-apim-gateway-handlers-api/src/main/java/io/gravitee/gateway/reactive/handlers/api/flow/resolver/ApiPlanFlowResolver.java
    }
}
