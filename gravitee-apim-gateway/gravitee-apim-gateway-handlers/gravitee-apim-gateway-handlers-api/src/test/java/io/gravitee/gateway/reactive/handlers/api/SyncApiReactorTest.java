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
package io.gravitee.gateway.reactive.handlers.api;

import static io.gravitee.common.http.HttpStatusCode.UNAUTHORIZED_401;
import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.PENDING_REQUESTS_TIMEOUT_PROPERTY;
import static io.gravitee.gateway.reactive.api.ExecutionPhase.RESPONSE;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_INVOKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.Logging;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.definition.model.Proxy;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.endpoint.lifecycle.GroupLifecycleManager;
import io.gravitee.gateway.core.invoker.EndpointInvoker;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.invoker.Invoker;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionException;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.gateway.reactive.core.processor.ProcessorChain;
import io.gravitee.gateway.reactive.core.tracing.TracingHook;
import io.gravitee.gateway.reactive.handlers.api.adapter.invoker.ConnectionHandlerAdapter;
import io.gravitee.gateway.reactive.handlers.api.adapter.invoker.InvokerAdapter;
import io.gravitee.gateway.reactive.handlers.api.flow.FlowChain;
import io.gravitee.gateway.reactive.handlers.api.flow.FlowChainFactory;
import io.gravitee.gateway.reactive.handlers.api.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.reactive.handlers.api.security.SecurityChain;
import io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.LoggingHook;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.opentelemetry.tracer.noop.NoOpTracer;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SyncApiReactorTest {

    public static final Long REQUEST_TIMEOUT = 200L;
    public static final Long REQUEST_TIMEOUT_GRACE_DELAY = 30L;

    @Mock
    Api api;

    @Mock
    io.gravitee.definition.model.Api apiDefinition;

    @Mock
    ComponentProvider componentProvider;

    List<TemplateVariableProvider> templateVariableProviders;

    @Mock
    Invoker defaultInvoker;

    @Spy
    ResourceLifecycleManager resourceLifecycleManager;

    @Mock
    ApiProcessorChainFactory apiProcessorChainFactory;

    @Mock
    PolicyManager policyManager;

    @Mock
    FlowChainFactory flowChainFactory;

    @Mock
    GroupLifecycleManager groupLifecycleManager;

    @Mock
    Configuration configuration;

    @Mock
    Node node;

    @Mock
    FlowChain platformFlowChain;

    @Mock
    FlowChain apiPlanFlowChain;

    @Mock
    FlowChain apiFlowChain;

    @Mock
    ProcessorChain beforeHandleProcessors;

    @Mock
    ProcessorChain afterHandleProcessors;

    @Mock
    ProcessorChain beforeSecurityChainProcessors;

    @Mock
    ProcessorChain beforeApiFlowsProcessors;

    @Mock
    ProcessorChain afterApiFlowsProcessors;

    @Mock
    ProcessorChain onErrorProcessors;

    @Mock
    MutableExecutionContext ctx;

    @Mock
    InvokerAdapter invokerAdapter;

    @Mock
    SecurityChain securityChain;

    @Spy
    Completable spyRequestPlatformFlowChain = Completable.complete();

    @Spy
    Completable spyResponsePlatformFlowChain = Completable.complete();

    @Spy
    Completable spyRequestApiPlanFlowChain = Completable.complete();

    @Spy
    Completable spyResponseApiPlanFlowChain = Completable.complete();

    @Spy
    Completable spyRequestApiFlowChain = Completable.complete();

    @Spy
    Completable spyResponseApiFlowChain = Completable.complete();

    @Spy
    Completable spyBeforeSecurityChainProcessors = Completable.complete();

    @Spy
    Completable spyBeforeApiFlowsProcessors = Completable.complete();

    @Spy
    Completable spyBeforeHandleProcessors = Completable.complete();

    @Spy
    Completable spyAfterHandleProcessors = Completable.complete();

    @Spy
    Completable spyAfterApiFlowsProcessors = Completable.complete();

    @Spy
    Completable spyOnErrorProcessors = Completable.complete();

    @Spy
    Completable spyInvokerAdapterChain = Completable.complete();

    @Spy
    Completable spyThrowable = Completable.error(new Throwable("Invoker error"));

    @Spy
    Completable spyInterruptionFailureException = Completable.error(new InterruptionFailureException(mock(ExecutionFailure.class)));

    Completable spyTimeout;

    @Spy
    Completable spyInterruptionException = Completable.error(new InterruptionException());

    @Spy
    Completable spySecurityChain = Completable.complete();

    @Spy
    Completable spyInterruptSecurityChain = Completable.error(new InterruptionFailureException(new ExecutionFailure(UNAUTHORIZED_401)));

    SyncApiReactor cut;

    TestScheduler testScheduler;

    @Mock
    RequestTimeoutConfiguration requestTimeoutConfiguration;

    @Mock
    private AccessPointManager accessPointManager;

    @Mock
    private EventManager eventManager;

    private TracingContext tracingContext = TracingContext.noop();

    @BeforeEach
    void init() {
        lenient().when(apiDefinition.getProxy()).thenReturn(mock(Proxy.class));
        lenient().when(api.getDefinition()).thenReturn(apiDefinition);
        when(flowChainFactory.createOrganizationFlow(api, tracingContext)).thenReturn(platformFlowChain);
        when(flowChainFactory.createPlanFlow(api, tracingContext)).thenReturn(apiPlanFlowChain);
        when(flowChainFactory.createApiFlow(api, tracingContext)).thenReturn(apiFlowChain);

        lenient().when(apiProcessorChainFactory.beforeHandle(api, tracingContext)).thenReturn(beforeHandleProcessors);
        lenient().when(apiProcessorChainFactory.afterHandle(api, tracingContext)).thenReturn(afterHandleProcessors);
        lenient().when(apiProcessorChainFactory.beforeSecurityChain(api, tracingContext)).thenReturn(beforeSecurityChainProcessors);
        lenient().when(apiProcessorChainFactory.beforeApiExecution(api, tracingContext)).thenReturn(beforeApiFlowsProcessors);
        lenient().when(apiProcessorChainFactory.afterApiExecution(api, tracingContext)).thenReturn(afterApiFlowsProcessors);
        lenient().when(apiProcessorChainFactory.onError(api, tracingContext)).thenReturn(onErrorProcessors);

        lenient().when(beforeHandleProcessors.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyBeforeHandleProcessors);
        lenient().when(afterHandleProcessors.execute(ctx, RESPONSE)).thenReturn(spyAfterHandleProcessors);
        lenient().when(beforeSecurityChainProcessors.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyBeforeSecurityChainProcessors);
        lenient().when(beforeApiFlowsProcessors.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyBeforeApiFlowsProcessors);
        lenient().when(afterApiFlowsProcessors.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyAfterApiFlowsProcessors);
        lenient().when(onErrorProcessors.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyOnErrorProcessors);

        lenient().when(beforeHandleProcessors.getId()).thenReturn("mockBeforeHandleProcessors");
        lenient().when(afterHandleProcessors.getId()).thenReturn("mockBeforeHandleProcessors");
        lenient().when(beforeSecurityChainProcessors.getId()).thenReturn("mockBeforeSecurityChainProcessors");
        lenient().when(beforeApiFlowsProcessors.getId()).thenReturn("mockBeforeApiFlowsProcessors");
        lenient().when(afterApiFlowsProcessors.getId()).thenReturn("mockAfterApiFlowsProcessors");
        lenient().when(onErrorProcessors.getId()).thenReturn("mockOnErrorProcessors");

        when(configuration.getProperty(PENDING_REQUESTS_TIMEOUT_PROPERTY, Long.class, 10_000L)).thenReturn(10_000L);

        lenient().when(requestTimeoutConfiguration.getRequestTimeout()).thenReturn(REQUEST_TIMEOUT);
        lenient().when(requestTimeoutConfiguration.getRequestTimeoutGraceDelay()).thenReturn(REQUEST_TIMEOUT_GRACE_DELAY);

        templateVariableProviders = new ArrayList<>();
        cut =
            new SyncApiReactor(
                api,
                componentProvider,
                templateVariableProviders,
                defaultInvoker,
                resourceLifecycleManager,
                apiProcessorChainFactory,
                policyManager,
                flowChainFactory,
                groupLifecycleManager,
                configuration,
                node,
                requestTimeoutConfiguration,
                accessPointManager,
                eventManager,
                tracingContext
            );

        lenient().when(ctx.getInternalAttribute(ATTR_INTERNAL_INVOKER)).thenReturn(invokerAdapter);
        lenient()
            .when(
                ctx.interruptWith(
                    argThat(argument ->
                        argument != null &&
                        argument.statusCode() == 504 &&
                        argument.message().equals("Request timeout") &&
                        argument.key().equals("REQUEST_TIMEOUT")
                    )
                )
            )
            .thenReturn(Completable.error(new InterruptionFailureException(new ExecutionFailure(504))));
        testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler(s -> testScheduler);

        spyTimeout = spy(Completable.timer(REQUEST_TIMEOUT + 30L, TimeUnit.MILLISECONDS, testScheduler));
    }

    @AfterEach
    void tearDown() {
        RxJavaPlugins.reset();
    }

    @Test
    void shouldStartWithoutTracing() throws Exception {
        cut.doStart();

        verify(resourceLifecycleManager).start();
        verify(policyManager).start();
        verify(groupLifecycleManager).start();
        assertThat(cut.securityChain).isNotNull();
        assertThat(cut.processorChainHooks).isEmpty();
        assertThat(cut.invokerHooks).isEmpty();
    }

    @Test
    void shouldStartWithTracing() throws Exception {
        ReflectionTestUtils.setField(cut, "tracingContext", new TracingContext(new NoOpTracer(), true, true));

        cut.doStart();

        verify(resourceLifecycleManager).start();
        verify(policyManager).start();
        verify(groupLifecycleManager).start();
        assertThat(cut.securityChain).isNotNull();
        assertThat(cut.processorChainHooks).hasSize(1);
        assertThat(cut.processorChainHooks.get(0)).isInstanceOf(TracingHook.class);
        assertThat(cut.invokerHooks).hasSize(1);
        assertThat(cut.invokerHooks.get(0)).isInstanceOf(TracingHook.class);
    }

    @Test
    void shouldStartWithLoggingContext() throws Exception {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT);
        when(apiDefinition.getProxy().getLogging()).thenReturn(logging);

        cut.doStart();

        verify(resourceLifecycleManager).start();
        verify(policyManager).start();
        verify(groupLifecycleManager).start();
        assertThat(cut.securityChain).isNotNull();
        assertThat(cut.invokerHooks).hasSize(1);
        assertThat(cut.invokerHooks.get(0)).isInstanceOf(LoggingHook.class);
    }

    @Test
    void shouldStopNow() throws Exception {
        Lifecycle.State state = mock(Lifecycle.State.class);
        when(node.lifecycleState()).thenReturn(state);

        cut.doStop();

        verify(resourceLifecycleManager).stop();
        verify(policyManager).stop();
        verify(groupLifecycleManager).stop();
    }

    @Test
    void shouldNotStopNow() throws Exception {
        cut.doStart();
        when(node.lifecycleState()).thenReturn(Lifecycle.State.STARTED);

        cut.doStop();

        verify(resourceLifecycleManager, never()).stop();
        verify(policyManager, never()).stop();
        verify(groupLifecycleManager, never()).stop();
    }

    @Test
    void shouldStopUntil() throws Exception {
        ReflectionTestUtils.setField(cut, "pendingRequests", new AtomicInteger(1));
        Observable<Long> stopUntil = cut.stopUntil(10000L);
        TestObserver<Long> testObserver = stopUntil.test();

        testScheduler.advanceTimeBy(300L, TimeUnit.MILLISECONDS);
        testObserver.assertValueCount(3);

        verify(resourceLifecycleManager, never()).stop();
        verify(policyManager, never()).stop();
        verify(groupLifecycleManager, never()).stop();

        ReflectionTestUtils.setField(cut, "pendingRequests", new AtomicInteger(0));
        testScheduler.advanceTimeBy(200L, TimeUnit.MILLISECONDS);

        verify(resourceLifecycleManager).stop();
        verify(policyManager).stop();
        verify(groupLifecycleManager).stop();
    }

    @Test
    void shouldHandleRequest() throws Exception {
        when(api.getDeployedAt()).thenReturn(new Date());
        when(platformFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestPlatformFlowChain);
        when(beforeApiFlowsProcessors.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyBeforeApiFlowsProcessors);
        when(platformFlowChain.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyResponsePlatformFlowChain);
        when(apiPlanFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiPlanFlowChain);
        when(apiPlanFlowChain.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyResponseApiPlanFlowChain);
        when(apiFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiFlowChain);
        when(apiFlowChain.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyResponseApiFlowChain);
        when(afterApiFlowsProcessors.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyAfterApiFlowsProcessors);
        fillRequestExecutionContext();
        when(invokerAdapter.invoke(any(ExecutionContext.class))).thenReturn(spyInvokerAdapterChain);
        cut.doStart();
        when(securityChain.execute(any())).thenReturn(spySecurityChain);
        ReflectionTestUtils.setField(cut, "securityChain", securityChain);

        cut.handle(ctx).test().assertComplete();

        InOrder orderedChain = getInOrder();

        orderedChain.verify(spyBeforeHandleProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestPlatformFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyBeforeSecurityChainProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spySecurityChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyBeforeApiFlowsProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestApiPlanFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestApiFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyInvokerAdapterChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiPlanFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyAfterApiFlowsProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponsePlatformFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyAfterHandleProcessors).subscribe(any(CompletableObserver.class));
    }

    @ParameterizedTest
    @ValueSource(longs = { -10L, 0 })
    void shouldHandleRequestWhenTimeoutZeroOrLess(Long timeout) throws Exception {
        when(requestTimeoutConfiguration.getRequestTimeout()).thenReturn(timeout);
        when(api.getDeployedAt()).thenReturn(new Date());
        when(platformFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestPlatformFlowChain);
        when(beforeApiFlowsProcessors.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyBeforeApiFlowsProcessors);
        when(platformFlowChain.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyResponsePlatformFlowChain);
        when(apiPlanFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiPlanFlowChain);
        when(apiPlanFlowChain.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyResponseApiPlanFlowChain);
        when(apiFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiFlowChain);
        when(apiFlowChain.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyResponseApiFlowChain);
        when(afterApiFlowsProcessors.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyAfterApiFlowsProcessors);
        fillRequestExecutionContext();
        when(invokerAdapter.invoke(any(ExecutionContext.class))).thenReturn(spyInvokerAdapterChain);
        cut.doStart();
        when(securityChain.execute(any())).thenReturn(spySecurityChain);
        ReflectionTestUtils.setField(cut, "securityChain", securityChain);

        cut.handle(ctx).test().assertComplete();
        InOrder orderedChain = getInOrder();

        orderedChain.verify(spyBeforeHandleProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestPlatformFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyBeforeSecurityChainProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spySecurityChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyBeforeApiFlowsProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestApiPlanFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestApiFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyInvokerAdapterChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiPlanFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyAfterApiFlowsProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponsePlatformFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyAfterHandleProcessors).subscribe(any(CompletableObserver.class));
    }

    @ParameterizedTest
    @ValueSource(longs = { -10L, 0 })
    void shouldNotApplyFlowChainWhenTimeoutZeroOrLess_InterruptionException(Long timeout) throws Exception {
        when(requestTimeoutConfiguration.getRequestTimeout()).thenReturn(timeout);
        InOrder orderedChain = testFlowChainWithInvokeBackendError(spyInterruptionException, InterruptionException.class);

        orderedChain.verify(spyAfterApiFlowsProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyOnErrorProcessors, never()).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponsePlatformFlowChain).subscribe(any(CompletableObserver.class));
    }

    @Test
    void shouldHandleRequestWithLegacyInvoker() throws Exception {
        when(api.getDeployedAt()).thenReturn(new Date());
        when(platformFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestPlatformFlowChain);
        when(beforeApiFlowsProcessors.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyBeforeApiFlowsProcessors);
        when(apiPlanFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiPlanFlowChain);
        when(apiFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiFlowChain);
        when(afterApiFlowsProcessors.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyAfterApiFlowsProcessors);
        fillRequestExecutionContext();
        EndpointInvoker endpointInvoker = mock(EndpointInvoker.class);
        lenient().when(ctx.getInternalAttribute(ATTR_INTERNAL_INVOKER)).thenReturn(endpointInvoker);
        ArgumentCaptor<Handler<ProxyConnection>> handlerArgumentCaptor = ArgumentCaptor.forClass(Handler.class);
        cut.doStart();
        when(securityChain.execute(any())).thenReturn(spySecurityChain);
        ReflectionTestUtils.setField(cut, "securityChain", securityChain);

        cut.handle(ctx).subscribe();

        verify(endpointInvoker).invoke(any(), any(), handlerArgumentCaptor.capture());
        assertThat(handlerArgumentCaptor.getValue()).isInstanceOf(ConnectionHandlerAdapter.class);
    }

    @Test
    void shouldNotApplyFlowChain_InterruptionException() throws Exception {
        InOrder orderedChain = testFlowChainWithInvokeBackendError(spyInterruptionException, InterruptionException.class);

        orderedChain.verify(spyAfterApiFlowsProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyOnErrorProcessors, never()).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponsePlatformFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyAfterHandleProcessors).subscribe(any(CompletableObserver.class));
    }

    @Test
    void shouldNotApplyFlowChain_InterruptionFailureException() throws Exception {
        when(onErrorProcessors.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyOnErrorProcessors);

        InOrder orderedChain = testFlowChainWithInvokeBackendError(spyInterruptionFailureException, InterruptionFailureException.class);

        orderedChain.verify(spyAfterApiFlowsProcessors, never()).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyOnErrorProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponsePlatformFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyAfterHandleProcessors).subscribe(any(CompletableObserver.class));
    }

    @Test
    void shouldNotApplyFlowChain_Throwable() throws Exception {
        when(onErrorProcessors.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyOnErrorProcessors);

        InOrder orderedChain = testFlowChainWithInvokeBackendError(spyThrowable, Throwable.class);

        orderedChain.verify(spyAfterApiFlowsProcessors, never()).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyOnErrorProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponsePlatformFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyAfterHandleProcessors).subscribe(any(CompletableObserver.class));
    }

    InOrder testFlowChainWithInvokeBackendError(Completable spyInvokerAdapterError, Class<? extends Throwable> clazz) throws Exception {
        when(api.getDeployedAt()).thenReturn(new Date());
        when(platformFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestPlatformFlowChain);
        when(beforeApiFlowsProcessors.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyBeforeApiFlowsProcessors);
        when(platformFlowChain.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyResponsePlatformFlowChain);
        when(apiPlanFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiPlanFlowChain);
        when(apiFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiFlowChain);
        when(afterApiFlowsProcessors.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyAfterApiFlowsProcessors);
        fillRequestExecutionContext();

        when(invokerAdapter.invoke(any(ExecutionContext.class))).thenReturn(spyInvokerAdapterError);

        cut.doStart();
        when(securityChain.execute(any())).thenReturn(spySecurityChain);
        ReflectionTestUtils.setField(cut, "securityChain", securityChain);

        cut.handle(ctx).test().assertComplete();
        InOrder orderedChain = getInOrder();

        orderedChain.verify(spyBeforeHandleProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestPlatformFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyBeforeSecurityChainProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spySecurityChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyBeforeApiFlowsProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestApiPlanFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestApiFlowChain).subscribe(any(CompletableObserver.class));
        spyInvokerAdapterError.test().assertError(clazz);
        orderedChain.verify(spyResponseApiPlanFlowChain, never()).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiFlowChain, never()).subscribe(any(CompletableObserver.class));
        return orderedChain;
    }

    @Test
    void shouldInterruptChainBecauseOfTimeoutOnBackend() throws Exception {
        when(onErrorProcessors.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyOnErrorProcessors);

        when(api.getDeployedAt()).thenReturn(new Date());
        when(platformFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestPlatformFlowChain);
        when(beforeApiFlowsProcessors.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyBeforeApiFlowsProcessors);
        when(platformFlowChain.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyResponsePlatformFlowChain);
        when(apiPlanFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiPlanFlowChain);
        when(apiFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiFlowChain);
        when(afterApiFlowsProcessors.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyAfterApiFlowsProcessors);
        fillRequestExecutionContext();

        when(invokerAdapter.invoke(any(ExecutionContext.class))).thenReturn(spyTimeout);

        cut.doStart();
        when(securityChain.execute(any())).thenReturn(spySecurityChain);
        ReflectionTestUtils.setField(cut, "securityChain", securityChain);

        cut.handle(ctx).subscribe();
        testScheduler.advanceTimeBy(REQUEST_TIMEOUT + 30L, TimeUnit.MILLISECONDS);

        InOrder orderedChain = getInOrder();

        orderedChain.verify(spyBeforeHandleProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestPlatformFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyBeforeSecurityChainProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spySecurityChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyBeforeApiFlowsProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestApiPlanFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestApiFlowChain).subscribe(any(CompletableObserver.class));
        spyTimeout.test().assertNotComplete();
        orderedChain.verify(spyResponseApiFlowChain, never()).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiPlanFlowChain, never()).subscribe(any(CompletableObserver.class));

        orderedChain.verify(spyAfterApiFlowsProcessors, never()).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyOnErrorProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponsePlatformFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyAfterHandleProcessors).subscribe(any(CompletableObserver.class));
    }

    @Test
    void shouldInterruptChainBecauseOfTimeoutOnRequest() throws Exception {
        when(onErrorProcessors.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyOnErrorProcessors);

        when(api.getDeployedAt()).thenReturn(new Date());
        when(platformFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestPlatformFlowChain);
        when(beforeApiFlowsProcessors.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyBeforeApiFlowsProcessors);
        when(platformFlowChain.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyResponsePlatformFlowChain);
        when(apiPlanFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiPlanFlowChain);
        when(apiFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyTimeout);
        when(afterApiFlowsProcessors.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyAfterApiFlowsProcessors);
        fillRequestExecutionContext();

        cut.doStart();
        when(securityChain.execute(any())).thenReturn(spySecurityChain);
        ReflectionTestUtils.setField(cut, "securityChain", securityChain);

        cut.handle(ctx).subscribe();
        testScheduler.advanceTimeBy(REQUEST_TIMEOUT + 30L, TimeUnit.MILLISECONDS);

        InOrder orderedChain = getInOrder();

        orderedChain.verify(spyBeforeHandleProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestPlatformFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyBeforeSecurityChainProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spySecurityChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyBeforeApiFlowsProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestApiPlanFlowChain).subscribe(any(CompletableObserver.class));
        spyTimeout.test().assertNotComplete();
        orderedChain.verify(spyInvokerAdapterChain, never()).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiFlowChain, never()).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiPlanFlowChain, never()).subscribe(any(CompletableObserver.class));

        orderedChain.verify(spyAfterApiFlowsProcessors, never()).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyOnErrorProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponsePlatformFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyAfterHandleProcessors).subscribe(any(CompletableObserver.class));
    }

    @Test
    void shouldInterruptChainBecauseOfTimeoutOnResponsePlatformPolicies() throws Exception {
        when(onErrorProcessors.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyOnErrorProcessors);

        when(api.getDeployedAt()).thenReturn(new Date());
        when(platformFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestPlatformFlowChain);
        when(beforeApiFlowsProcessors.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyBeforeApiFlowsProcessors);
        when(apiPlanFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiPlanFlowChain);
        when(apiFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiFlowChain);
        when(afterApiFlowsProcessors.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyAfterApiFlowsProcessors);
        when(platformFlowChain.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyTimeout);
        fillRequestExecutionContext();

        cut.doStart();
        when(securityChain.execute(any())).thenReturn(spySecurityChain);
        ReflectionTestUtils.setField(cut, "securityChain", securityChain);

        cut.handle(ctx).subscribe();
        testScheduler.advanceTimeBy(REQUEST_TIMEOUT + 30L, TimeUnit.MILLISECONDS);

        InOrder orderedChain = getInOrder();

        orderedChain.verify(spyBeforeHandleProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestPlatformFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyBeforeSecurityChainProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spySecurityChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyBeforeApiFlowsProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestApiPlanFlowChain).subscribe(any(CompletableObserver.class));
        spyTimeout.test().assertNotComplete();
        orderedChain.verify(spyInvokerAdapterChain, never()).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiFlowChain, never()).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiPlanFlowChain, never()).subscribe(any(CompletableObserver.class));

        orderedChain.verify(spyAfterApiFlowsProcessors, never()).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyOnErrorProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyAfterHandleProcessors).subscribe(any(CompletableObserver.class));
    }

    @Test
    void shouldNotApplyFlowChain_SecurityChain() throws Exception {
        when(api.getDeployedAt()).thenReturn(new Date());
        when(platformFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyRequestPlatformFlowChain);
        when(beforeApiFlowsProcessors.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(spyBeforeApiFlowsProcessors);
        when(platformFlowChain.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyResponsePlatformFlowChain);
        when(afterApiFlowsProcessors.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyAfterApiFlowsProcessors);
        when(onErrorProcessors.execute(ctx, ExecutionPhase.RESPONSE)).thenReturn(spyOnErrorProcessors);
        fillRequestExecutionContext();
        cut.doStart();
        when(securityChain.execute(any())).thenReturn(spyInterruptSecurityChain);
        ReflectionTestUtils.setField(cut, "securityChain", securityChain);

        cut.handle(ctx).test().assertComplete();
        InOrder orderedChain = getInOrder();

        orderedChain.verify(spyBeforeHandleProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestPlatformFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyInterruptSecurityChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyBeforeApiFlowsProcessors, never()).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestApiPlanFlowChain, never()).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestApiFlowChain, never()).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyInvokerAdapterChain, never()).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiPlanFlowChain, never()).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiFlowChain, never()).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyAfterApiFlowsProcessors, never()).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyOnErrorProcessors).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponsePlatformFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyAfterHandleProcessors).subscribe(any(CompletableObserver.class));
    }

    private InOrder getInOrder() {
        return inOrder(
            spyBeforeHandleProcessors,
            spyAfterHandleProcessors,
            spyRequestPlatformFlowChain,
            spyBeforeSecurityChainProcessors,
            spySecurityChain,
            spyInterruptSecurityChain,
            spyBeforeApiFlowsProcessors,
            spyRequestApiPlanFlowChain,
            spyRequestApiFlowChain,
            spyInvokerAdapterChain,
            spyResponseApiFlowChain,
            spyResponseApiPlanFlowChain,
            spyAfterApiFlowsProcessors,
            spyOnErrorProcessors,
            spyResponsePlatformFlowChain
        );
    }

    private void fillRequestExecutionContext() {
        MutableRequest request = mock(MutableRequest.class);
        when(request.contextPath()).thenReturn("/contextPath");
        Metrics metrics = mock(Metrics.class);
        when(ctx.metrics()).thenReturn(metrics);
        when(ctx.request()).thenReturn(request);
        MutableResponse response = mock(MutableResponse.class);
        when(response.end(ctx)).thenReturn(Completable.complete());
        when(ctx.response()).thenReturn(response);
        when(ctx.componentProvider(any())).thenReturn(ctx);
        when(ctx.templateVariableProviders(any())).thenReturn(ctx);
    }
}
