package io.gravitee.gateway.core.policy.builder;

import io.gravitee.gateway.api.Policy;
import io.gravitee.gateway.core.policy.PolicyChainBuilder;
import io.gravitee.gateway.core.policy.ResponsePolicyChain;
import io.gravitee.gateway.core.policy.rules.TransformPolicy;

import java.util.HashSet;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ResponsePolicyChainBuilder implements PolicyChainBuilder<ResponsePolicyChain> {

    @Override
    public ResponsePolicyChain newPolicyChain() {
        Set<Policy> policies = policies();

        return new ResponsePolicyChain(policies);
    }

    private Set<Policy> policies() {
        Set<Policy> policies = new HashSet<>();

        policies.add(new TransformPolicy());

        return policies;
    }
}
