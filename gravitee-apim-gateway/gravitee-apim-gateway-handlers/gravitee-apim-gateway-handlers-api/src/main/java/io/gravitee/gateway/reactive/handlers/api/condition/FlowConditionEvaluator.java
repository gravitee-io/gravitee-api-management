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
