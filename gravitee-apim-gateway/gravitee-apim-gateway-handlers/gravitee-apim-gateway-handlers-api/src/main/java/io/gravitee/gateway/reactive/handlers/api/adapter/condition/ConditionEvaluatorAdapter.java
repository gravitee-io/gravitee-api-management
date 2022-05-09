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
package io.gravitee.gateway.reactive.handlers.api.adapter.condition;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.core.condition.CompositeConditionEvaluator;
import io.gravitee.gateway.flow.condition.evaluation.ExpressionLanguageFlowConditionEvaluator;
import io.gravitee.gateway.flow.condition.evaluation.HttpMethodConditionEvaluator;
import io.gravitee.gateway.flow.condition.evaluation.PathBasedConditionEvaluator;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.RequestExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionEvaluator;
import io.gravitee.gateway.reactive.policy.adapter.context.ExecutionContextAdapter;
import io.reactivex.Flowable;

/**
 * Adapts an existing v3 {@link io.gravitee.gateway.core.condition.ConditionEvaluator} to make it working as a regular {@link ConditionEvaluator}.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConditionEvaluatorAdapter<T> implements ConditionEvaluator<T> {

    /**
     * Default evaluator that can be reused to avoid unnecessary instantiations.
     * For now, this evaluator is an adapter that reuses the existing composite evaluator.
     * It is composed with:
     * <ul>
     *     <li>{@link HttpMethodConditionEvaluator}: used to filter the flows  by expected http method</li>
     *     <li>{@link PathBasedConditionEvaluator}: filter the flows based on the request path</li>
     *     <li>{@link ExpressionLanguageFlowConditionEvaluator}: filter the flows to keep only those matching the flow condition</li>
     * </ul>
     *
     * <b>WARN</b>: this default evaluator embed the {@link ExpressionLanguageFlowConditionEvaluator}.
     * This is subject to change when it will be possible to execute the condition in a reactive way (to support condition on body)
     */
    public static final ConditionEvaluator<Flow> DEFAULT_FLOW_EVALUATOR = new ConditionEvaluatorAdapter<>(
        new CompositeConditionEvaluator<>(
            new HttpMethodConditionEvaluator(),
            new PathBasedConditionEvaluator(),
            new ExpressionLanguageFlowConditionEvaluator()
        )
    );

    private final io.gravitee.gateway.core.condition.ConditionEvaluator<T> legacyEvaluator;

    public ConditionEvaluatorAdapter(io.gravitee.gateway.core.condition.ConditionEvaluator<T> legacyEvaluator) {
        this.legacyEvaluator = legacyEvaluator;
    }

    @Override
    public Flowable<T> filter(RequestExecutionContext ctx, Flowable<T> flows) {
        return flows.filter(flow -> legacyEvaluator.evaluate(ExecutionContextAdapter.create(ctx), flow));
    }
}
