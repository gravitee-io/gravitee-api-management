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
package io.gravitee.gateway.reactive.handlers.api.condition;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.core.condition.CompositeConditionEvaluator;
import io.gravitee.gateway.flow.condition.evaluation.ExpressionLanguageFlowConditionEvaluator;
import io.gravitee.gateway.flow.condition.evaluation.HttpMethodConditionEvaluator;
import io.gravitee.gateway.flow.condition.evaluation.PathBasedConditionEvaluator;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.handlers.api.adapter.context.ExecutionContextAdapter;
import io.reactivex.Flowable;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowConditionEvaluator implements ConditionEvaluator<Flow> {

    private final io.gravitee.gateway.core.condition.ConditionEvaluator<Flow> conditionEvaluator = new CompositeConditionEvaluator(
        new HttpMethodConditionEvaluator(),
        new PathBasedConditionEvaluator(),
        new ExpressionLanguageFlowConditionEvaluator()
    );

    @Override
    public Flowable<Flow> filter(ExecutionContext<?, ?> executionContext, Flowable<Flow> flows) {
        return flows.filter(flow -> conditionEvaluator.evaluate(ExecutionContextAdapter.create(executionContext), flow));
    }
}
