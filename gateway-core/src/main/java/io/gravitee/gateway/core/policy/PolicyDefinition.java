package io.gravitee.gateway.core.policy;

import io.gravitee.gateway.api.Policy;
import io.gravitee.gateway.api.PolicyConfiguration;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface PolicyDefinition {

    String name();

    String description();

    Class<Policy> policy();

    Class<PolicyConfiguration> configuration();
}
