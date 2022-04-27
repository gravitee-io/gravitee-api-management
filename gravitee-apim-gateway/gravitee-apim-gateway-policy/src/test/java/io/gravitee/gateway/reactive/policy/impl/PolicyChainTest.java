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
package io.gravitee.gateway.reactive.policy.impl;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.async.AsyncExecutionContext;
import io.gravitee.gateway.reactive.api.context.sync.SyncExecutionContext;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.gateway.reactive.policy.PolicyChain;
import io.reactivex.Completable;
import io.reactivex.observers.TestObserver;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class PolicyChainTest {

    protected static final String CHAIN_ID = "unit-test";
    protected static final String MOCK_ERROR_MESSAGE = "Mock error";

    @Test
    public void shouldExecuteNothingWithEmptyPolicyList() {
        PolicyChain cut = new PolicyChain(CHAIN_ID, new ArrayList<>(), ExecutionPhase.REQUEST);
        final ExecutionContext<?, ?> ctx = mock(ExecutionContext.class);
        final TestObserver<Void> obs = cut.execute(ctx).test();

        obs.assertComplete();
    }

    @Test
    public void shouldExecutePoliciesOnRequest() {
        final Policy policy1 = mock(Policy.class);
        final Policy policy2 = mock(Policy.class);
        final SyncExecutionContext ctx = mock(SyncExecutionContext.class);

        final PolicyChain cut = new PolicyChain(CHAIN_ID, asList(policy1, policy2), ExecutionPhase.REQUEST);

        when(policy1.onRequest(ctx)).thenReturn(Completable.complete());
        when(policy2.onRequest(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        verify(policy1).onRequest(ctx);
        verify(policy1).getId();
        verify(policy2).onRequest(ctx);
        verify(policy2).getId();

        verifyNoMoreInteractions(policy1, policy2);
    }

    @Test
    public void shouldExecutePoliciesOnResponse() {
        final Policy policy1 = mock(Policy.class);
        final Policy policy2 = mock(Policy.class);
        final SyncExecutionContext ctx = mock(SyncExecutionContext.class);

        final PolicyChain cut = new PolicyChain(CHAIN_ID, asList(policy1, policy2), ExecutionPhase.RESPONSE);

        when(policy1.onResponse(ctx)).thenReturn(Completable.complete());
        when(policy2.onResponse(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        verify(policy1).onResponse(ctx);
        verify(policy1).getId();
        verify(policy2).onResponse(ctx);
        verify(policy2).getId();

        verifyNoMoreInteractions(policy1, policy2);
    }

    @Test
    public void shouldExecutePoliciesOnAsyncRequest() {
        final Policy policy1 = mock(Policy.class);
        final Policy policy2 = mock(Policy.class);
        final AsyncExecutionContext ctx = mock(AsyncExecutionContext.class);

        final PolicyChain cut = new PolicyChain(CHAIN_ID, asList(policy1, policy2), ExecutionPhase.ASYNC_REQUEST);

        when(policy1.onAsyncRequest(ctx)).thenReturn(Completable.complete());
        when(policy2.onAsyncRequest(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        verify(policy1).onAsyncRequest(ctx);
        verify(policy1).getId();
        verify(policy2).onAsyncRequest(ctx);
        verify(policy2).getId();

        verifyNoMoreInteractions(policy1, policy2);
    }

    @Test
    public void shouldExecutePoliciesOnAsyncResponse() {
        final Policy policy1 = mock(Policy.class);
        final Policy policy2 = mock(Policy.class);
        final AsyncExecutionContext ctx = mock(AsyncExecutionContext.class);

        final PolicyChain cut = new PolicyChain(CHAIN_ID, asList(policy1, policy2), ExecutionPhase.ASYNC_RESPONSE);

        when(policy1.onAsyncResponse(ctx)).thenReturn(Completable.complete());
        when(policy2.onAsyncResponse(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        verify(policy1).onAsyncResponse(ctx);
        verify(policy1).getId();
        verify(policy2).onAsyncResponse(ctx);
        verify(policy2).getId();

        verifyNoMoreInteractions(policy1, policy2);
    }

    @Test
    public void shouldExecuteOnlyPolicy1IfInterrupted() {
        final Policy policy1 = mock(Policy.class);
        final Policy policy2 = mock(Policy.class);
        final SyncExecutionContext ctx = mock(SyncExecutionContext.class);

        final PolicyChain cut = new PolicyChain(CHAIN_ID, asList(policy1, policy2), ExecutionPhase.REQUEST);

        when(ctx.isInterrupted()).thenReturn(false).thenReturn(true);
        when(policy1.onRequest(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        verify(policy1).onRequest(ctx);
        verify(policy1).getId();

        verifyNoMoreInteractions(policy1, policy2);
    }

    @Test
    public void shouldExecuteOnlyPolicy1IfError() {
        final Policy policy1 = mock(Policy.class);
        final Policy policy2 = mock(Policy.class);
        final SyncExecutionContext ctx = mock(SyncExecutionContext.class);

        final PolicyChain cut = new PolicyChain(CHAIN_ID, asList(policy1, policy2), ExecutionPhase.REQUEST);

        when(policy1.onRequest(ctx)).thenReturn(Completable.error(new RuntimeException(MOCK_ERROR_MESSAGE)));

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertErrorMessage(MOCK_ERROR_MESSAGE);

        verify(policy1).onRequest(ctx);
        verify(policy1).getId();

        verifyNoMoreInteractions(policy1, policy2);
    }
}
