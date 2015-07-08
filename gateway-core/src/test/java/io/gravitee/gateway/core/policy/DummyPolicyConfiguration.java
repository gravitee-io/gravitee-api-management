package io.gravitee.gateway.core.policy;

import io.gravitee.gateway.api.PolicyConfiguration;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class DummyPolicyConfiguration implements PolicyConfiguration {

    private int value;

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
