package io.gravitee.gateway.core.policy;

import io.gravitee.gateway.api.Policy;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface PolicyBuilder {

    Policy build(PolicyDefinition policyDefinition, String configuration);
}
