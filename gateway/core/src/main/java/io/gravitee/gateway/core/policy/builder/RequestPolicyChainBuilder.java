package io.gravitee.gateway.core.policy.builder;

import io.gravitee.gateway.api.Policy;
import io.gravitee.gateway.core.policy.PolicyChainBuilder;
import io.gravitee.gateway.core.policy.RequestPolicyChain;
import io.gravitee.gateway.core.policy.rules.AccessControlPolicy;
import io.gravitee.gateway.core.policy.rules.RateLimitPolicy;

import java.util.HashSet;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RequestPolicyChainBuilder implements PolicyChainBuilder<RequestPolicyChain> {

    @Override
    public RequestPolicyChain newPolicyChain() {
        Set<Policy> policies = policies();

        return new RequestPolicyChain(policies);
    }

    private Set<Policy> policies() {
        Set<Policy> policies = new HashSet<>();

        policies.add(new AccessControlPolicy());
        policies.add(new RateLimitPolicy());

        return policies;
    }
}
