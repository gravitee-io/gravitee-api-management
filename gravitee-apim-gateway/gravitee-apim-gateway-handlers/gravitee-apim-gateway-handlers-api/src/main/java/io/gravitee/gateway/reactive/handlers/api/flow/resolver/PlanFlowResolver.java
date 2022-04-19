package io.gravitee.gateway.reactive.handlers.api.flow.resolver;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.handlers.api.condition.ConditionEvaluator;
import io.reactivex.Flowable;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanFlowResolver extends AsbtractFlowResolver {

    private final Api api;

    public PlanFlowResolver(Api api, ConditionEvaluator<Flow> evaluator) {
        super(evaluator);
        this.api = api;
    }

    @Override
    public Flowable<Flow> provideFlows(ExecutionContext<?, ?> ctx) {
        if (api.getPlans() == null || api.getPlans().isEmpty()) {
            return Flowable.empty();
        }

        return Flowable.fromIterable(
            api
                .getPlans()
                .stream()
                .filter(plan -> plan.getId().equals(ctx.getAttribute(ExecutionContext.ATTR_PLAN)))
                .filter(plan -> Objects.nonNull(plan.getFlows()))
                .flatMap(plan -> plan.getFlows().stream())
                .filter(Flow::isEnabled)
                .collect(Collectors.toList())
        );
    }
}
