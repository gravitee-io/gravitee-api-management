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

import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.context.HttpRequestInternal;
import io.gravitee.gateway.reactive.core.context.HttpResponseInternal;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
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

    final HttpExecutionContextInternal executionContext = mock(HttpExecutionContextInternal.class);

    @Mock
    Function<HttpExecutionContext, Completable> onActionResponse;

    @Mock
    Function<HttpExecutionContext, Completable> onActionResponse2;

    @BeforeEach
    void setUp() {
        lenient().when(executionContext.request()).thenReturn(mock(HttpRequestInternal.class));
        lenient().when(executionContext.response()).thenReturn(mock(HttpResponseInternal.class));
    }

    @Test
    void should_execute_nothing_with_empty_policy_list() {
        // Arrange
        HttpPolicyChain httpPolicyChain = new HttpPolicyChain(CHAIN_ID, new ArrayList<>(), ExecutionPhase.REQUEST);
        final HttpExecutionContext ctx = mock(HttpExecutionContext.class);

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
        final HttpExecutionContext ctx = mock(HttpExecutionContext.class);

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
        final MutableRequest request = mock(MutableRequest.class);
        final MutableResponse response = mock(MutableResponse.class);
        final DefaultExecutionContext ctx = new DefaultExecutionContext(request, response);

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
        final MutableRequest request = mock(MutableRequest.class);
        final MutableResponse response = mock(MutableResponse.class);
        final DefaultExecutionContext ctx = new DefaultExecutionContext(request, response);

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
        final MutableRequest request = mock(MutableRequest.class);
        final MutableResponse response = mock(MutableResponse.class);
        final DefaultExecutionContext ctx = new DefaultExecutionContext(request, response);

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
        final HttpExecutionContext ctx = mock(HttpExecutionContext.class);

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
}
