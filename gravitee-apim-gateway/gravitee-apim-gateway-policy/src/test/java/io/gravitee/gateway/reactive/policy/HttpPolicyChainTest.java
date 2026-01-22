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
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionException;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.ArrayList;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class HttpPolicyChainTest {

    protected static final String CHAIN_ID = "unit-test";
    protected static final String MOCK_ERROR_MESSAGE = "Mock error";

    @Mock
    MutableRequest request;

    @Mock
    MutableResponse response;

    @Mock
    ComponentProvider componentProvider;

    @Mock
    Node node;

    @Mock
    Metrics metrics;

    @Mock
    Function<HttpExecutionContext, Completable> onActionResponse;

    @Mock
    Function<HttpExecutionContext, Completable> onActionResponse2;

    DefaultExecutionContext ctx;

    @BeforeEach
    void setUp() {
        lenient().when(componentProvider.getComponent(Node.class)).thenReturn(node);
        ctx = new DefaultExecutionContext(request, response).componentProvider(componentProvider);
        ctx.metrics(metrics);
    }

    @Test
    void should_execute_nothing_with_empty_policy_list() {
        // Arrange
        HttpPolicyChain httpPolicyChain = new HttpPolicyChain(CHAIN_ID, new ArrayList<>(), ExecutionPhase.REQUEST);

        // Act
        final TestObserver<Void> obs = httpPolicyChain.execute(ctx).test();

        // Assert
        obs.assertComplete();
    }

    @Test
    void should_execute_policies_on_request() {
        // Arrange
        final HttpPolicy httpPolicy1 = mock(HttpPolicy.class);
        final HttpPolicy httpPolicy2 = mock(HttpPolicy.class);

        final HttpPolicyChain httpPolicyChain = new HttpPolicyChain(CHAIN_ID, asList(httpPolicy1, httpPolicy2), ExecutionPhase.REQUEST);

        when(httpPolicy1.onRequest(ctx)).thenReturn(Completable.complete());
        when(httpPolicy2.onRequest(ctx)).thenReturn(Completable.complete());

        // Act
        final TestObserver<Void> obs = httpPolicyChain.execute(ctx).test();
        obs.assertComplete();

        // Assert
        verify(httpPolicy1).onRequest(ctx);
        verify(httpPolicy2).onRequest(ctx);
    }

    @Test
    void should_execute_policies_on_response() {
        // Arrange
        final HttpPolicy httpPolicy1 = mock(HttpPolicy.class);
        final HttpPolicy httpPolicy2 = mock(HttpPolicy.class);

        final HttpPolicyChain httpPolicyChain = new HttpPolicyChain(CHAIN_ID, asList(httpPolicy1, httpPolicy2), ExecutionPhase.RESPONSE);

        when(httpPolicy1.onResponse(ctx)).thenReturn(Completable.complete());
        when(httpPolicy2.onResponse(ctx)).thenReturn(Completable.complete());

        // Act
        final TestObserver<Void> obs = httpPolicyChain.execute(ctx).test();
        obs.assertComplete();

        // Assert
        verify(httpPolicy1).onResponse(ctx);
        verify(httpPolicy2).onResponse(ctx);
    }

    @Test
    void should_execute_on_response_actions_in_order_on_response_phase() {
        // Arrange
        final HttpPolicy httpPolicy1 = mock(HttpPolicy.class);
        final HttpPolicy httpPolicy2 = mock(HttpPolicy.class);

        ctx.addActionOnResponse(httpPolicy1, onActionResponse);
        ctx.addActionOnResponse(httpPolicy2, onActionResponse2);

        when(onActionResponse.apply(ctx)).thenReturn(Completable.complete());
        when(onActionResponse2.apply(ctx)).thenReturn(Completable.complete());

        final HttpPolicyChain httpPolicyChain = new HttpPolicyChain(CHAIN_ID, asList(httpPolicy1, httpPolicy2), ExecutionPhase.RESPONSE);

        // Act
        final TestObserver<Void> obs = httpPolicyChain.executeActionsOnResponse(ctx).test();
        obs.assertComplete();

        // Assert
        InOrder inOrder = inOrder(onActionResponse, onActionResponse2);

        inOrder.verify(onActionResponse2).apply(ctx);
        inOrder.verify(onActionResponse).apply(ctx);
    }

    @Test
    void should_not_execute_on_response_actions_when_not_response_phase() {
        // Arrange
        final HttpPolicy httpPolicy1 = mock(HttpPolicy.class);
        final HttpPolicy httpPolicy2 = mock(HttpPolicy.class);

        ctx.addActionOnResponse(httpPolicy1, onActionResponse);

        final HttpPolicyChain httpPolicyChain = new HttpPolicyChain(CHAIN_ID, asList(httpPolicy1, httpPolicy2), ExecutionPhase.REQUEST);

        when(httpPolicy1.onRequest(ctx)).thenReturn(Completable.complete());
        when(httpPolicy2.onRequest(ctx)).thenReturn(Completable.complete());

        // Act
        final TestObserver<Void> obs = httpPolicyChain.execute(ctx).test();
        obs.assertComplete();

        // Assert
        verify(onActionResponse, never()).apply(ctx);
        verify(httpPolicy1).onRequest(ctx);
        verify(httpPolicy2).onRequest(ctx);
    }

    @Test
    void should_interrupt_with_first_policy_in_error() {
        // Arrange
        final HttpPolicy httpPolicy1 = mock(HttpPolicy.class);
        final HttpPolicy httpPolicy2 = mock(HttpPolicy.class);

        final HttpPolicyChain httpPolicyChain = new HttpPolicyChain(CHAIN_ID, asList(httpPolicy1, httpPolicy2), ExecutionPhase.REQUEST);

        when(httpPolicy1.onRequest(ctx)).thenReturn(Completable.error(new RuntimeException("policy1 failed")));

        // Act
        final TestObserver<Void> obs = httpPolicyChain.execute(ctx).test();
        obs
            .assertError(RuntimeException.class)
            .assertError(t -> "policy1 failed".equals(t.getMessage()))
            .assertFailure(RuntimeException.class);

        // Assert
        verify(httpPolicy1).onRequest(ctx);
        verify(httpPolicy2, never()).onRequest(ctx);
    }

    @Test
    public void should_execute_policies_on_async_request() {
        final HttpPolicy policy1 = mock(HttpPolicy.class);
        final HttpPolicy policy2 = mock(HttpPolicy.class);

        final HttpPolicyChain cut = new HttpPolicyChain(CHAIN_ID, asList(policy1, policy2), ExecutionPhase.MESSAGE_REQUEST);

        when(policy1.onMessageRequest(ctx)).thenReturn(Completable.complete());
        when(policy2.onMessageRequest(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> ctxObserver = cut.execute(ctx).test();
        ctxObserver.assertResult();

        verify(policy1).onMessageRequest(ctx);
        verify(policy2).onMessageRequest(ctx);
    }

    @Test
    public void should_execute_policies_on_async_response() {
        final HttpPolicy policy1 = mock(HttpPolicy.class);
        final HttpPolicy policy2 = mock(HttpPolicy.class);

        final HttpPolicyChain cut = new HttpPolicyChain(CHAIN_ID, asList(policy1, policy2), ExecutionPhase.MESSAGE_RESPONSE);

        when(policy1.onMessageResponse(ctx)).thenReturn(Completable.complete());
        when(policy2.onMessageResponse(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        verify(policy1).onMessageResponse(ctx);
        verify(policy2).onMessageResponse(ctx);
    }

    @Test
    public void should_execute_only_policy_1_if_interrupted() {
        final HttpPolicy policy1 = mock(HttpPolicy.class);
        final HttpPolicy policy2 = mock(HttpPolicy.class);
        final HttpExecutionContext ctx = new DefaultExecutionContext(null, null).componentProvider(componentProvider).metrics(metrics);
        when(policy1.onRequest(ctx)).thenAnswer(invocation -> ((HttpExecutionContext) invocation.getArgument(0)).interrupt());

        final HttpPolicyChain cut = new HttpPolicyChain(CHAIN_ID, asList(policy1, policy2), ExecutionPhase.REQUEST);

        cut.execute(ctx).test().assertFailure(InterruptionException.class);

        verify(policy1).onRequest(ctx);

        verifyNoMoreInteractions(policy2);
    }

    @Test
    public void should_execute_only_policy_1_and_interrupt_when_policy_1_error() {
        final HttpPolicy policy1 = mock(HttpPolicy.class);
        final HttpPolicy policy2 = mock(HttpPolicy.class);
        final HttpExecutionContext ctx = new DefaultExecutionContext(null, response).componentProvider(componentProvider).metrics(metrics);

        final HttpPolicyChain cut = new HttpPolicyChain(CHAIN_ID, asList(policy1, policy2), ExecutionPhase.REQUEST);
        when(policy1.onRequest(ctx)).thenAnswer(invocation -> ctx.interruptWith(new ExecutionFailure(400).message(MOCK_ERROR_MESSAGE)));

        cut.execute(ctx).test().assertFailure(InterruptionFailureException.class);

        verify(policy1).onRequest(ctx);
        verifyNoMoreInteractions(policy2);
    }

    @Test
    public void should_error_when_policy_error() {
        final HttpPolicy policy1 = mock(HttpPolicy.class);
        final HttpPolicy policy2 = mock(HttpPolicy.class);
        final HttpExecutionContext ctx = new DefaultExecutionContext(null, response).componentProvider(componentProvider).metrics(metrics);

        final HttpPolicyChain cut = new HttpPolicyChain(CHAIN_ID, asList(policy1, policy2), ExecutionPhase.REQUEST);
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
