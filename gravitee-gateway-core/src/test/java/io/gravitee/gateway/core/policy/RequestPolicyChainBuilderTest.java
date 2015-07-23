package io.gravitee.gateway.core.policy;

import io.gravitee.gateway.api.policy.PolicyChain;
import io.gravitee.gateway.core.policy.impl.RequestPolicyChainBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RequestPolicyChainBuilderTest {
    
    @Test(expected = IllegalArgumentException.class)
    public void buildPolicyChain_withNullPolicies() {
        PolicyChainBuilder builder = new RequestPolicyChainBuilder();
        builder.newPolicyChain(null);
    }

    @Test
    public void buildPolicyChain_withEmptyPolicies() {
        PolicyChainBuilder builder = new RequestPolicyChainBuilder();
        PolicyChain chain = builder.newPolicyChain(new ArrayList<>());

        Assert.assertNotNull(chain);
    }
}
