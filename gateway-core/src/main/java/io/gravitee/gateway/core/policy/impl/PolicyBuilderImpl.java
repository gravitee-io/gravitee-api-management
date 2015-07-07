package io.gravitee.gateway.core.policy.impl;

import io.gravitee.gateway.api.Policy;
import io.gravitee.gateway.api.PolicyConfiguration;
import io.gravitee.gateway.core.policy.PolicyBuilder;
import io.gravitee.gateway.core.policy.PolicyDefinition;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyBuilderImpl implements PolicyBuilder {

    @Override
    public Policy build(PolicyDefinition policyDefinition, String configuration) {
        Class<PolicyConfiguration> policyConfiguration = policyDefinition.configuration();
        Class<? extends Policy> policyClass = policyDefinition.policy();

        Policy policy = null;

        try {
            if (policyClass != null) {
                policy = createInstance(policyClass);

                if (policyConfiguration != null) {

                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        return policy;
    }

    private <T> T createInstance(Class<T> clazz) throws IllegalAccessException, InstantiationException {
        return clazz.newInstance();
    }
}
