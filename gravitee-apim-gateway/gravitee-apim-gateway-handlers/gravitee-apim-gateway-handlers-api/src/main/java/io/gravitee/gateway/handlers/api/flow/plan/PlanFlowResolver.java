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
package io.gravitee.gateway.handlers.api.flow.plan;

import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.condition.ConditionEvaluator;
import io.gravitee.gateway.flow.condition.ConditionalFlowResolver;
import io.gravitee.gateway.handlers.api.definition.Api;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanFlowResolver extends ConditionalFlowResolver {

    private final Api api;

    public PlanFlowResolver(Api api, ConditionEvaluator<Flow> evaluator) {
        super(evaluator);
        this.api = api;
    }

    @Override
    public List<Flow> resolve0(ExecutionContext context) {
        Plan plan = api.getPlan(context.request().metrics().getPlan());

        return (plan != null) ? plan.getFlows() : null;
    }

    @Override
    public String place() {
        return "PLAN";
    }
}
