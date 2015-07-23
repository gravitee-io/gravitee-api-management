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
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RequestPolicyChainTest {

    private Policy policy = new Policy() {
        @Override
        public void onRequest(Object... args) throws Exception {}

        @Override
        public void onResponse(Object... args) throws Exception {}
    };

    @Before
    public void setUp() {
        policy = spy(policy);
    }

    @Test
    public void doNext_emptyPolicies() throws Exception {
        PolicyChain chain = new RequestPolicyChain(new ArrayList<>());
        chain.doNext(null, null);

        verify(policy, never()).onRequest();
        verify(policy, never()).onResponse();
    }

    @Test
    public void doNext_listOfPolicies() throws Exception {
        PolicyChain chain = new RequestPolicyChain(policies());
        chain.doNext(null, null);

//        verify(policy, atLeastOnce()).onRequest();
//        verify(policy, never()).onResponse();
    }

    private List<Policy> policies() {
        List<Policy> policies = new ArrayList<>();

        policies.add(policy);

        return policies;
    }
}
