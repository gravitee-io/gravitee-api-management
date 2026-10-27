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

import static io.gravitee.gateway.reactive.core.v4.invoker.HttpEndpointInvoker.ATTR_INTERNAL_FAILOVER_ENDPOINT_NAME;
import static io.gravitee.gateway.reactive.core.v4.invoker.HttpEndpointInvoker.ATTR_INTERNAL_FAILOVER_MANAGED_ENDPOINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.failover.Failover;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.core.context.AbstractRequest;
import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint;
import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpointGroup;
import io.gravitee.gateway.reactive.core.v4.invoker.HttpEndpointInvoker;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
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
    private MutableResponse response;

    @Mock
    private Metrics metrics;

    @Mock
    private EndpointManager endpointManager;

    private MutableRequest request;

    private HttpExecutionContext executionContext;

    private FailoverInvoker cut;

    private static MutableRequest fakeRequest(Buffer body) {
        var request = new AbstractRequest() {
            {
                this.headers = HttpHeaders.create();
            }
        };
        request.body(body);
        return request;
    }

    @BeforeEach
    void setUp() {
        request = fakeRequest(Buffer.buffer("body"));
        executionContext = new DefaultExecutionContext(request, response);
        ((DefaultExecutionContext) executionContext).metrics(metrics);
    }

    @Test
    void should_return_id() {
        cut = new FailoverInvoker(endpointInvoker, Failover.builder().build(), API_ID, endpointManager);
        assertThat(cut.getId()).isEqualTo("failover-invoker");
    }

    @Test
    void should_configure_one_circuit_breaker_if_not_per_subscription() {
        cut = new FailoverInvoker(endpointInvoker, Failover.builder().perSubscription(false).build(), API_ID, endpointManager);
        assertThat(cut.circuitBreaker).isNotNull();
        assertThat(cut.circuitBreaker.getName()).isEqualTo(API_ID);
        assertThat(cut.circuitBreakerRegistry).isNull();
    }

    @Test
    void should_configure_circuit_breaker_registry_if_per_subscription() {
        cut = new FailoverInvoker(endpointInvoker, Failover.builder().perSubscription(true).build(), API_ID, endpointManager);
        assertThat(cut.circuitBreaker).isNull();
        assertThat(cut.circuitBreakerRegistry).isNotNull();
    }

    @Test
    void should_create_a_circuit_breaker_per_subscription() {
        cut = new FailoverInvoker(
            endpointInvoker,
            Failover.builder().slowCallDuration(50).maxRetries(2).perSubscription(true).build(),
            API_ID,
            endpointManager
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
        cut = new FailoverInvoker(
            endpointInvoker,
            Failover.builder().slowCallDuration(50).maxRetries(2).perSubscription(false).build(),
            API_ID,
            endpointManager
        );
        when(endpointInvoker.invoke(executionContext)).thenReturn(Completable.complete().delay(100, TimeUnit.MILLISECONDS));
        executionContext.setAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT, "endpoint-name");
        cut
            .invoke(executionContext)
            .test()
            .awaitDone(2, TimeUnit.SECONDS)
            .assertError(
                t -> t instanceof InterruptionFailureException interruption && interruption.getExecutionFailure().statusCode() == 502
            );

        ArgumentCaptor<HttpExecutionContext> executionContextArgumentCaptor = ArgumentCaptor.forClass(HttpExecutionContext.class);
        verify(endpointInvoker, times(3)).invoke(executionContextArgumentCaptor.capture());

        assertThat(executionContextArgumentCaptor.getAllValues())
            .hasSize(3)
            .allSatisfy(ctx -> assertThat(ctx.<String>getAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT)).isEqualTo("endpoint-name"));
    }

    @Test
    void should_invoke_endpoint_invoker_with_execution_failure_on_first_attempt_and_complete_on_second_attempt() {
        cut = new FailoverInvoker(
            endpointInvoker,
            Failover.builder().slowCallDuration(50000).maxRetries(2).perSubscription(false).build(),
            API_ID,
            endpointManager
        );
        when(endpointInvoker.invoke(executionContext)).thenReturn(
            executionContext.interruptWith(new ExecutionFailure(505)),
            Completable.complete()
        );
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
        cut = new FailoverInvoker(
            endpointInvoker,
            Failover.builder().slowCallDuration(50).maxRetries(0).perSubscription(false).build(),
            API_ID,
            endpointManager
        );
        when(endpointInvoker.invoke(executionContext)).thenReturn(Completable.complete().delay(100, TimeUnit.MILLISECONDS));
        executionContext.setAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT, "endpoint-name");
        cut
            .invoke(executionContext)
            .test()
            .awaitDone(2, TimeUnit.SECONDS)
            .assertError(
                t ->
                    t instanceof InterruptionFailureException &&
                    ((InterruptionFailureException) t).getExecutionFailure().statusCode() == 502
            );

        ArgumentCaptor<HttpExecutionContext> executionContextArgumentCaptor = ArgumentCaptor.forClass(HttpExecutionContext.class);
        verify(endpointInvoker, times(1)).invoke(executionContextArgumentCaptor.capture());

        assertThat(executionContextArgumentCaptor.getAllValues())
            .hasSize(1)
            .allSatisfy(ctx -> assertThat(ctx.<String>getAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT)).isEqualTo("endpoint-name"));
    }

    @Nested
    class ForceNextEndpoint {

        @BeforeEach
        void allow_context_logging() {
            ((DefaultExecutionContext) executionContext).componentProvider(loggingComponentProvider());
        }

        @Test
        void should_keep_the_request_path_available_when_a_connection_error_switches_endpoint() {
            request.pathInfo("/items/42");
            selectEndpoint(executionContext, "primary", "primary", "secondary");
            cut = failover(null, 2);

            List<Attempt> attempts = new ArrayList<>();
            AtomicInteger calls = new AtomicInteger();
            when(endpointInvoker.invoke(executionContext)).thenAnswer(invocation -> {
                attempts.add(attempt(executionContext));
                if (calls.getAndIncrement() < 2) {
                    // Connector rewrite happens during connect, after failover has chosen the endpoint.
                    request.pathInfo("/service");
                    return Completable.error(new IOException("connection reset"));
                }
                return Completable.complete();
            });

            cut.invoke(executionContext).test().awaitDone(2, TimeUnit.SECONDS).assertComplete();

            assertThat(attempts).containsExactly(
                new Attempt(null, null, "/items/42"),
                new Attempt(null, "secondary", "/items/42"),
                new Attempt(null, "primary", "/items/42")
            );
        }

        @Test
        void should_keep_a_colon_in_the_request_path_when_switching_endpoint() {
            request.pathInfo("/openai/deployments/gpt-4o/chat/completions:stream");
            selectEndpoint(executionContext, "primary", "primary", "secondary");
            cut = failover(null, 1);

            List<Attempt> attempts = recordAttemptsUntilSuccess();

            cut.invoke(executionContext).test().awaitDone(2, TimeUnit.SECONDS).assertComplete();

            assertThat(attempts).containsExactly(
                new Attempt(null, null, "/openai/deployments/gpt-4o/chat/completions:stream"),
                new Attempt(null, "secondary", "/openai/deployments/gpt-4o/chat/completions:stream")
            );
        }

        @Test
        void should_leave_the_request_path_unset_when_path_info_is_empty() {
            selectEndpoint(executionContext, "primary", "primary", "secondary");
            cut = failover(null, 1);

            List<Attempt> attempts = recordAttemptsUntilSuccess();

            cut.invoke(executionContext).test().awaitDone(2, TimeUnit.SECONDS).assertComplete();

            assertThat(attempts).containsExactly(new Attempt(null, null, null), new Attempt(null, "secondary", null));
        }

        @Test
        void should_keep_the_request_path_available_when_the_failure_condition_matches() {
            request.pathInfo("/items/42");
            TemplateEngine templateEngine = mock(TemplateEngine.class);
            when(templateEngine.eval("{#response.status >= 500}", Boolean.class)).thenReturn(Maybe.just(true), Maybe.just(false));
            HttpExecutionContext ctx = executionContextWith(templateEngine);
            selectEndpoint(ctx, "primary", "primary", "secondary");
            cut = failover("{#response.status >= 500}", 1);

            List<Attempt> attempts = new ArrayList<>();
            AtomicInteger calls = new AtomicInteger();
            when(endpointInvoker.invoke(ctx)).thenAnswer(invocation -> {
                attempts.add(attempt(ctx));
                if (calls.getAndIncrement() == 0) {
                    request.pathInfo("/service");
                }
                return Completable.complete();
            });

            cut.invoke(ctx).test().awaitDone(2, TimeUnit.SECONDS).assertComplete();

            assertThat(attempts).containsExactly(new Attempt(null, null, "/items/42"), new Attempt(null, "secondary", "/items/42"));
        }

        @Test
        void should_leave_a_dynamic_routing_override_when_switching_endpoint() {
            executionContext.setAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT, "primary:/routed");
            request.pathInfo("/items/42");
            selectEndpoint(executionContext, "primary", "primary", "secondary");
            cut = failover(null, 1);

            List<Attempt> attempts = recordAttemptsUntilSuccess();

            cut.invoke(executionContext).test().awaitDone(2, TimeUnit.SECONDS).assertComplete();

            assertThat(attempts).containsExactly(
                new Attempt("primary:/routed", null, "/items/42"),
                new Attempt("primary:/routed", "secondary", "/items/42")
            );
        }

        @Test
        void should_leave_an_endpoint_reference_when_switching_endpoint() {
            executionContext.setAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT, "primary:");
            request.pathInfo("/items/42");
            selectEndpoint(executionContext, "primary", "primary", "secondary");
            cut = failover(null, 1);

            List<Attempt> attempts = recordAttemptsUntilSuccess();

            cut.invoke(executionContext).test().awaitDone(2, TimeUnit.SECONDS).assertComplete();

            assertThat(attempts).containsExactly(
                new Attempt("primary:", null, "/items/42"),
                new Attempt("primary:", "secondary", "/items/42")
            );
        }

        @Test
        void should_leave_an_override_template_when_switching_endpoint() {
            String template = "{#request.headers['x'] != null ? #endpoints['a'] : #endpoints['b']}/routed";
            executionContext.setAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT, template);
            request.pathInfo("/items/42");
            selectEndpoint(executionContext, "primary", "primary", "secondary");
            cut = failover(null, 1);

            List<Attempt> attempts = recordAttemptsUntilSuccess();

            cut.invoke(executionContext).test().awaitDone(2, TimeUnit.SECONDS).assertComplete();

            assertThat(attempts).containsExactly(new Attempt(template, null, "/items/42"), new Attempt(template, "secondary", "/items/42"));
        }

        private record Attempt(String endpointAttribute, String failoverEndpointName, String pathInfo) {}

        private Attempt attempt(HttpExecutionContext ctx) {
            return new Attempt(
                ctx.getAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT),
                ctx.getInternalAttribute(ATTR_INTERNAL_FAILOVER_ENDPOINT_NAME),
                ctx.request().pathInfo()
            );
        }

        private List<Attempt> recordAttemptsUntilSuccess() {
            List<Attempt> attempts = new ArrayList<>();
            AtomicInteger calls = new AtomicInteger();
            when(endpointInvoker.invoke(executionContext)).thenAnswer(invocation -> {
                attempts.add(attempt(executionContext));
                if (calls.getAndIncrement() == 0) {
                    return Completable.error(new IOException("connection reset"));
                }
                return Completable.complete();
            });
            return attempts;
        }

        private FailoverInvoker failover(String failureCondition, int maxRetries) {
            return new FailoverInvoker(
                endpointInvoker,
                Failover.builder()
                    .failureCondition(failureCondition)
                    .forceNextEndpointOnFailure(true)
                    .slowCallDuration(50_000)
                    .maxRetries(maxRetries)
                    .perSubscription(false)
                    .build(),
                API_ID,
                endpointManager
            );
        }

        private void selectEndpoint(HttpExecutionContext ctx, String selectedName, String... names) {
            List<Endpoint> endpoints = new ArrayList<>();
            Endpoint selectedDefinition = null;
            for (String name : names) {
                Endpoint endpoint = Endpoint.builder().name(name).type("http-proxy").build();
                endpoints.add(endpoint);
                if (name.equals(selectedName)) {
                    selectedDefinition = endpoint;
                }
            }
            EndpointGroup group = EndpointGroup.builder().name("default").type("http-proxy").endpoints(endpoints).build();
            ManagedEndpointGroup managedGroup = mock(ManagedEndpointGroup.class);
            when(managedGroup.getDefinition()).thenReturn(group);
            ManagedEndpoint selected = mock(ManagedEndpoint.class);
            when(selected.getDefinition()).thenReturn(selectedDefinition);
            when(selected.getGroup()).thenReturn(managedGroup);
            ctx.setInternalAttribute(ATTR_INTERNAL_FAILOVER_MANAGED_ENDPOINT, selected);
        }

        private HttpExecutionContext executionContextWith(TemplateEngine templateEngine) {
            DefaultExecutionContext real = new DefaultExecutionContext(request, response);
            real.metrics(metrics);
            real.componentProvider(loggingComponentProvider());
            DefaultExecutionContext ctx = spy(real);
            doReturn(templateEngine).when(ctx).getTemplateEngine();
            doReturn(metrics).when(ctx).metrics();
            return ctx;
        }

        private ComponentProvider loggingComponentProvider() {
            ComponentProvider provider = mock(ComponentProvider.class);
            lenient().when(provider.getComponent(io.gravitee.node.api.Node.class)).thenReturn(mock(io.gravitee.node.api.Node.class));
            return provider;
        }
    }

    @Nested
    class FailureConditionEvaluation {

        private static final String CONDITION = "{#response.status >= 500}";

        @Mock
        private TemplateEngine templateEngine;

        @Mock
        private HttpExecutionContext mockCtx;

        @BeforeEach
        void setUpConditionTests() {
            lenient().when(mockCtx.getAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT)).thenReturn("endpoint-name");
            lenient().when(mockCtx.request()).thenReturn(fakeRequest(Buffer.buffer("body")));
            lenient().when(mockCtx.getTemplateEngine()).thenReturn(templateEngine);
            lenient()
                .when(mockCtx.interruptWith(any()))
                .thenAnswer(invocation -> {
                    ExecutionFailure failure = invocation.getArgument(0);
                    return Completable.error(new InterruptionFailureException(failure));
                });
        }

        @Test
        void should_retry_when_failure_condition_matches() {
            // Given
            cut = new FailoverInvoker(
                endpointInvoker,
                Failover.builder().failureCondition(CONDITION).slowCallDuration(50000).maxRetries(2).perSubscription(false).build(),
                API_ID,
                endpointManager
            );
            when(templateEngine.eval(CONDITION, Boolean.class)).thenReturn(Maybe.just(true));
            when(endpointInvoker.invoke(mockCtx)).thenReturn(Completable.complete());

            // When - condition always true → retries exhausted → 502
            cut
                .invoke(mockCtx)
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertError(
                    t ->
                        t instanceof InterruptionFailureException &&
                        ((InterruptionFailureException) t).getExecutionFailure().statusCode() == 502
                );

            // Then - 1 initial + 2 retries = 3 invocations
            verify(endpointInvoker, times(3)).invoke(mockCtx);
        }

        @Test
        void should_not_retry_when_condition_does_not_match() {
            // Given
            cut = new FailoverInvoker(
                endpointInvoker,
                Failover.builder().failureCondition(CONDITION).slowCallDuration(50000).maxRetries(2).perSubscription(false).build(),
                API_ID,
                endpointManager
            );
            when(templateEngine.eval(CONDITION, Boolean.class)).thenReturn(Maybe.just(false));
            when(endpointInvoker.invoke(mockCtx)).thenReturn(Completable.complete());

            // When
            cut.invoke(mockCtx).test().awaitDone(2, TimeUnit.SECONDS).assertComplete();

            // Then
            verify(endpointInvoker, times(1)).invoke(mockCtx);
        }

        @Test
        void should_not_evaluate_when_no_condition_configured() {
            // Given
            cut = new FailoverInvoker(
                endpointInvoker,
                Failover.builder().slowCallDuration(50000).maxRetries(2).perSubscription(false).build(),
                API_ID,
                endpointManager
            );
            when(endpointInvoker.invoke(mockCtx)).thenReturn(Completable.complete());

            // When
            cut.invoke(mockCtx).test().awaitDone(2, TimeUnit.SECONDS).assertComplete();

            // Then
            verify(endpointInvoker, times(1)).invoke(mockCtx);
            verify(templateEngine, times(0)).eval(CONDITION, Boolean.class);
        }

        @Test
        void should_ignore_el_evaluation_error() {
            // Given
            cut = new FailoverInvoker(
                endpointInvoker,
                Failover.builder().failureCondition(CONDITION).slowCallDuration(50000).maxRetries(2).perSubscription(false).build(),
                API_ID,
                endpointManager
            );
            when(templateEngine.eval(CONDITION, Boolean.class)).thenReturn(Maybe.error(new RuntimeException("EL evaluation error")));
            when(endpointInvoker.invoke(mockCtx)).thenReturn(Completable.complete());

            // When
            cut.invoke(mockCtx).test().awaitDone(2, TimeUnit.SECONDS).assertComplete();

            // Then - no retry, error is silently ignored
            verify(endpointInvoker, times(1)).invoke(mockCtx);
        }
    }
}
