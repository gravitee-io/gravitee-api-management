/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.core.policy;

import io.gravitee.gateway.api.policy.PolicyChain;
import io.gravitee.gateway.core.policy.impl.RequestPolicyChain;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RequestPolicyChainTest {

    private Policy policy = new Policy() {
        @Override
        public void onRequest(Object... args) throws Exception {
            ((PolicyChain)args[2]).doNext(null, null);
        }

        @Override
        public void onResponse(Object... args) throws Exception {}
    };

    private Policy policy2 = new Policy() {
        @Override
        public void onRequest(Object... args) throws Exception {
            ((PolicyChain)args[2]).doNext(null, null);
        }

        @Override
        public void onResponse(Object... args) throws Exception {}
    };

    private Policy policy3 = new Policy() {
        @Override
        public void onRequest(Object... args) throws Exception {
            throw new RuntimeException();
        }

        @Override
        public void onResponse(Object... args) throws Exception {}
    };

    @Before
    public void setUp() {
        policy = spy(policy);
        policy2 = spy(policy2);
        policy3 = spy(policy3);
    }

    @Test
    public void doNext_emptyPolicies() throws Exception {
        PolicyChain chain = new RequestPolicyChain(new ArrayList<>());
        chain.doNext(null, null);

        verify(policy, never()).onRequest();
        verify(policy, never()).onResponse();
    }

    @Test
    public void doNext_singlePolicy() throws Exception {
        PolicyChain chain = new RequestPolicyChain(policies());
        chain.doNext(null, null);

        verify(policy, atLeastOnce()).onRequest(anyVararg());
        verify(policy, never()).onResponse(anyVararg());
    }

    @Test
    public void doNext_multiplePolicy() throws Exception {
        PolicyChain chain = new RequestPolicyChain(policies2());
        chain.doNext(null, null);

        verify(policy, atLeastOnce()).onRequest(null, null, chain);
        verify(policy2, atLeastOnce()).onRequest(null, null, chain);
    }

    @Test
    public void doNext_multiplePolicyOrder() throws Exception {
        PolicyChain chain = new RequestPolicyChain(policies2());
        InOrder inOrder = inOrder(policy, policy2);

        chain.doNext(null, null);

        inOrder.verify(policy).onRequest(anyVararg());
        inOrder.verify(policy2).onRequest(anyVararg());
    }

    @Test
    public void doNext_multiplePolicy_throwError() throws Exception {
        PolicyChain chain = new RequestPolicyChain(policies3());
        chain.doNext(null, null);

        verify(policy3, atLeastOnce()).onRequest(null, null, chain);
        verify(policy2, atLeastOnce()).onRequest(null, null, chain);
    }

    private List<Policy> policies() {
        List<Policy> policies = new ArrayList<>();
        policies.add(policy);
        return policies;
    }

    private List<Policy> policies2() {
        List<Policy> policies = new ArrayList<>();
        policies.add(policy);
        policies.add(policy2);
        return policies;
    }

    private List<Policy> policies3() {
        List<Policy> policies = new ArrayList<>();
        policies.add(policy3);
        policies.add(policy2);
        return policies;
    }
}
