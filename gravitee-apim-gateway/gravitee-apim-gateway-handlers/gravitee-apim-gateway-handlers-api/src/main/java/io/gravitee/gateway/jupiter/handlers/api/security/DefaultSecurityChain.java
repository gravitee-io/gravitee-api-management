package io.gravitee.gateway.jupiter.handlers.api.security;

import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.jupiter.handlers.api.security.plan.SecurityPlan;
import io.gravitee.gateway.jupiter.handlers.api.security.plan.SecurityPlanFactory;
import io.gravitee.gateway.jupiter.policy.PolicyManager;
import io.reactivex.Flowable;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultSecurityChain extends AbstractSecurityChain {

    public DefaultSecurityChain(Api api, PolicyManager policyManager) {
        super(Flowable.fromIterable(
                api
                        .getDefinition()
                        .getPlans()
                        .stream()
                        .map(plan -> SecurityPlanFactory.forPlan(plan, policyManager))
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparingInt(SecurityPlan::order))
                        .collect(Collectors.toList())
        ));
    }
}
