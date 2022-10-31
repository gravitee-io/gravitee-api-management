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
package io.gravitee.gateway.jupiter.handlers.api.v4;

import static io.gravitee.common.component.Lifecycle.State.STOPPED;
import static io.gravitee.common.http.HttpStatusCode.UNAUTHORIZED_401;
import static io.gravitee.gateway.api.ExecutionContext.ATTR_INVOKER;
import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.PENDING_REQUESTS_TIMEOUT_PROPERTY;
import static io.gravitee.gateway.jupiter.api.ExecutionPhase.MESSAGE_RESPONSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.gateway.core.component.CompositeComponentProvider;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ContextAttributes;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.api.invoker.Invoker;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionException;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionFailureException;
import io.gravitee.gateway.jupiter.core.processor.ProcessorChain;
import io.gravitee.gateway.jupiter.core.v4.entrypoint.DefaultEntrypointConnectorResolver;
import io.gravitee.gateway.jupiter.core.v4.invoker.EndpointInvoker;
import io.gravitee.gateway.jupiter.handlers.api.v4.flow.FlowChainFactory;
import io.gravitee.gateway.jupiter.handlers.api.v4.processor.ApiMessageProcessorChainFactory;
import io.gravitee.gateway.jupiter.handlers.api.v4.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.jupiter.handlers.api.v4.security.SecurityChain;
import io.gravitee.gateway.jupiter.policy.PolicyManager;
import io.gravitee.gateway.jupiter.reactor.v4.subscription.SubscriptionAcceptor;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.gravitee.gateway.reactor.handler.HttpAcceptor;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.gravitee.reporter.api.http.Metrics;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AsyncApiReactorTest {

    public static final String CONTEXT_PATH = "context-path";
    public static final String API_ID = "api-id";
    public static final String ORGANIZATION_ID = "organization-id";
    public static final String ENVIRONMENT_ID = "environment-id";

    @Spy
    Completable spyRequestPlatformFlowChain = Completable.complete();

    @Spy
    Completable spyMessageRequestPlatformFlowChain = Completable.complete();

    @Spy
    Completable spyResponsePlatformFlowChain = Completable.complete();

    @Spy
    Completable spyMessageResponsePlatformFlowChain = Completable.complete();

    @Spy
    Completable spyRequestPlanFlowChain = Completable.complete();

    @Spy
    Completable spyMessageRequestPlanFlowChain = Completable.complete();

    @Spy
    Completable spyResponsePlanFlowChain = Completable.complete();

    @Spy
    Completable spyMessageResponsePlanFlowChain = Completable.complete();

    @Spy
    Completable spyRequestApiFlowChain = Completable.complete();

    @Spy
    Completable spyMessageRequestApiFlowChain = Completable.complete();

    @Spy
    Completable spyResponseApiFlowChain = Completable.complete();

    @Spy
    Completable spyMessageResponseApiFlowChain = Completable.complete();

    @Spy
    Completable spyEntrypointRequest = Completable.complete();

    @Spy
    Completable spyEntrypointResponse = Completable.complete();

    @Spy
    Completable spyApiPreProcessorChain = Completable.complete();

    @Spy
    Completable spyApiPostProcessorChain = Completable.complete();

    @Spy
    Completable spyApiErrorProcessorChain = Completable.complete();

    @Spy
    Completable spyApiMessageProcessorChain = Completable.complete();

    @Spy
    Completable spyInvokerChain = Completable.complete();

    @Spy
    Completable spyInterruptionFailureException = Completable.error(new InterruptionFailureException(mock(ExecutionFailure.class)));

    @Spy
    Completable spyInterruptionException = Completable.error(new InterruptionException());

    @Spy
    Completable spySecurityChain = Completable.complete();

    @Spy
    Completable spyInterruptSecurityChain = Completable.error(new InterruptionFailureException(new ExecutionFailure(UNAUTHORIZED_401)));

    @Spy
    ResourceLifecycleManager resourceLifecycleManager;

    @Mock
    Configuration configuration;

    @Mock
    Node node;

    @Mock
    private Api api;

    @Mock
    private io.gravitee.definition.model.v4.Api apiDefinition;

    @Mock
    private CompositeComponentProvider apiComponentProvider;

    @Mock
    private PolicyManager policyManager;

    @Mock
    private DefaultEntrypointConnectorResolver asyncEntrypointResolver;

    @Mock
    private EntrypointConnectorPluginManager entrypointConnectorPluginManager;

    @Mock
    private EndpointConnectorPluginManager endpointConnectorPluginManager;

    @Mock
    private EndpointInvoker defaultInvoker;

    @Mock
    private io.gravitee.gateway.jupiter.handlers.api.flow.FlowChainFactory flowChainFactory;

    @Mock
    private FlowChainFactory v4FlowChainFactory;

    @Mock
    private ApiProcessorChainFactory apiProcessorChainFactory;

    @Mock
    private ProcessorChain apiPreProcessorChain;

    @Mock
    private ProcessorChain apiPostProcessorChain;

    @Mock
    private ProcessorChain apiErrorProcessorChain;

    @Mock
    private ApiMessageProcessorChainFactory apiMessageProcessorChainFactory;

    @Mock
    private ProcessorChain apiMessageProcessorChain;

    @Mock
    private MutableExecutionContext ctx;

    @Mock
    private MutableRequest request;

    @Mock
    private MutableResponse response;

    @Mock
    private EntrypointAsyncConnector entrypointConnector;

    @Mock
    private io.gravitee.gateway.jupiter.handlers.api.flow.FlowChain platformFlowChain;

    @Mock
    private io.gravitee.gateway.jupiter.handlers.api.v4.flow.FlowChain apiPlanFlowChain;

    @Mock
    private io.gravitee.gateway.jupiter.handlers.api.v4.flow.FlowChain apiFlowChain;

    @Mock
    private SecurityChain securityChain;

    private TestScheduler testScheduler;

    private AsyncApiReactor cut;

    @BeforeEach
    public void init() throws Exception {
        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(ctx.response()).thenReturn(response);
        lenient().when(ctx.getAttribute(ATTR_INVOKER)).thenReturn(defaultInvoker);
        lenient().when(request.contextPath()).thenReturn("/contextPath");
        lenient().when(request.metrics()).thenReturn(mock(Metrics.class));
        lenient().when(response.end()).thenReturn(Completable.complete());
        lenient().when(ctx.response()).thenReturn(response);
        lenient().when(ctx.componentProvider(any())).thenReturn(ctx);
        lenient().when(ctx.templateVariableProviders(any())).thenReturn(ctx);
        lenient().when(ctx.interruptWith(any())).thenReturn(spyInterruptionFailureException);
        lenient().when(ctx.interrupt()).thenReturn(spyInterruptionException);

        lenient().when(request.contextPath()).thenReturn(CONTEXT_PATH);
        lenient().when(api.getDefinition()).thenReturn(apiDefinition);
        lenient().when(api.getId()).thenReturn(API_ID);
        lenient().when(api.getDeployedAt()).thenReturn(new Date());
        lenient().when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);
        lenient().when(api.getEnvironmentId()).thenReturn(ENVIRONMENT_ID);
        lenient().when(apiProcessorChainFactory.preProcessorChain(api)).thenReturn(apiPreProcessorChain);
        lenient().when(apiProcessorChainFactory.postProcessorChain(api)).thenReturn(apiPostProcessorChain);
        lenient().when(apiProcessorChainFactory.errorProcessorChain(api)).thenReturn(apiErrorProcessorChain);
        lenient().when(apiMessageProcessorChainFactory.messageProcessorChain(api)).thenReturn(apiMessageProcessorChain);

        lenient().when(flowChainFactory.createPlatformFlow(api)).thenReturn(platformFlowChain);
        lenient().when(platformFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestPlatformFlowChain);
        lenient().when(platformFlowChain.execute(ctx, ExecutionPhase.MESSAGE_REQUEST)).thenReturn(spyMessageRequestPlatformFlowChain);
        lenient().when(platformFlowChain.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyResponsePlatformFlowChain);
        lenient().when(platformFlowChain.execute(ctx, ExecutionPhase.MESSAGE_RESPONSE)).thenReturn(spyMessageResponsePlatformFlowChain);
        lenient().when(securityChain.execute(any())).thenReturn(spySecurityChain);
        lenient().when(v4FlowChainFactory.createPlanFlow(api)).thenReturn(apiPlanFlowChain);
        lenient().when(apiPlanFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestPlanFlowChain);
        lenient().when(apiPlanFlowChain.execute(ctx, ExecutionPhase.MESSAGE_REQUEST)).thenReturn(spyMessageRequestPlanFlowChain);
        lenient().when(apiPlanFlowChain.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyResponsePlanFlowChain);
        lenient().when(apiPlanFlowChain.execute(ctx, ExecutionPhase.MESSAGE_RESPONSE)).thenReturn(spyMessageResponsePlanFlowChain);
        lenient().when(v4FlowChainFactory.createApiFlow(api)).thenReturn(apiFlowChain);
        lenient().when(apiFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiFlowChain);
        lenient().when(apiFlowChain.execute(ctx, ExecutionPhase.MESSAGE_REQUEST)).thenReturn(spyMessageRequestApiFlowChain);
        lenient().when(apiFlowChain.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyResponseApiFlowChain);
        lenient().when(apiFlowChain.execute(ctx, ExecutionPhase.MESSAGE_RESPONSE)).thenReturn(spyMessageResponseApiFlowChain);

        lenient().when(apiPreProcessorChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyApiPreProcessorChain);
        lenient().when(apiPostProcessorChain.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyApiPostProcessorChain);
        lenient().when(apiErrorProcessorChain.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyApiErrorProcessorChain);
        lenient().when(apiMessageProcessorChain.execute(ctx, MESSAGE_RESPONSE)).thenReturn(spyApiMessageProcessorChain);

        lenient().when(defaultInvoker.invoke(any(ExecutionContext.class))).thenReturn(spyInvokerChain);
        lenient().when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_INVOKER)).thenReturn(defaultInvoker);
        lenient().when(entrypointConnector.handleRequest(ctx)).thenReturn(spyEntrypointRequest);
        lenient().when(entrypointConnector.handleResponse(ctx)).thenReturn(spyEntrypointResponse);

        when(configuration.getProperty(PENDING_REQUESTS_TIMEOUT_PROPERTY, Long.class, 10_000L)).thenReturn(10_000L);

        cut =
            new AsyncApiReactor(
                api,
                apiComponentProvider,
                policyManager,
                entrypointConnectorPluginManager,
                endpointConnectorPluginManager,
                resourceLifecycleManager,
                apiProcessorChainFactory,
                apiMessageProcessorChainFactory,
                flowChainFactory,
                v4FlowChainFactory,
                configuration,
                node
            );
        ReflectionTestUtils.setField(cut, "asyncEntrypointResolver", asyncEntrypointResolver);
        ReflectionTestUtils.setField(cut, "defaultInvoker", defaultInvoker);
        cut.doStart();
        ReflectionTestUtils.setField(cut, "securityChain", securityChain);

        testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler(s -> testScheduler);
    }

    @Test
    void shouldReturnAsyncApiType() {
        ApiType apiType = cut.apiType();
        assertThat(apiType).isEqualTo(ApiType.ASYNC);
    }

    @Test
    void shouldPrepareContextAttributes() {
        cut.handle(ctx).test().assertComplete();

        verify(ctx).setAttribute(ContextAttributes.ATTR_CONTEXT_PATH, CONTEXT_PATH);
        verify(ctx).setAttribute(ContextAttributes.ATTR_API, API_ID);
        verify(ctx).setAttribute(ContextAttributes.ATTR_ORGANIZATION, ORGANIZATION_ID);
        verify(ctx).setAttribute(ContextAttributes.ATTR_ENVIRONMENT, ENVIRONMENT_ID);
        verify(ctx).setInternalAttribute(ContextAttributes.ATTR_API, api);
    }

    @Test
    void shouldEndResponseWith404WhenNoEntrypoint() {
        when(response.end()).thenReturn(Completable.complete());
        when(asyncEntrypointResolver.resolve(ctx)).thenReturn(null);
        cut.handle(ctx).test().assertComplete();

        verify(ctx)
            .interruptWith(
                argThat(
                    argument -> {
                        assertThat(argument.statusCode()).isEqualTo(404);
                        assertThat(argument.message()).isEqualTo("No entrypoint matches the incoming request");
                        return true;
                    }
                )
            );
        verify(response).end();
    }

    @Test
    void shouldHandleRequest() {
        when(response.end()).thenReturn(Completable.complete());
        when(asyncEntrypointResolver.resolve(ctx)).thenReturn(entrypointConnector);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(entrypointConnector);

        cut.handle(ctx).test().assertComplete();

        // verify flow chain has been executed in the right order
        InOrder inOrder = inOrder(
            spyRequestPlatformFlowChain,
            spySecurityChain,
            spyApiPreProcessorChain,
            spyEntrypointRequest,
            spyRequestPlanFlowChain,
            spyRequestApiFlowChain,
            spyMessageRequestPlatformFlowChain,
            spyMessageRequestPlanFlowChain,
            spyMessageRequestApiFlowChain,
            spyInvokerChain,
            spyResponsePlanFlowChain,
            spyResponseApiFlowChain,
            spyMessageResponsePlanFlowChain,
            spyMessageResponseApiFlowChain,
            spyResponsePlatformFlowChain,
            spyMessageResponsePlatformFlowChain,
            spyApiMessageProcessorChain,
            spyApiPostProcessorChain,
            spyEntrypointResponse
        );
        inOrder.verify(spyRequestPlatformFlowChain).subscribe(any(CompletableObserver.class));
        inOrder.verify(spySecurityChain).subscribe(any(CompletableObserver.class));
        inOrder.verify(spyEntrypointRequest).subscribe(any(CompletableObserver.class));
        inOrder.verify(spyMessageRequestPlatformFlowChain).subscribe(any(CompletableObserver.class));
        inOrder.verify(spyApiPreProcessorChain).subscribe(any(CompletableObserver.class));
        inOrder.verify(spyRequestPlanFlowChain).subscribe(any(CompletableObserver.class));
        inOrder.verify(spyMessageRequestPlanFlowChain).subscribe(any(CompletableObserver.class));
        inOrder.verify(spyRequestApiFlowChain).subscribe(any(CompletableObserver.class));
        inOrder.verify(spyInvokerChain).subscribe(any(CompletableObserver.class));
        inOrder.verify(spyResponsePlanFlowChain).subscribe(any(CompletableObserver.class));
        inOrder.verify(spyMessageResponsePlanFlowChain).subscribe(any(CompletableObserver.class));
        inOrder.verify(spyResponseApiFlowChain).subscribe(any(CompletableObserver.class));
        inOrder.verify(spyMessageResponseApiFlowChain).subscribe(any(CompletableObserver.class));
        inOrder.verify(spyResponsePlatformFlowChain).subscribe(any(CompletableObserver.class));
        inOrder.verify(spyMessageResponsePlatformFlowChain).subscribe(any(CompletableObserver.class));
        inOrder.verify(spyApiMessageProcessorChain).subscribe(any(CompletableObserver.class));
        inOrder.verify(spyEntrypointResponse).subscribe(any(CompletableObserver.class));
        inOrder.verify(spyApiPostProcessorChain).subscribe(any(CompletableObserver.class));
    }

    @Test
    void shouldReturnHttpAcceptors() {
        HttpListener httpListener = new HttpListener();
        Path path = new Path("host", "path");
        httpListener.setPaths(List.of(path));
        SubscriptionListener subscriptionListener = new SubscriptionListener();
        when(apiDefinition.getListeners()).thenReturn(List.of(httpListener, subscriptionListener));

        List<Acceptor<?>> acceptors = cut.acceptors();
        assertThat(acceptors).hasSize(2);
        Acceptor<?> acceptor1 = acceptors.get(0);
        assertThat(acceptor1).isInstanceOf(HttpAcceptor.class);
        HttpAcceptor httpAcceptor = (HttpAcceptor) acceptor1;
        assertThat(httpAcceptor.path()).isEqualTo("path/");
        assertThat(httpAcceptor.host()).isEqualTo(path.getHost());
        Acceptor<?> acceptor2 = acceptors.get(1);
        assertThat(acceptor2).isInstanceOf(SubscriptionAcceptor.class);
        SubscriptionAcceptor subscriptionAcceptor = (SubscriptionAcceptor) acceptor2;
        assertThat(subscriptionAcceptor.apiId()).isEqualTo(api.getId());
    }

    @Test
    void shouldWaitForPendingRequestBeforeStopping() throws Exception {
        when(node.lifecycleState()).thenReturn(Lifecycle.State.STARTED);
        final AtomicLong pendingRequests = new AtomicLong(1);
        ReflectionTestUtils.setField(cut, "pendingRequests", pendingRequests);

        cut.doStop();

        // Pre-stop should have been called
        verify(asyncEntrypointResolver).preStop();
        verify(defaultInvoker).preStop();

        testScheduler.advanceTimeBy(2500L, TimeUnit.MILLISECONDS);
        testScheduler.triggerActions();

        // Not called yet as there is still a pending request and timeout has not expired.
        verify(asyncEntrypointResolver, times(0)).stop();
        verify(defaultInvoker, times(0)).stop();
        verify(resourceLifecycleManager, times(0)).stop();
        verify(policyManager, times(0)).stop();

        // Ends the pending request.
        pendingRequests.decrementAndGet();
        testScheduler.advanceTimeBy(100L, TimeUnit.MILLISECONDS);
        testScheduler.triggerActions();

        verify(asyncEntrypointResolver).stop();
        verify(defaultInvoker).stop();
        verify(resourceLifecycleManager).stop();
        verify(policyManager).stop();
    }

    @Test
    void shouldWaitForPendingRequestAndForceStopAfter10sWhenRequestDoesNotFinish() throws Exception {
        when(node.lifecycleState()).thenReturn(Lifecycle.State.STARTED);
        final AtomicLong pendingRequests = new AtomicLong(1);
        ReflectionTestUtils.setField(cut, "pendingRequests", pendingRequests);
        cut.doStop();

        // Pre-stop should have been called
        verify(asyncEntrypointResolver).preStop();
        verify(defaultInvoker).preStop();

        for (int i = 0; i < 99; i++) {
            testScheduler.advanceTimeBy(100L, TimeUnit.MILLISECONDS);
            testScheduler.triggerActions();

            // Not called yet as there is still a pending request and timeout has not expired.
            verify(asyncEntrypointResolver, times(0)).stop();
            verify(defaultInvoker, times(0)).stop();
            verify(resourceLifecycleManager, times(0)).stop();
            verify(policyManager, times(0)).stop();
        }

        testScheduler.advanceTimeBy(100L, TimeUnit.MILLISECONDS);
        testScheduler.triggerActions();

        verify(asyncEntrypointResolver).stop();
        verify(defaultInvoker).stop();
        verify(resourceLifecycleManager).stop();
        verify(policyManager).stop();

        assertEquals(STOPPED, cut.lifecycleState());
    }

    @Test
    void shouldNotWaitForPendingRequestWhenNodeIsShutdown() throws Exception {
        when(node.lifecycleState()).thenReturn(STOPPED);
        final AtomicLong pendingRequests = new AtomicLong(1);
        ReflectionTestUtils.setField(cut, "pendingRequests", pendingRequests);
        cut.doStop();

        verify(asyncEntrypointResolver).preStop();
        verify(asyncEntrypointResolver).stop();
        verify(defaultInvoker).preStop();
        verify(defaultInvoker).stop();
        verify(resourceLifecycleManager).stop();
        verify(policyManager).stop();
    }

    @Test
    void shouldIgnoreErrorAndContinueWhenErrorOccurredDuringStop() throws Exception {
        when(node.lifecycleState()).thenReturn(STOPPED);
        when(asyncEntrypointResolver.stop()).thenThrow(new RuntimeException("Mock exception"));
        cut.stop();

        verify(asyncEntrypointResolver).preStop();
        verify(asyncEntrypointResolver).stop();
        verify(defaultInvoker).preStop();
        verify(defaultInvoker, never()).stop();
        verify(resourceLifecycleManager, never()).stop();
        verify(policyManager, never()).stop();
    }
}
