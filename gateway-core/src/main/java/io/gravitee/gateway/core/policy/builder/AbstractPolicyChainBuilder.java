package io.gravitee.gateway.core.policy.builder;

import io.gravitee.gateway.api.Policy;
import io.gravitee.gateway.api.PolicyChain;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.policy.PolicyChainBuilder;
import io.gravitee.gateway.core.policy.PolicyProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class AbstractPolicyChainBuilder<T extends PolicyChain> implements PolicyChainBuilder<T, Request> {

    @Autowired
    private PolicyProvider policyProvider;

    protected Set<Policy> policies() {
        return policyProvider.getPolicies();
    }
}
