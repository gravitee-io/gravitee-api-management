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
package io.gravitee.gateway.jupiter.policy;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.policy.Policy;
import io.gravitee.gateway.jupiter.core.context.MutableMessageExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableRequestExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionException;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionFailureException;
import io.gravitee.gateway.jupiter.reactor.handler.context.DefaultRequestExecutionContext;
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
    protected static final String MOCK_STATUS_ERROR_MESSAGE = "Mock error on status";

    @Test
    public void shouldExecuteNothingWithEmptyPolicyList() {
        PolicyChain cut = new PolicyChain(CHAIN_ID, new ArrayList<>(), ExecutionPhase.REQUEST);
        final MutableRequestExecutionContext ctx = mock(MutableRequestExecutionContext.class);
        final TestObserver<Void> obs = cut.execute(ctx).test();

        obs.assertComplete();
    }

    @Test
    public void shouldExecutePoliciesOnRequest() {
        final Policy policy1 = mock(Policy.class);
        final Policy policy2 = mock(Policy.class);
        final MutableRequestExecutionContext ctx = mock(MutableRequestExecutionContext.class);

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
        final MutableRequestExecutionContext ctx = mock(MutableRequestExecutionContext.class);

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
        final MutableMessageExecutionContext ctx = mock(MutableMessageExecutionContext.class);

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
        final MutableMessageExecutionContext ctx = mock(MutableMessageExecutionContext.class);

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
        final DefaultRequestExecutionContext ctx = new DefaultRequestExecutionContext(null, null);
        when(policy1.onRequest(ctx)).thenAnswer(invocation -> ((RequestExecutionContext) invocation.getArgument(0)).interrupt());

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
        final DefaultRequestExecutionContext ctx = new DefaultRequestExecutionContext(null, response);

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
        final DefaultRequestExecutionContext ctx = new DefaultRequestExecutionContext(null, response);

        final PolicyChain cut = new PolicyChain(CHAIN_ID, asList(policy1, policy2), ExecutionPhase.REQUEST);
        when(policy1.onRequest(ctx)).thenReturn(Completable.complete());
        when(policy1.onRequest(ctx)).thenReturn(Completable.error(new RuntimeException(MOCK_ERROR_MESSAGE)));

        cut.execute(ctx).test().assertErrorMessage(MOCK_ERROR_MESSAGE).assertFailure(RuntimeException.class);

        verify(policy1).onRequest(ctx);
    }
}
