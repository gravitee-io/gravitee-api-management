package io.gravitee.gateway.core.policy.rules;

import io.gravitee.gateway.core.policy.PolicyAdapter;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class TransformPolicy extends PolicyAdapter {

    @Override
    public String name() {
        return "Transform Policy";
    }
}
