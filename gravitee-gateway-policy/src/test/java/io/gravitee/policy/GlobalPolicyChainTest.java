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
package io.gravitee.policy;

import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.policy.Policy;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Spy;

import io.gravitee.gateway.policy.impl.PolicyChain;
import io.gravitee.gateway.policy.impl.RequestPolicyChain;
import io.gravitee.gateway.policy.impl.ResponsePolicyChain;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class GlobalPolicyChainTest {

    @Spy
    private Policy policy = new SuccessPolicy();

    @Spy
    private Policy policy2 = new SuccessPolicy();

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void doNext_multiplePolicyOrder() throws Exception {
        List<Policy> policies = policies2();

        PolicyChain requestChain = RequestPolicyChain.create(policies, mock(ExecutionContext.class));
        requestChain.setResultHandler(result -> {});

        PolicyChain responseChain = ResponsePolicyChain.create(policies, mock(ExecutionContext.class));
        responseChain.setResultHandler(result -> {});

        InOrder requestOrder = inOrder(policy, policy2);
        InOrder responseOrder = inOrder(policy, policy2);

        requestChain.doNext(null, null);
        responseChain.doNext(null, null);

        requestOrder.verify(policy).onRequest(anyVararg());
        requestOrder.verify(policy2).onRequest(anyVararg());

        responseOrder.verify(policy2).onResponse(anyVararg());
        responseOrder.verify(policy).onResponse(anyVararg());
    }

    private List<Policy> policies2() {
        List<Policy> policies = new ArrayList<>();
        policies.add(policy);
        policies.add(policy2);
        return policies;
    }
}
