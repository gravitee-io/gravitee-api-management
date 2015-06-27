package io.gravitee.gateway.core.policy;

import io.gravitee.gateway.api.Policy;
import io.gravitee.gateway.core.policy.impl.AccessControlPolicy;
import io.gravitee.gateway.core.policy.impl.RateLimitPolicy;

import java.util.HashSet;
import java.util.Set;

/**
 * TODO: Policy provider should be replace by getting policies from the API configuration
 * This policy provider comes only with "default" policies.
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyProvider {

    private final Set<Policy> policies = new HashSet();

    {
        policies.add(new AccessControlPolicy());
        policies.add(new RateLimitPolicy());
    }

    public Set<Policy> getPolicies() {
        return policies;
    }
}
