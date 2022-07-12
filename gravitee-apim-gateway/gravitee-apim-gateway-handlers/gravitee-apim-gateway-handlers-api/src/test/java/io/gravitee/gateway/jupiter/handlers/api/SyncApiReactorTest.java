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
package io.gravitee.gateway.jupiter.handlers.api;

import static io.gravitee.common.http.HttpStatusCode.UNAUTHORIZED_401;
import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.PENDING_REQUESTS_TIMEOUT_PROPERTY;
import static io.gravitee.gateway.jupiter.api.context.ExecutionContext.ATTR_INVOKER;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.endpoint.lifecycle.GroupLifecycleManager;
import io.gravitee.gateway.core.invoker.EndpointInvoker;
import io.gravitee.gateway.core.logging.LoggingContext;
import io.gravitee.gateway.core.logging.utils.LoggingUtils;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.invoker.Invoker;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.gravitee.gateway.jupiter.core.context.MutableRequestExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionException;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionFailureException;
import io.gravitee.gateway.jupiter.core.processor.ProcessorChain;
import io.gravitee.gateway.jupiter.core.tracing.TracingHook;
import io.gravitee.gateway.jupiter.handlers.api.adapter.invoker.ConnectionHandlerAdapter;
import io.gravitee.gateway.jupiter.handlers.api.adapter.invoker.InvokerAdapter;
import io.gravitee.gateway.jupiter.handlers.api.flow.FlowChain;
import io.gravitee.gateway.jupiter.handlers.api.flow.FlowChainFactory;
import io.gravitee.gateway.jupiter.handlers.api.hook.logging.LoggingHook;
import io.gravitee.gateway.jupiter.handlers.api.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.jupiter.handlers.api.security.SecurityChain;
import io.gravitee.gateway.jupiter.policy.PolicyManager;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.repository.log.model.Log;
import io.reactivex.*;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.TestScheduler;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class SyncApiReactorTest {

    @Mock
    Api api;

    @Mock
    ComponentProvider componentProvider;

    @Mock
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
    ProcessorChain apiPreProcessorChain;

    @Mock
    ProcessorChain apiPostProcessorChain;

    @Mock
    ProcessorChain apiErrorProcessorChain;

    @Mock
    MutableRequestExecutionContext requestExecutionContext;

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
    Completable spyRequestApiPreProcessorChain = Completable.complete();

    @Spy
    Completable spyResponseApiPostProcessorChain = Completable.complete();

    @Spy
    Completable spyInvokerAdapterChain = Completable.complete();

    @Spy
    Completable spyThrowable = Completable.error(new Throwable("Invoker error"));

    @Spy
    Completable spyInterruptionFailureException = Completable.error(new InterruptionFailureException(mock(ExecutionFailure.class)));

    @Spy
    Completable spyInterruptionException = Completable.error(new InterruptionException());

    @Spy
    Completable spyResponseApiErrorProcessorChain = Completable.complete();

    @Spy
    Completable spySecurityChain = Completable.complete();

    @Spy
    Completable spyInterruptSecurityChain = Completable.error(new InterruptionFailureException(new ExecutionFailure(UNAUTHORIZED_401)));

    MockedStatic<LoggingUtils> LoggingUtilsMockedStatic;

    @Mock
    LoggingContext loggingContext;

    SyncApiReactor cut;

    TestScheduler testScheduler;

    @BeforeEach
    void init() {
        LoggingUtilsMockedStatic = mockStatic(LoggingUtils.class);
        LoggingUtilsMockedStatic.when(() -> LoggingUtils.getLoggingContext(api)).thenReturn(null);

        when(flowChainFactory.createPlatformFlow(api)).thenReturn(platformFlowChain);
        when(flowChainFactory.createPlanFlow(api)).thenReturn(apiPlanFlowChain);
        when(flowChainFactory.createApiFlow(api)).thenReturn(apiFlowChain);
        when(apiProcessorChainFactory.preProcessorChain(api)).thenReturn(apiPreProcessorChain);
        when(apiProcessorChainFactory.postProcessorChain(api)).thenReturn(apiPostProcessorChain);
        when(apiProcessorChainFactory.errorProcessorChain(api)).thenReturn(apiErrorProcessorChain);

        when(configuration.getProperty("services.tracing.enabled", Boolean.class, false)).thenReturn(false);
        when(configuration.getProperty(PENDING_REQUESTS_TIMEOUT_PROPERTY, Long.class, 10_000L)).thenReturn(10_000L);

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
                node
            );

        lenient().when(requestExecutionContext.getAttribute(ATTR_INVOKER)).thenReturn(invokerAdapter);
        testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler(s -> testScheduler);
    }

    @AfterEach
    void tearDown() {
        LoggingUtilsMockedStatic.close();
        RxJavaPlugins.reset();
    }

    @Test
    void shouldStartWithoutTracing() throws Exception {
        cut.doStart();

        assertEquals(cut.executionMode(), ExecutionMode.JUPITER);
        verify(resourceLifecycleManager).start();
        verify(policyManager).start();
        verify(groupLifecycleManager).start();
        assertNotNull(cut.securityChain);
        assertTrue(cut.processorChainHooks.isEmpty());
        assertTrue(cut.invokerHooks.isEmpty());
    }

    @Test
    void shouldStartWithTracing() throws Exception {
        ReflectionTestUtils.setField(cut, "tracingEnabled", true);

        cut.doStart();

        verify(resourceLifecycleManager).start();
        verify(policyManager).start();
        verify(groupLifecycleManager).start();
        assertNotNull(cut.securityChain);
        assertEquals(1, cut.processorChainHooks.size());
        assertTrue(cut.processorChainHooks.get(0) instanceof TracingHook);
        assertEquals(1, cut.invokerHooks.size());
        assertTrue(cut.invokerHooks.get(0) instanceof TracingHook);
    }

    @Test
    void shouldStartWithLoggingContext() throws Exception {
        LoggingUtilsMockedStatic.when(() -> LoggingUtils.getLoggingContext(api)).thenReturn(loggingContext);

        cut.doStart();

        verify(resourceLifecycleManager).start();
        verify(policyManager).start();
        verify(groupLifecycleManager).start();
        assertNotNull(cut.securityChain);
        assertEquals(1, cut.invokerHooks.size());
        assertTrue(cut.invokerHooks.get(0) instanceof LoggingHook);
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

        verify(resourceLifecycleManager, times(0)).stop();
        verify(policyManager, times(0)).stop();
        verify(groupLifecycleManager, times(0)).stop();
    }

    @Test
    void shouldStopUntil() throws Exception {
        ReflectionTestUtils.setField(cut, "pendingRequests", new AtomicInteger(1));
        Observable<Long> stopUntil = cut.stopUntil(10000L);
        TestObserver<Long> testObserver = stopUntil.test();

        testScheduler.advanceTimeBy(300L, TimeUnit.MILLISECONDS);
        assertEquals(3, testObserver.valueCount());

        verify(resourceLifecycleManager, times(0)).stop();
        verify(policyManager, times(0)).stop();
        verify(groupLifecycleManager, times(0)).stop();

        ReflectionTestUtils.setField(cut, "pendingRequests", new AtomicInteger(0));
        testScheduler.advanceTimeBy(200L, TimeUnit.MILLISECONDS);

        verify(resourceLifecycleManager).stop();
        verify(policyManager).stop();
        verify(groupLifecycleManager).stop();
    }

    @Test
    void shouldHandleRequest() throws Exception {
        when(api.getDeployedAt()).thenReturn(new Date());
        when(platformFlowChain.execute(requestExecutionContext, ExecutionPhase.REQUEST)).thenReturn(spyRequestPlatformFlowChain);
        when(apiPreProcessorChain.execute(requestExecutionContext, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiPreProcessorChain);
        when(platformFlowChain.execute(requestExecutionContext, ExecutionPhase.RESPONSE)).thenReturn(spyResponsePlatformFlowChain);
        when(apiPlanFlowChain.execute(requestExecutionContext, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiPlanFlowChain);
        when(apiPlanFlowChain.execute(requestExecutionContext, ExecutionPhase.RESPONSE)).thenReturn(spyResponseApiPlanFlowChain);
        when(apiFlowChain.execute(requestExecutionContext, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiFlowChain);
        when(apiFlowChain.execute(requestExecutionContext, ExecutionPhase.RESPONSE)).thenReturn(spyResponseApiFlowChain);
        when(apiPostProcessorChain.execute(requestExecutionContext, ExecutionPhase.RESPONSE)).thenReturn(spyResponseApiPostProcessorChain);
        fillRequestExecutionContext();
        when(invokerAdapter.invoke(any())).thenReturn(spyInvokerAdapterChain);
        cut.doStart();
        when(securityChain.execute(any())).thenReturn(spySecurityChain);
        ReflectionTestUtils.setField(cut, "securityChain", securityChain);

        TestObserver<Void> handleRequestObserver = cut.handle(requestExecutionContext).test();

        handleRequestObserver.assertSubscribed();
        InOrder orderedChain = inOrder(
            spyRequestPlatformFlowChain,
            spySecurityChain,
            spyRequestApiPreProcessorChain,
            spyRequestApiPlanFlowChain,
            spyRequestApiFlowChain,
            spyInvokerAdapterChain,
            spyResponseApiFlowChain,
            spyResponseApiPlanFlowChain,
            spyResponseApiPostProcessorChain,
            spyResponsePlatformFlowChain
        );
        orderedChain.verify(spyRequestPlatformFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spySecurityChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestApiPreProcessorChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestApiPlanFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestApiFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyInvokerAdapterChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiPlanFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiPostProcessorChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponsePlatformFlowChain).subscribe(any(CompletableObserver.class));
    }

    @Test
    void shouldHandleRequestWithLegacyInvoker() throws Exception {
        when(api.getDeployedAt()).thenReturn(new Date());
        when(platformFlowChain.execute(requestExecutionContext, ExecutionPhase.REQUEST)).thenReturn(spyRequestPlatformFlowChain);
        when(apiPreProcessorChain.execute(requestExecutionContext, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiPreProcessorChain);
        when(apiPlanFlowChain.execute(requestExecutionContext, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiPlanFlowChain);
        when(apiFlowChain.execute(requestExecutionContext, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiFlowChain);
        when(apiPostProcessorChain.execute(requestExecutionContext, ExecutionPhase.RESPONSE)).thenReturn(spyResponseApiPostProcessorChain);
        fillRequestExecutionContext();
        EndpointInvoker endpointInvoker = mock(EndpointInvoker.class);
        lenient().when(requestExecutionContext.getAttribute(ATTR_INVOKER)).thenReturn(endpointInvoker);
        ArgumentCaptor<Handler<ProxyConnection>> handlerArgumentCaptor = ArgumentCaptor.forClass(Handler.class);
        cut.doStart();
        when(securityChain.execute(any())).thenReturn(spySecurityChain);
        ReflectionTestUtils.setField(cut, "securityChain", securityChain);

        TestObserver<Void> handleRequestObserver = cut.handle(requestExecutionContext).test();

        handleRequestObserver.assertSubscribed();
        verify(endpointInvoker).invoke(any(), any(), handlerArgumentCaptor.capture());
        assertNotNull(handlerArgumentCaptor.getValue());
        assertTrue(handlerArgumentCaptor.getValue() instanceof ConnectionHandlerAdapter);
    }

    @Test
    void shouldNotApplyFlowChain_InterruptionException() throws Exception {
        InOrder orderedChain = testFlowChainWithInvokeBackendError(spyInterruptionException, InterruptionException.class);

        orderedChain.verify(spyResponseApiPostProcessorChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiErrorProcessorChain, times(0)).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponsePlatformFlowChain).subscribe(any(CompletableObserver.class));
    }

    @Test
    void shouldNotApplyFlowChain_InterruptionFailureException() throws Exception {
        when(apiErrorProcessorChain.execute(requestExecutionContext, ExecutionPhase.RESPONSE))
            .thenReturn(spyResponseApiErrorProcessorChain);

        InOrder orderedChain = testFlowChainWithInvokeBackendError(spyInterruptionFailureException, InterruptionFailureException.class);

        orderedChain.verify(spyResponseApiPostProcessorChain, times(0)).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiErrorProcessorChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponsePlatformFlowChain).subscribe(any(CompletableObserver.class));
    }

    @Test
    void shouldNotApplyFlowChain_Throwable() throws Exception {
        when(apiErrorProcessorChain.execute(requestExecutionContext, ExecutionPhase.RESPONSE))
            .thenReturn(spyResponseApiErrorProcessorChain);

        InOrder orderedChain = testFlowChainWithInvokeBackendError(spyThrowable, Throwable.class);

        orderedChain.verify(spyResponseApiPostProcessorChain, times(0)).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiErrorProcessorChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponsePlatformFlowChain).subscribe(any(CompletableObserver.class));
    }

    InOrder testFlowChainWithInvokeBackendError(Completable spyInvokerAdapterError, Class clasz) throws Exception {
        when(api.getDeployedAt()).thenReturn(new Date());
        when(platformFlowChain.execute(requestExecutionContext, ExecutionPhase.REQUEST)).thenReturn(spyRequestPlatformFlowChain);
        when(apiPreProcessorChain.execute(requestExecutionContext, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiPreProcessorChain);
        when(platformFlowChain.execute(requestExecutionContext, ExecutionPhase.RESPONSE)).thenReturn(spyResponsePlatformFlowChain);
        when(apiPlanFlowChain.execute(requestExecutionContext, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiPlanFlowChain);
        when(apiFlowChain.execute(requestExecutionContext, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiFlowChain);
        when(apiPostProcessorChain.execute(requestExecutionContext, ExecutionPhase.RESPONSE)).thenReturn(spyResponseApiPostProcessorChain);
        fillRequestExecutionContext();

        when(invokerAdapter.invoke(any())).thenReturn(spyInvokerAdapterError);

        cut.doStart();
        when(securityChain.execute(any())).thenReturn(spySecurityChain);
        ReflectionTestUtils.setField(cut, "securityChain", securityChain);

        TestObserver<Void> handleRequestObserver = cut.handle(requestExecutionContext).test();

        InOrder orderedChain = inOrder(
            spyRequestPlatformFlowChain,
            spySecurityChain,
            spyRequestApiPreProcessorChain,
            spyRequestApiPlanFlowChain,
            spyRequestApiFlowChain,
            spyInvokerAdapterError,
            spyResponseApiFlowChain,
            spyResponseApiPlanFlowChain,
            spyResponseApiPostProcessorChain,
            spyResponseApiErrorProcessorChain,
            spyResponsePlatformFlowChain
        );

        handleRequestObserver.assertSubscribed();

        orderedChain.verify(spyRequestPlatformFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spySecurityChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestApiPreProcessorChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestApiPlanFlowChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestApiFlowChain).subscribe(any(CompletableObserver.class));
        spyInvokerAdapterError.test().assertError(clasz);
        orderedChain.verify(spyResponseApiPlanFlowChain, times(0)).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiFlowChain, times(0)).subscribe(any(CompletableObserver.class));
        return orderedChain;
    }

    @Test
    void shouldNotApplyFlowChain_SecurityChain() throws Exception {
        when(api.getDeployedAt()).thenReturn(new Date());
        when(platformFlowChain.execute(requestExecutionContext, ExecutionPhase.REQUEST)).thenReturn(spyRequestPlatformFlowChain);
        when(apiPreProcessorChain.execute(requestExecutionContext, ExecutionPhase.REQUEST)).thenReturn(spyRequestApiPreProcessorChain);
        when(platformFlowChain.execute(requestExecutionContext, ExecutionPhase.RESPONSE)).thenReturn(spyResponsePlatformFlowChain);
        when(apiPostProcessorChain.execute(requestExecutionContext, ExecutionPhase.RESPONSE)).thenReturn(spyResponseApiPostProcessorChain);
        when(apiErrorProcessorChain.execute(requestExecutionContext, ExecutionPhase.RESPONSE))
            .thenReturn(spyResponseApiErrorProcessorChain);
        fillRequestExecutionContext();
        cut.doStart();
        when(securityChain.execute(any())).thenReturn(spyInterruptSecurityChain);
        ReflectionTestUtils.setField(cut, "securityChain", securityChain);

        TestObserver<Void> handleRequestObserver = cut.handle(requestExecutionContext).test();

        InOrder orderedChain = inOrder(
            spyRequestPlatformFlowChain,
            spyInterruptSecurityChain,
            spyRequestApiPreProcessorChain,
            spyRequestApiPlanFlowChain,
            spyRequestApiFlowChain,
            spyInvokerAdapterChain,
            spyResponseApiFlowChain,
            spyResponseApiPlanFlowChain,
            spyResponseApiPostProcessorChain,
            spyResponseApiErrorProcessorChain,
            spyResponsePlatformFlowChain
        );

        handleRequestObserver.assertSubscribed();

        orderedChain.verify(spyRequestPlatformFlowChain).subscribe(any(CompletableObserver.class));
        spyInterruptSecurityChain.test().assertError(InterruptionFailureException.class);
        orderedChain.verify(spyRequestApiPreProcessorChain, times(0)).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestApiPlanFlowChain, times(0)).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyRequestApiFlowChain, times(0)).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyInvokerAdapterChain, times(0)).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiPlanFlowChain, times(0)).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiFlowChain, times(0)).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiPostProcessorChain, times(0)).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponseApiErrorProcessorChain).subscribe(any(CompletableObserver.class));
        orderedChain.verify(spyResponsePlatformFlowChain).subscribe(any(CompletableObserver.class));
    }

    private void fillRequestExecutionContext() {
        MutableRequest request = mock(MutableRequest.class);
        when(request.contextPath()).thenReturn("/contextPath");
        Metrics metrics = mock(Metrics.class);
        when(request.metrics()).thenReturn(metrics);
        when(requestExecutionContext.request()).thenReturn(request);
        MutableResponse response = mock(MutableResponse.class);
        when(response.end()).thenReturn(Completable.complete());
        when(requestExecutionContext.response()).thenReturn(response);
        when(requestExecutionContext.componentProvider(any())).thenReturn(requestExecutionContext);
        when(requestExecutionContext.templateVariableProviders(any())).thenReturn(requestExecutionContext);
    }
}
