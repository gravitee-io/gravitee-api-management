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
package io.gravitee.gateway.reactive.handlers.api.v4;

import static io.gravitee.common.component.Lifecycle.State.STOPPED;
import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.REPORTERS_LOGGING_EXCLUDED_RESPONSE_TYPES_PROPERTY;
import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.REPORTERS_LOGGING_MAX_SIZE_PROPERTY;
import static io.gravitee.gateway.reactive.handlers.api.v4.DefaultApiReactor.PENDING_REQUESTS_TIMEOUT_PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.connector.entrypoint.BaseEntrypointConnector;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.tcp.TcpExecutionContext;
import io.gravitee.gateway.reactive.core.context.DefaultDeploymentContext;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.core.v4.entrypoint.DefaultEntrypointConnectorResolver;
import io.gravitee.gateway.reactive.core.v4.invoker.TcpEndpointInvoker;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.gravitee.gateway.reactor.handler.TcpAcceptor;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class TcpApiReactorTest {

    public static final String API_ID = "api-id";
    public static final String ORGANIZATION_ID = "organization-id";
    public static final String ENVIRONMENT_ID = "environment-id";

    @Spy
    Completable spyEntrypointRequest = Completable.complete();

    @Spy
    Completable spyEntrypointResponse = Completable.complete();

    @Spy
    Completable spyInvokerChain = Completable.complete();

    @Spy
    Completable spyResponseEnd = Completable.complete();

    @Mock
    Configuration configuration;

    @Mock
    Node node;

    @Mock
    private Api api;

    @Mock
    private io.gravitee.definition.model.v4.Api apiDefinition;

    @Mock
    private DefaultEntrypointConnectorResolver entrypointConnectorResolver;

    @Mock
    private EntrypointConnectorPluginManager entrypointConnectorPluginManager;

    @Mock
    private EndpointManager endpointManager;

    @Mock
    private TcpEndpointInvoker defaultInvoker;

    @Mock
    private MutableExecutionContext ctx;

    @Mock
    private MutableResponse response;

    @Mock
    private BaseEntrypointConnector entrypointConnector;

    @Mock
    private RequestTimeoutConfiguration requestTimeoutConfiguration;

    private TestScheduler testScheduler;

    private TracingContext tracingContext = TracingContext.noop();

    private TcpApiReactor cut;

    @BeforeEach
    public void init() throws Exception {
        lenient().when(response.end(any())).thenReturn(spyResponseEnd);
        lenient().when(ctx.response()).thenReturn(response);

        lenient().when(api.getDefinition()).thenReturn(apiDefinition);
        lenient().when(api.getId()).thenReturn(API_ID);
        lenient().when(api.getName()).thenReturn(API_ID);
        lenient().when(api.getDeployedAt()).thenReturn(new Date());
        lenient().when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);
        lenient().when(api.getEnvironmentId()).thenReturn(ENVIRONMENT_ID);
        lenient().when(apiDefinition.getType()).thenReturn(io.gravitee.definition.model.v4.ApiType.PROXY);

        lenient().when(defaultInvoker.invoke(any(TcpExecutionContext.class))).thenReturn(spyInvokerChain);
        lenient().when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_INVOKER)).thenReturn(defaultInvoker);
        lenient().when(entrypointConnector.handleRequest(ctx)).thenReturn(spyEntrypointRequest);
        lenient().when(entrypointConnector.handleResponse(ctx)).thenReturn(spyEntrypointResponse);

        lenient().when(entrypointConnectorResolver.resolve(ctx)).thenReturn(entrypointConnector);
        lenient()
            .when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR))
            .thenReturn(entrypointConnector);
        lenient().when(entrypointConnector.supportedApi()).thenReturn(ApiType.PROXY);

        when(configuration.getProperty(REPORTERS_LOGGING_EXCLUDED_RESPONSE_TYPES_PROPERTY, String.class, null)).thenReturn(null);
        when(configuration.getProperty(REPORTERS_LOGGING_MAX_SIZE_PROPERTY, String.class, null)).thenReturn(null);
        when(configuration.getProperty(PENDING_REQUESTS_TIMEOUT_PROPERTY, Long.class, 10_000L)).thenReturn(10_000L);

        lenient().when(requestTimeoutConfiguration.getRequestTimeout()).thenReturn(0L);
        lenient().when(requestTimeoutConfiguration.getRequestTimeoutGraceDelay()).thenReturn(10000L);

        cut = buildApiReactor();

        // make sure that Services that inherit from Lifecycle have been started after the ApiReactor start cal
        verify(endpointManager).start();

        testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler(s -> testScheduler);
    }

    private TcpApiReactor buildApiReactor() {
        TcpApiReactor tcpApiReactor = null;
        try {
            tcpApiReactor = new TcpApiReactor(
                api,
                node,
                configuration,
                new DefaultDeploymentContext(),
                entrypointConnectorPluginManager,
                endpointManager,
                requestTimeoutConfiguration,
                tracingContext
            );
            ReflectionTestUtils.setField(tcpApiReactor, "entrypointConnectorResolver", entrypointConnectorResolver);
            ReflectionTestUtils.setField(tcpApiReactor, "defaultInvoker", defaultInvoker);
            tcpApiReactor.doStart();
        } catch (Exception e) {
            fail(e);
        }
        return tcpApiReactor;
    }

    @Test
    void should_prepare_common_attributes() {
        cut.handle(ctx).test().assertComplete();

        verify(ctx).setAttribute(ContextAttributes.ATTR_API, API_ID);
        verify(ctx).setAttribute(ContextAttributes.ATTR_API_NAME, API_ID);
        verify(ctx).setAttribute(ContextAttributes.ATTR_API_DEPLOYED_AT, api.getDeployedAt().getTime());
        verify(ctx).setAttribute(ContextAttributes.ATTR_ORGANIZATION, ORGANIZATION_ID);
        verify(ctx).setAttribute(ContextAttributes.ATTR_ENVIRONMENT, ENVIRONMENT_ID);
    }

    @Test
    void should_return_tcp_acceptors() {
        TcpListener tcpListener = new TcpListener();
        tcpListener.setHosts(List.of("myhost"));
        tcpListener.setEntrypoints(List.of(new Entrypoint()));
        when(apiDefinition.getListeners()).thenReturn(List.of(tcpListener));

        cut = buildApiReactor();
        List<Acceptor<?>> acceptors = cut.acceptors();
        assertThat(acceptors).hasSize(1);
        Acceptor<?> acceptor1 = acceptors.get(0);
        assertThat(acceptor1).isInstanceOf(TcpAcceptor.class);
        TcpAcceptor tcpAcceptor = (TcpAcceptor) acceptor1;
        assertThat(tcpAcceptor.host()).isEqualTo("myhost");
    }

    @Test
    void should_execute_request_and_response_phases() {
        cut.handle(ctx).test().assertComplete();

        // Verify flow chain has been executed in the right order
        InOrder inOrder = getInOrder();

        inOrder.verify(spyEntrypointRequest).subscribe(any(CompletableObserver.class));
        inOrder.verify(spyInvokerChain).subscribe(any(CompletableObserver.class));
        inOrder.verify(spyEntrypointResponse).subscribe(any(CompletableObserver.class));
        inOrder.verify(spyResponseEnd).subscribe(any(CompletableObserver.class));
    }

    @Test
    void should_wait_for_pending_request_before_stopping() throws Exception {
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> testScheduler);
        when(node.lifecycleState()).thenReturn(Lifecycle.State.STARTED);
        final AtomicLong pendingRequests = new AtomicLong(1);
        ReflectionTestUtils.setField(cut, "pendingRequests", pendingRequests);

        cut.doStop();

        // Pre-stop should have been called
        verify(entrypointConnectorResolver).preStop();
        verify(endpointManager).preStop();

        testScheduler.advanceTimeBy(2500L, TimeUnit.MILLISECONDS);
        testScheduler.triggerActions();

        // Not called yet as there is still a pending request and timeout has not expired.
        verify(entrypointConnectorResolver, times(0)).stop();
        verify(endpointManager, times(0)).stop();

        // Ends the pending request.
        pendingRequests.decrementAndGet();
        testScheduler.advanceTimeBy(100L, TimeUnit.MILLISECONDS);
        testScheduler.triggerActions();

        verify(entrypointConnectorResolver).stop();
        verify(endpointManager).stop();
        RxJavaPlugins.reset();
    }

    @Test
    void should_wait_for_pending_request_and_force_stop_after10s_when_request_does_not_finish() throws Exception {
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> testScheduler);
        when(node.lifecycleState()).thenReturn(Lifecycle.State.STARTED);
        final AtomicLong pendingRequests = new AtomicLong(1);
        ReflectionTestUtils.setField(cut, "pendingRequests", pendingRequests);
        cut.doStop();

        // Pre-stop should have been called
        verify(entrypointConnectorResolver).preStop();
        verify(endpointManager).preStop();

        for (int i = 0; i < 99; i++) {
            testScheduler.advanceTimeBy(100L, TimeUnit.MILLISECONDS);
            testScheduler.triggerActions();

            // Not called yet as there is still a pending request and timeout has not expired.
            verify(entrypointConnectorResolver, times(0)).stop();
            verify(endpointManager, times(0)).stop();
        }

        testScheduler.advanceTimeBy(100L, TimeUnit.MILLISECONDS);
        testScheduler.triggerActions();

        verify(entrypointConnectorResolver).stop();
        verify(endpointManager).stop();

        assertEquals(STOPPED, cut.lifecycleState());
        RxJavaPlugins.reset();
    }

    @Test
    void should_not_wait_for_pending_request_when_node_is_shutdown() throws Exception {
        when(node.lifecycleState()).thenReturn(STOPPED);
        final AtomicLong pendingRequests = new AtomicLong(1);
        ReflectionTestUtils.setField(cut, "pendingRequests", pendingRequests);
        cut.doStop();

        verify(entrypointConnectorResolver).preStop();
        verify(entrypointConnectorResolver).stop();
        verify(endpointManager).preStop();
        verify(endpointManager).stop();
    }

    @Test
    void should_ignore_error_and_continue_when_error_occurred_during_stop() throws Exception {
        when(node.lifecycleState()).thenReturn(STOPPED);
        when(entrypointConnectorResolver.stop()).thenThrow(new RuntimeException("Mock exception"));
        cut.stop();

        verify(entrypointConnectorResolver).preStop();
        verify(entrypointConnectorResolver).stop();
        verify(endpointManager).preStop();
        verify(endpointManager, never()).stop();
    }

    private InOrder getInOrder() {
        return inOrder(spyEntrypointRequest, spyInvokerChain, spyEntrypointResponse, spyResponseEnd);
    }
}
