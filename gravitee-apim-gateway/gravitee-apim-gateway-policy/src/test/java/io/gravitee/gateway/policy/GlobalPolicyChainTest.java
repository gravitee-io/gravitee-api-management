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
package io.gravitee.gateway.policy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.policy.impl.OrderedPolicyChain;
import io.gravitee.gateway.policy.impl.PolicyChain;
import io.gravitee.gateway.policy.impl.ReversedPolicyChain;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Spy;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
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

        PolicyChain requestChain = OrderedPolicyChain.create(policies, mock(ExecutionContext.class));
        requestChain.handler(result -> {});

        PolicyChain responseChain = ReversedPolicyChain.create(policies, mock(ExecutionContext.class));
        responseChain.handler(result -> {});

        InOrder requestOrder = inOrder(policy, policy2);
        InOrder responseOrder = inOrder(policy, policy2);

        requestChain.doNext(null, null);
        responseChain.doNext(null, null);

        requestOrder.verify(policy).execute(any(), any());
        requestOrder.verify(policy2).execute(any(), any());

        responseOrder.verify(policy2).execute(any(), any());
        responseOrder.verify(policy).execute(any(), any());
    }

    private List<Policy> policies2() {
        List<Policy> policies = new ArrayList<>();
        policies.add(policy);
        policies.add(policy2);
        return policies;
    }
}
