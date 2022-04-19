package io.gravitee.gateway.reactive.handlers.api.flow.resolver;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.handlers.api.condition.ConditionEvaluator;
import io.reactivex.Flowable;

import java.util.stream.Collectors;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiFlowResolver extends AsbtractFlowResolver {

    private final Flowable<Flow> flows;

    public ApiFlowResolver(Api api, ConditionEvaluator<Flow> evaluator) {
        super(evaluator);
        // Api flows can be determined once and then reused.
        this.flows = getFlows(api);
    }

    @Override
    public Flowable<Flow> provideFlows(ExecutionContext<?, ?> ctx) {
        return this.flows;
    }

    private Flowable<Flow> getFlows(Api api) {
        if (api.getFlows() == null || api.getFlows().isEmpty()) {
            return Flowable.empty();
        }

        return Flowable.fromIterable(api.getFlows().stream().filter(Flow::isEnabled).collect(Collectors.toList())).cache();
    }
}
