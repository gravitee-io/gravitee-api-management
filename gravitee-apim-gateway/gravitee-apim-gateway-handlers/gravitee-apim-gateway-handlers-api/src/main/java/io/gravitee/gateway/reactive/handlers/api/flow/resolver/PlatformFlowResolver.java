package io.gravitee.gateway.reactive.handlers.api.flow.resolver;

import io.gravitee.definition.model.Organization;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.handlers.api.condition.ConditionEvaluator;
import io.reactivex.Flowable;

import java.util.stream.Collectors;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlatformFlowResolver extends AsbtractFlowResolver {

    private final Flowable<Flow> flows;

    public PlatformFlowResolver(Organization organization, ConditionEvaluator<Flow> evaluator) {
        super(evaluator);
        this.flows = getFlows(organization);
    }

    @Override
    public Flowable<Flow> provideFlows(ExecutionContext<?, ?> ctx) {
        return this.flows;
    }

    private Flowable<Flow> getFlows(Organization organization) {
        if (organization == null || organization.getFlows() == null || organization.getFlows().isEmpty()) {
            return Flowable.empty();
        }

        return Flowable.fromIterable(organization.getFlows().stream().filter(Flow::isEnabled).collect(Collectors.toList())).cache();
    }
}
