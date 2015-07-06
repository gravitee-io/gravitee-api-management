package io.gravitee.gateway.core.service.impl;

import io.gravitee.gateway.core.policy.PolicyDefinition;
import io.gravitee.gateway.core.policy.PolicyRegistry;
import io.gravitee.gateway.core.service.PolicyService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyServiceImpl implements PolicyService {

    @Autowired
    private PolicyRegistry policyRegistry;

    @Override
    public Set<PolicyDefinition> findAll() {
        return new HashSet<>(policyRegistry.policies());
    }

    @Override
    public PolicyDefinition get(String name) {
        return policyRegistry.getPolicy(name);
    }
}
