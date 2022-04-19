package io.gravitee.gateway.reactive.handlers.api.flow.resolver;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.handlers.api.condition.ConditionEvaluator;
import io.reactivex.Flowable;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AsbtractFlowResolver implements FlowResolver {

    private final ConditionEvaluator<Flow> evaluator;

    protected AsbtractFlowResolver(ConditionEvaluator<Flow> evaluator) {
        this.evaluator = evaluator;
    }

    public Flowable<Flow> resolve(ExecutionContext<?, ?> ctx) {
        return evaluator.filter(ctx, provideFlows(ctx));
    }
}
