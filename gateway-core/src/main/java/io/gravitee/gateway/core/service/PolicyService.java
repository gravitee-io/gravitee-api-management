package io.gravitee.gateway.core.service;

import io.gravitee.gateway.core.policy.PolicyDefinition;

import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface PolicyService {

    Set<PolicyDefinition> findAll();

    PolicyDefinition get(String name);
}
