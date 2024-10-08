/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactive.policy;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionException;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class PolicyChainTest {

    protected static final String CHAIN_ID = "unit-test";
    protected static final String MOCK_ERROR_MESSAGE = "Mock error";
    protected static final String MOCK_STATUS_ERROR_MESSAGE = "Mock error on status";

    @Test
    public void shouldExecuteNothingWithEmptyPolicyList() {
        PolicyChain cut = new PolicyChain(CHAIN_ID, new ArrayList<>(), ExecutionPhase.REQUEST);
        final HttpExecutionContext ctx = mock(HttpExecutionContext.class);
        final TestObserver<Void> obs = cut.execute(ctx).test();

        obs.assertComplete();
    }

    @Test
    public void shouldExecutePoliciesOnRequest() {
        final Policy policy1 = mock(Policy.class);
        final Policy policy2 = mock(Policy.class);
        final HttpExecutionContext ctx = mock(HttpExecutionContext.class);

        final PolicyChain cut = new PolicyChain(CHAIN_ID, asList(policy1, policy2), ExecutionPhase.REQUEST);

        when(policy1.onRequest(ctx)).thenReturn(Completable.complete());
        when(policy2.onRequest(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        verify(policy1).onRequest(ctx);
        verify(policy2).onRequest(ctx);
    }

    @Test
    public void shouldExecutePoliciesOnResponse() {
        final Policy policy1 = mock(Policy.class);
        final Policy policy2 = mock(Policy.class);
        final HttpExecutionContext ctx = mock(HttpExecutionContext.class);

        final PolicyChain cut = new PolicyChain(CHAIN_ID, asList(policy1, policy2), ExecutionPhase.RESPONSE);

        when(policy1.onResponse(ctx)).thenReturn(Completable.complete());
        when(policy2.onResponse(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        verify(policy1).onResponse(ctx);
        verify(policy2).onResponse(ctx);
    }

    @Test
    public void shouldExecutePoliciesOnAsyncRequest() {
        final Policy policy1 = mock(Policy.class);
        final Policy policy2 = mock(Policy.class);
        final HttpExecutionContext ctx = mock(HttpExecutionContext.class);

        final PolicyChain cut = new PolicyChain(CHAIN_ID, asList(policy1, policy2), ExecutionPhase.MESSAGE_REQUEST);

        when(policy1.onMessageRequest(ctx)).thenReturn(Completable.complete());
        when(policy2.onMessageRequest(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> ctxObserver = cut.execute(ctx).test();
        ctxObserver.assertResult();

        verify(policy1).onMessageRequest(ctx);
        verify(policy2).onMessageRequest(ctx);
    }

    @Test
    public void shouldExecutePoliciesOnAResponse() {
        final Policy policy1 = mock(Policy.class);
        final Policy policy2 = mock(Policy.class);
        final HttpExecutionContext ctx = mock(HttpExecutionContext.class);

        final PolicyChain cut = new PolicyChain(CHAIN_ID, asList(policy1, policy2), ExecutionPhase.MESSAGE_RESPONSE);

        when(policy1.onMessageResponse(ctx)).thenReturn(Completable.complete());
        when(policy2.onMessageResponse(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        verify(policy1).onMessageResponse(ctx);
        verify(policy2).onMessageResponse(ctx);
    }

    @Test
    public void shouldExecuteOnlyPolicy1IfInterrupted() {
        final Policy policy1 = mock(Policy.class);
        final Policy policy2 = mock(Policy.class);
        final HttpExecutionContext ctx = new DefaultExecutionContext(null, null);
        when(policy1.onRequest(ctx)).thenAnswer(invocation -> ((HttpExecutionContext) invocation.getArgument(0)).interrupt());

        final PolicyChain cut = new PolicyChain(CHAIN_ID, asList(policy1, policy2), ExecutionPhase.REQUEST);

        cut.execute(ctx).test().assertFailure(InterruptionException.class);

        verify(policy1).onRequest(ctx);

        verifyNoMoreInteractions(policy2);
    }

    @Test
    public void shouldExecuteOnlyPolicy1AndInterruptWhenPolicy1Error() {
        final Policy policy1 = mock(Policy.class);
        final Policy policy2 = mock(Policy.class);
        final MutableResponse response = mock(MutableResponse.class);
        final HttpExecutionContext ctx = new DefaultExecutionContext(null, response);

        final PolicyChain cut = new PolicyChain(CHAIN_ID, asList(policy1, policy2), ExecutionPhase.REQUEST);
        when(policy1.onRequest(ctx)).thenAnswer(invocation -> ctx.interruptWith(new ExecutionFailure(400).message(MOCK_ERROR_MESSAGE)));

        cut.execute(ctx).test().assertFailure(InterruptionFailureException.class);

        verify(policy1).onRequest(ctx);
        verifyNoMoreInteractions(policy2);
    }

    @Test
    public void shouldErrorWhenPolicyError() {
        final Policy policy1 = mock(Policy.class);
        final Policy policy2 = mock(Policy.class);
        final MutableResponse response = mock(MutableResponse.class);
        final HttpExecutionContext ctx = new DefaultExecutionContext(null, response);

        final PolicyChain cut = new PolicyChain(CHAIN_ID, asList(policy1, policy2), ExecutionPhase.REQUEST);
        when(policy1.onRequest(ctx)).thenReturn(Completable.complete());
        when(policy1.onRequest(ctx)).thenReturn(Completable.error(new RuntimeException(MOCK_ERROR_MESSAGE)));

        cut
            .execute(ctx)
            .test()
            .assertError(RuntimeException.class)
            .assertError(t -> MOCK_ERROR_MESSAGE.equals(t.getMessage()))
            .assertFailure(RuntimeException.class);

        verify(policy1).onRequest(ctx);
    }
}
