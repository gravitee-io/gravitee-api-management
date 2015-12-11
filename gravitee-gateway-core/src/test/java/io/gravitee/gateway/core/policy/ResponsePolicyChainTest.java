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

import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;

import io.gravitee.gateway.api.ExecutionContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Spy;

import io.gravitee.gateway.core.policy.impl.AbstractPolicyChain;
import io.gravitee.gateway.core.policy.impl.ResponsePolicyChain;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ResponsePolicyChainTest {

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

    @Test
    public void doNext_emptyPolicies() throws Exception {
        AbstractPolicyChain chain = new ResponsePolicyChain(new ArrayList<>(), mock(ExecutionContext.class));
        chain.setResultHandler(result -> {});
        chain.doNext(null, null);

        verify(policy, never()).onRequest();
        verify(policy, never()).onResponse();
    }

    @Test
    public void doNext_singlePolicy() throws Exception {
        AbstractPolicyChain chain = new ResponsePolicyChain(policies(), mock(ExecutionContext.class));
        chain.setResultHandler(result -> {});
        chain.doNext(null, null);

        verify(policy, never()).onRequest(anyVararg());
        verify(policy, atLeastOnce()).onResponse(anyVararg());
    }

    @Test
    public void doNext_multiplePolicy() throws Exception {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        AbstractPolicyChain chain = new ResponsePolicyChain(policies2(), executionContext);
        chain.setResultHandler(result -> {});

        chain.doNext(null, null);

        verify(policy, atLeastOnce()).onResponse(null, null, chain, executionContext);
        verify(policy2, atLeastOnce()).onResponse(null, null, chain, executionContext);
    }

    @Test
    public void doNext_multiplePolicyOrder() throws Exception {
        AbstractPolicyChain chain = new ResponsePolicyChain(policies2(), mock(ExecutionContext.class));
        chain.setResultHandler(result -> {});

        InOrder inOrder = inOrder(policy, policy2);

        chain.doNext(null, null);

        inOrder.verify(policy2).onResponse(anyVararg());
        inOrder.verify(policy).onResponse(anyVararg());
    }

    @Test
    public void doNext_multiplePolicy_throwError() throws Exception {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        AbstractPolicyChain chain = new ResponsePolicyChain(policies3(), executionContext);
        chain.setResultHandler(result -> {});
        chain.doNext(null, null);

        verify(policy3, atLeastOnce()).onResponse(null, null, chain, executionContext);
        verify(policy2, never()).onResponse(null, null, chain, executionContext);
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
        policies.add(policy2);
        policies.add(policy3);
        return policies;
    }
}
