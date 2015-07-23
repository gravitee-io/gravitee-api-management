package io.gravitee.gateway.core.policy;

import io.gravitee.gateway.api.policy.PolicyChain;
import io.gravitee.gateway.core.policy.impl.RequestPolicyChainBuilder;
import io.gravitee.gateway.core.policy.impl.ResponsePolicyChainBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ResponsePolicyChainBuilderTest {
    
    @Test(expected = IllegalArgumentException.class)
    public void buildPolicyChain_withNullPolicies() {
        PolicyChainBuilder builder = new ResponsePolicyChainBuilder();
        builder.newPolicyChain(null);
    }

    @Test
    public void buildPolicyChain_withEmptyPolicies() {
        PolicyChainBuilder builder = new ResponsePolicyChainBuilder();
        PolicyChain chain = builder.newPolicyChain(new ArrayList<>());

        Assert.assertNotNull(chain);
    }
}
