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
package io.gravitee.gateway.reactive.core.failover;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.failover.Failover;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.gateway.reactive.core.v4.invoker.HttpEndpointInvoker;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Feature is functionally tested in gravitee-apim-integration-tests module
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class FailoverInvokerTest {

    private static final String API_ID = "api-id";

    @Mock
    private HttpEndpointInvoker endpointInvoker;

    @Mock
    private MutableRequest request;

    @Mock
    private MutableResponse response;

    private HttpExecutionContext executionContext;

    private FailoverInvoker cut;

    @BeforeEach
    void setUp() {
        executionContext = new DefaultExecutionContext(request, response);
        lenient().when(request.body()).thenReturn(Maybe.just(Buffer.buffer("body")));
    }

    @Test
    void should_return_id() {
        cut = new FailoverInvoker(endpointInvoker, Failover.builder().build(), API_ID);
        assertThat(cut.getId()).isEqualTo("failover-invoker");
    }

    @Test
    void should_configure_one_circuit_breaker_if_not_per_subscription() {
        cut = new FailoverInvoker(endpointInvoker, Failover.builder().perSubscription(false).build(), API_ID);
        assertThat(cut.circuitBreaker).isNotNull();
        assertThat(cut.circuitBreaker.getName()).isEqualTo(API_ID);
        assertThat(cut.circuitBreakerRegistry).isNull();
    }

    @Test
    void should_configure_circuit_breaker_registry_if_per_subscription() {
        cut = new FailoverInvoker(endpointInvoker, Failover.builder().perSubscription(true).build(), API_ID);
        assertThat(cut.circuitBreaker).isNull();
        assertThat(cut.circuitBreakerRegistry).isNotNull();
    }

    @Test
    void should_create_a_circuit_breaker_per_subscription() {
        cut =
            new FailoverInvoker(
                endpointInvoker,
                Failover.builder().slowCallDuration(50).maxRetries(2).perSubscription(true).build(),
                API_ID
            );
        when(endpointInvoker.invoke(executionContext)).thenReturn(Completable.complete());
        executionContext.setAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT, "endpoint-name");
        executionContext.setAttribute(ContextAttributes.ATTR_SUBSCRIPTION_ID, "first-subscription");

        // First call
        cut.invoke(executionContext).test().awaitDone(2, TimeUnit.SECONDS).assertComplete();

        // Second call
        executionContext.setAttribute(ContextAttributes.ATTR_SUBSCRIPTION_ID, "second-subscription");
        cut.invoke(executionContext).test().awaitDone(2, TimeUnit.SECONDS).assertComplete();

        assertThat(cut.circuitBreakerRegistry.getAllCircuitBreakers())
            .hasSize(2)
            .satisfiesExactlyInAnyOrder(
                circuitBreaker -> assertThat(circuitBreaker.getName()).isEqualTo("first-subscription"),
                circuitBreaker -> assertThat(circuitBreaker.getName()).isEqualTo("second-subscription")
            );
    }

    @Test
    void should_invoke_endpoint_invoker_with_same_target_on_each_retry() {
        cut =
            new FailoverInvoker(
                endpointInvoker,
                Failover.builder().slowCallDuration(50).maxRetries(2).perSubscription(false).build(),
                API_ID
            );
        when(endpointInvoker.invoke(executionContext)).thenReturn(Completable.complete().delay(100, TimeUnit.MILLISECONDS));
        executionContext.setAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT, "endpoint-name");
        cut
            .invoke(executionContext)
            .test()
            .awaitDone(2, TimeUnit.SECONDS)
            .assertError(t ->
                t instanceof InterruptionFailureException && ((InterruptionFailureException) t).getExecutionFailure().statusCode() == 502
            );

        ArgumentCaptor<HttpExecutionContext> executionContextArgumentCaptor = ArgumentCaptor.forClass(HttpExecutionContext.class);
        verify(endpointInvoker, times(3)).invoke(executionContextArgumentCaptor.capture());

        assertThat(executionContextArgumentCaptor.getAllValues())
            .hasSize(3)
            .allSatisfy(ctx -> assertThat(ctx.<String>getAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT)).isEqualTo("endpoint-name"));
    }

    @Test
    void should_invoke_endpoint_invoker_with_execution_failure_on_first_attempt_and_complete_on_second_attempt() {
        cut =
            new FailoverInvoker(
                endpointInvoker,
                Failover.builder().slowCallDuration(50000).maxRetries(2).perSubscription(false).build(),
                API_ID
            );
        when(endpointInvoker.invoke(executionContext))
            .thenReturn(executionContext.interruptWith(new ExecutionFailure(505)), Completable.complete());
        executionContext.setAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT, "endpoint-name");
        cut.invoke(executionContext).test().awaitDone(2, TimeUnit.SECONDS).assertComplete();

        ArgumentCaptor<HttpExecutionContext> executionContextArgumentCaptor = ArgumentCaptor.forClass(HttpExecutionContext.class);
        verify(endpointInvoker, times(2)).invoke(executionContextArgumentCaptor.capture());

        assertThat(executionContextArgumentCaptor.getValue())
            // Internal attributes should not contain ATTR_INTERNAL_EXECUTION_FAILURE as the retry executed properly
            .satisfies(ctx ->
                assertThat(ctx.<ExecutionFailure>getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE)).isNull()
            );
    }

    @Test
    void should_fail_immediately_if_no_retry() {
        cut =
            new FailoverInvoker(
                endpointInvoker,
                Failover.builder().slowCallDuration(50).maxRetries(0).perSubscription(false).build(),
                API_ID
            );
        when(endpointInvoker.invoke(executionContext)).thenReturn(Completable.complete().delay(100, TimeUnit.MILLISECONDS));
        executionContext.setAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT, "endpoint-name");
        cut
            .invoke(executionContext)
            .test()
            .awaitDone(2, TimeUnit.SECONDS)
            .assertError(t ->
                t instanceof InterruptionFailureException && ((InterruptionFailureException) t).getExecutionFailure().statusCode() == 502
            );

        ArgumentCaptor<HttpExecutionContext> executionContextArgumentCaptor = ArgumentCaptor.forClass(HttpExecutionContext.class);
        verify(endpointInvoker, times(1)).invoke(executionContextArgumentCaptor.capture());

        assertThat(executionContextArgumentCaptor.getAllValues())
            .hasSize(1)
            .allSatisfy(ctx -> assertThat(ctx.<String>getAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT)).isEqualTo("endpoint-name"));
    }
}
