package io.gravitee.gateway.core.policy;

import io.gravitee.gateway.api.policy.PolicyChain;
import io.gravitee.gateway.core.policy.impl.RequestPolicyChain;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RequestPolicyChainTest {

    @Test
    public void doNext_emptyPolicies() {
        PolicyChain chain = new RequestPolicyChain(new ArrayList<>());
        chain.doNext(null, null);
    }

    @Test
    public void doNext_listOfPolicies() {
        PolicyChain chain = new RequestPolicyChain(policies());
        chain.doNext(null, null);
    }

    private List<Policy> policies() {
        List<Policy> policies = new ArrayList<>();

        policies.add(new Policy() {

            @Override
            public void onRequest(Object... args) throws Exception {

            }

            @Override
            public void onResponse(Object... args) throws Exception {

            }
        });
        return policies;
    }
}
