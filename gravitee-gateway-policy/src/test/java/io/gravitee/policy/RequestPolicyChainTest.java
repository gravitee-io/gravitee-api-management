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

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.impl.PolicyChain;
import io.gravitee.gateway.policy.impl.RequestPolicyChain;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RequestPolicyChainTest {

    @Spy
    private Policy policy = new SuccessPolicy();

    @Spy
    private Policy policy2 = new SuccessPolicy();

    @Spy
    private Policy policy3 = new FailurePolicy();

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test(expected = NullPointerException.class)
    public void buildPolicyChain_withNullPolicies() {
        RequestPolicyChain.create(null, mock(ExecutionContext.class));
    }

    @Test
    public void buildPolicyChain_withEmptyPolicies() {
        io.gravitee.policy.api.PolicyChain chain = RequestPolicyChain.create(new ArrayList<>(), mock(ExecutionContext.class));

        Assert.assertNotNull(chain);
    }

    @Test
    public void doNext_emptyPolicies() throws Exception {
        PolicyChain chain = RequestPolicyChain.create(new ArrayList<>(), mock(ExecutionContext.class));
        chain.setResultHandler(result -> {});

        chain.doNext(null, null);

        verify(policy, never()).onRequest();
        verify(policy, never()).onResponse();
    }

    @Test
    public void doNext_singlePolicy() throws Exception {
        PolicyChain chain = RequestPolicyChain.create(policies(), mock(ExecutionContext.class));
        chain.setResultHandler(result -> {});

        chain.doNext(null, null);

        verify(policy, atLeastOnce()).onRequest(anyVararg());
        verify(policy, never()).onResponse(anyVararg());
    }

    @Test
    public void doNext_multiplePolicy() throws Exception {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        PolicyChain chain = RequestPolicyChain.create(policies2(), executionContext);
        chain.setResultHandler(result -> {});

        chain.doNext(null, null);

        verify(policy, atLeastOnce()).onRequest(null, null, chain, executionContext);
        verify(policy2, atLeastOnce()).onRequest(null, null, chain, executionContext);
    }

    @Test
    public void doNext_multiplePolicyOrder() throws Exception {
        PolicyChain chain = RequestPolicyChain.create(policies2(), mock(ExecutionContext.class));
        chain.setResultHandler(result -> {});

        InOrder inOrder = inOrder(policy, policy2);

        chain.doNext(null, null);

        inOrder.verify(policy).onRequest(anyVararg());
        inOrder.verify(policy2).onRequest(anyVararg());
    }

    @Test
    public void doNext_multiplePolicy_throwError() throws Exception {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        PolicyChain chain = RequestPolicyChain.create(policies3(), executionContext);
        chain.setResultHandler(result -> {});
        chain.doNext(null, null);

        verify(policy3, atLeastOnce()).onRequest(null, null, chain, executionContext);
        verify(policy2, never()).onRequest(null, null, chain);
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
