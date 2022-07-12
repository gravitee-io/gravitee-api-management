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

import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.PENDING_REQUESTS_TIMEOUT_PROPERTY;
import static io.gravitee.gateway.jupiter.api.context.ExecutionContext.ATTR_INVOKER;
import static io.gravitee.gateway.jupiter.handlers.api.security.SecurityChain.SKIP_SECURITY_CHAIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.definition.model.Proxy;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.endpoint.lifecycle.GroupLifecycleManager;
import io.gravitee.gateway.core.invoker.EndpointInvoker;
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
import io.gravitee.gateway.jupiter.handlers.api.adapter.invoker.InvokerAdapter;
import io.gravitee.gateway.jupiter.handlers.api.flow.FlowChain;
import io.gravitee.gateway.jupiter.handlers.api.flow.FlowChainFactory;
import io.gravitee.gateway.jupiter.handlers.api.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.jupiter.policy.PolicyManager;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.reporter.api.http.Metrics;
import io.reactivex.Completable;
import io.reactivex.observers.TestObserver;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SyncApiReactorTest {

    @Mock
    Api api;

    @Mock
    ComponentProvider componentProvider;

    @Mock
    List<TemplateVariableProvider> templateVariableProviders;

    @Mock
    Invoker defaultInvoker;

    @Mock
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

    @Before
    public void setUp() {
        Proxy proxy = mock(Proxy.class);
        when(proxy.getLogging()).thenReturn(null);
        when(api.getProxy()).thenReturn(proxy);
        when(api.getDeployedAt()).thenReturn(new Date());

        when(platformFlowChain.execute(any(), any())).thenReturn(Completable.complete());
        when(apiPlanFlowChain.execute(any(), any())).thenReturn(Completable.complete());
        when(apiFlowChain.execute(any(), any())).thenReturn(Completable.complete());

        when(flowChainFactory.createPlatformFlow(eq(api))).thenReturn(platformFlowChain);
        when(flowChainFactory.createPlanFlow(eq(api))).thenReturn(apiPlanFlowChain);
        when(flowChainFactory.createApiFlow(eq(api))).thenReturn(apiFlowChain);

        when(apiPreProcessorChain.getId()).thenReturn("apiPreProcessorChain");
        when(apiPostProcessorChain.getId()).thenReturn("apiPostProcessorChain");
        when(apiPreProcessorChain.execute(any(), eq(ExecutionPhase.REQUEST))).thenReturn(Completable.complete());
        when(apiPostProcessorChain.execute(any(), eq(ExecutionPhase.RESPONSE))).thenReturn(Completable.complete());

        when(apiProcessorChainFactory.preProcessorChain(eq(api))).thenReturn(apiPreProcessorChain);
        when(apiProcessorChainFactory.postProcessorChain(eq(api))).thenReturn(apiPostProcessorChain);
        when(apiProcessorChainFactory.errorProcessorChain(eq(api))).thenReturn(apiErrorProcessorChain);

        when(configuration.getProperty("services.tracing.enabled", Boolean.class, false)).thenReturn(false);
        when(configuration.getProperty(PENDING_REQUESTS_TIMEOUT_PROPERTY, Long.class, 10_000L)).thenReturn(10_000L);
    }

    private SyncApiReactor newSyncApiReactor() {
        return new SyncApiReactor(
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
    }

    @Test
    public void shouldStartWithoutTracing() throws Exception {
        SyncApiReactor syncApiReactor = newSyncApiReactor();

        syncApiReactor.doStart();

        assertEquals(syncApiReactor.executionMode(), ExecutionMode.JUPITER);
        verify(resourceLifecycleManager, times(1)).start();
        verify(policyManager, times(1)).start();
        verify(groupLifecycleManager, times(1)).start();
        assertNotNull(syncApiReactor.securityChain);
        assertEquals(syncApiReactor.processorChainHooks.size(), 0);
        assertEquals(syncApiReactor.invokerHooks.size(), 0);
    }

    @Test
    public void shouldStopNow() throws Exception {
        Lifecycle.State state = mock(Lifecycle.State.class);
        when(node.lifecycleState()).thenReturn(state);
        SyncApiReactor syncApiReactor = newSyncApiReactor();

        syncApiReactor.doStop();

        verify(resourceLifecycleManager, times(1)).stop();
        verify(policyManager, times(1)).stop();
        verify(groupLifecycleManager, times(1)).stop();
    }

    @Test
    public void shouldStopUntil() throws Exception {
        when(node.lifecycleState()).thenReturn(Lifecycle.State.STARTED);
        SyncApiReactor syncApiReactor = newSyncApiReactor();

        syncApiReactor.doStop();

        verify(resourceLifecycleManager, times(0)).stop();
        verify(policyManager, times(0)).stop();
        verify(groupLifecycleManager, times(0)).stop();
    }

    @Test
    public void shouldStartWithTracing() throws Exception {
        when(configuration.getProperty("services.tracing.enabled", Boolean.class, false)).thenReturn(true);
        SyncApiReactor syncApiReactor = newSyncApiReactor();

        syncApiReactor.doStart();

        verify(resourceLifecycleManager, times(1)).start();
        verify(policyManager, times(1)).start();
        verify(groupLifecycleManager, times(1)).start();
        assertNotNull(syncApiReactor.securityChain);
        assertEquals(syncApiReactor.processorChainHooks.size(), 1);
        assertEquals(syncApiReactor.invokerHooks.size(), 1);
    }

    @Test
    public void shouldHandleRequest() throws Exception {
        MutableRequestExecutionContext ctx = mock(MutableRequestExecutionContext.class);
        MutableRequest request = mock(MutableRequest.class);
        when(request.contextPath()).thenReturn("/contextPath");
        Metrics metrics = mock(Metrics.class);
        when(request.metrics()).thenReturn(metrics);
        when(ctx.request()).thenReturn(request);
        MutableResponse response = mock(MutableResponse.class);
        when(response.end()).thenReturn(Completable.complete());
        when(ctx.response()).thenReturn(response);
        when(ctx.componentProvider(any())).thenReturn(ctx);
        when(ctx.templateVariableProviders(any())).thenReturn(ctx);
        when(ctx.getAttribute(SKIP_SECURITY_CHAIN)).thenReturn(true);
        InvokerAdapter invokerAdapter = mock(InvokerAdapter.class);
        when(invokerAdapter.invoke(any())).thenReturn(Completable.complete());
        when(ctx.getAttribute(ATTR_INVOKER)).thenReturn(invokerAdapter);
        SyncApiReactor syncApiReactor = newSyncApiReactor();
        syncApiReactor.doStart();

        TestObserver<Void> handleRequestObserver = syncApiReactor.handle(ctx).test();

        handleRequestObserver.assertComplete();
        InOrder orderedChain = inOrder(
            platformFlowChain,
            apiPreProcessorChain,
            apiPlanFlowChain,
            apiFlowChain,
            apiPlanFlowChain,
            invokerAdapter,
            apiFlowChain,
            apiPostProcessorChain,
            platformFlowChain
        );
        orderedChain.verify(platformFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.REQUEST));
        orderedChain.verify(apiPreProcessorChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.REQUEST));
        orderedChain.verify(apiPlanFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.REQUEST));
        orderedChain.verify(apiFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.REQUEST));
        orderedChain.verify(invokerAdapter, times(1)).invoke(any());
        orderedChain.verify(apiPlanFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
        orderedChain.verify(apiFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
        orderedChain.verify(apiPostProcessorChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
        orderedChain.verify(platformFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
    }

    @Test
    public void shouldHandleRequestWithLegacyInvoker() throws Exception {
        MutableRequestExecutionContext ctx = mock(MutableRequestExecutionContext.class);
        MutableRequest request = mock(MutableRequest.class);
        when(request.contextPath()).thenReturn("/contextPath");
        Metrics metrics = mock(Metrics.class);
        when(request.metrics()).thenReturn(metrics);
        when(ctx.request()).thenReturn(request);
        MutableResponse response = mock(MutableResponse.class);
        when(response.end()).thenReturn(Completable.complete());
        when(ctx.response()).thenReturn(response);
        when(ctx.componentProvider(any())).thenReturn(ctx);
        when(ctx.templateVariableProviders(any())).thenReturn(ctx);
        when(ctx.getAttribute(SKIP_SECURITY_CHAIN)).thenReturn(true);
        EndpointInvoker endpointInvoker = mock(EndpointInvoker.class);
        when(ctx.getAttribute(ATTR_INVOKER)).thenReturn(endpointInvoker);
        SyncApiReactor syncApiReactor = newSyncApiReactor();
        syncApiReactor.doStart();

        TestObserver<Void> handleRequestObserver = syncApiReactor.handle(ctx).test();

        handleRequestObserver.assertComplete();
        InOrder orderedChain = inOrder(
            platformFlowChain,
            apiPreProcessorChain,
            apiPlanFlowChain,
            apiFlowChain,
            apiPlanFlowChain,
            endpointInvoker,
            apiFlowChain,
            apiPostProcessorChain,
            platformFlowChain
        );
        orderedChain.verify(platformFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.REQUEST));
        orderedChain.verify(apiPreProcessorChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.REQUEST));
        orderedChain.verify(apiPlanFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.REQUEST));
        orderedChain.verify(apiFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.REQUEST));
        orderedChain.verify(endpointInvoker, times(1)).invoke(any(), any(), any());
        orderedChain.verify(apiPlanFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
        orderedChain.verify(apiFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
        orderedChain.verify(apiPostProcessorChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
        orderedChain.verify(platformFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
    }

    @Test
    public void shouldExecuteApiErrorProcessorChain() throws Exception {
        MutableRequestExecutionContext ctx = mock(MutableRequestExecutionContext.class);
        MutableRequest request = mock(MutableRequest.class);
        when(request.contextPath()).thenReturn("/contextPath");
        Metrics metrics = mock(Metrics.class);
        when(request.metrics()).thenReturn(metrics);
        when(ctx.request()).thenReturn(request);
        MutableResponse response = mock(MutableResponse.class);
        when(response.end()).thenReturn(Completable.complete());
        when(ctx.response()).thenReturn(response);
        when(ctx.componentProvider(any())).thenReturn(ctx);
        when(ctx.templateVariableProviders(any())).thenReturn(ctx);
        when(ctx.getAttribute(SKIP_SECURITY_CHAIN)).thenReturn(true);
        InvokerAdapter invokerAdapter = mock(InvokerAdapter.class);
        when(invokerAdapter.invoke(any())).thenReturn(Completable.error(new Throwable("Invoker error")));
        when(ctx.getAttribute(ATTR_INVOKER)).thenReturn(invokerAdapter);
        SyncApiReactor syncApiReactor = newSyncApiReactor();
        syncApiReactor.doStart();

        TestObserver<Void> handleRequestObserver = syncApiReactor.handle(ctx).test();

        handleRequestObserver.assertComplete();
        InOrder orderedChain = inOrder(
            platformFlowChain,
            apiPreProcessorChain,
            apiPlanFlowChain,
            apiFlowChain,
            apiPlanFlowChain,
            invokerAdapter,
            apiFlowChain,
            apiPostProcessorChain,
            apiErrorProcessorChain,
            platformFlowChain
        );
        orderedChain.verify(platformFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.REQUEST));
        orderedChain.verify(apiPreProcessorChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.REQUEST));
        orderedChain.verify(apiPlanFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.REQUEST));
        orderedChain.verify(apiFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.REQUEST));
        orderedChain.verify(invokerAdapter, times(1)).invoke(any());
        orderedChain.verify(apiPlanFlowChain, times(0)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
        orderedChain.verify(apiFlowChain, times(0)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
        orderedChain.verify(apiPostProcessorChain, times(0)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
        orderedChain.verify(apiErrorProcessorChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
        orderedChain.verify(platformFlowChain, times(0)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
    }

    @Test
    public void shouldExecuteApiPostProcessorChain_InterruptionException() throws Exception {
        MutableRequestExecutionContext ctx = mock(MutableRequestExecutionContext.class);
        MutableRequest request = mock(MutableRequest.class);
        when(request.contextPath()).thenReturn("/contextPath");
        Metrics metrics = mock(Metrics.class);
        when(request.metrics()).thenReturn(metrics);
        when(ctx.request()).thenReturn(request);
        MutableResponse response = mock(MutableResponse.class);
        when(response.end()).thenReturn(Completable.complete());
        when(ctx.response()).thenReturn(response);
        when(ctx.componentProvider(any())).thenReturn(ctx);
        when(ctx.templateVariableProviders(any())).thenReturn(ctx);
        when(ctx.getAttribute(SKIP_SECURITY_CHAIN)).thenReturn(true);
        InvokerAdapter invokerAdapter = mock(InvokerAdapter.class);
        when(invokerAdapter.invoke(any())).thenReturn(Completable.error(new InterruptionException()));
        when(ctx.getAttribute(ATTR_INVOKER)).thenReturn(invokerAdapter);
        SyncApiReactor syncApiReactor = newSyncApiReactor();
        syncApiReactor.doStart();

        TestObserver<Void> handleRequestObserver = syncApiReactor.handle(ctx).test();

        handleRequestObserver.assertComplete();
        InOrder orderedChain = inOrder(
            platformFlowChain,
            apiPreProcessorChain,
            apiPlanFlowChain,
            apiFlowChain,
            apiPlanFlowChain,
            invokerAdapter,
            apiFlowChain,
            apiPostProcessorChain,
            apiErrorProcessorChain,
            platformFlowChain
        );
        orderedChain.verify(platformFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.REQUEST));
        orderedChain.verify(apiPreProcessorChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.REQUEST));
        orderedChain.verify(apiPlanFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.REQUEST));
        orderedChain.verify(apiFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.REQUEST));
        orderedChain.verify(invokerAdapter, times(1)).invoke(any());
        orderedChain.verify(apiPlanFlowChain, times(0)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
        orderedChain.verify(apiFlowChain, times(0)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
        orderedChain.verify(apiPostProcessorChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
        orderedChain.verify(apiErrorProcessorChain, times(0)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
        orderedChain.verify(platformFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
    }

    @Test
    public void shouldExecuteApiErrorProcessorChain_InterruptionFailureException() throws Exception {
        MutableRequestExecutionContext ctx = mock(MutableRequestExecutionContext.class);
        MutableRequest request = mock(MutableRequest.class);
        when(request.contextPath()).thenReturn("/contextPath");
        Metrics metrics = mock(Metrics.class);
        when(request.metrics()).thenReturn(metrics);
        when(ctx.request()).thenReturn(request);
        MutableResponse response = mock(MutableResponse.class);
        when(response.end()).thenReturn(Completable.complete());
        when(ctx.response()).thenReturn(response);
        when(ctx.componentProvider(any())).thenReturn(ctx);
        when(ctx.templateVariableProviders(any())).thenReturn(ctx);
        when(ctx.getAttribute(SKIP_SECURITY_CHAIN)).thenReturn(true);
        InvokerAdapter invokerAdapter = mock(InvokerAdapter.class);
        when(invokerAdapter.invoke(any())).thenReturn(Completable.error(new InterruptionFailureException(mock(ExecutionFailure.class))));
        when(ctx.getAttribute(ATTR_INVOKER)).thenReturn(invokerAdapter);
        SyncApiReactor syncApiReactor = newSyncApiReactor();
        syncApiReactor.doStart();

        TestObserver<Void> handleRequestObserver = syncApiReactor.handle(ctx).test();

        handleRequestObserver.assertComplete();
        InOrder orderedChain = inOrder(
            platformFlowChain,
            apiPreProcessorChain,
            apiPlanFlowChain,
            apiFlowChain,
            apiPlanFlowChain,
            invokerAdapter,
            apiFlowChain,
            apiPostProcessorChain,
            apiErrorProcessorChain,
            platformFlowChain
        );
        orderedChain.verify(platformFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.REQUEST));
        orderedChain.verify(apiPreProcessorChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.REQUEST));
        orderedChain.verify(apiPlanFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.REQUEST));
        orderedChain.verify(apiFlowChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.REQUEST));
        orderedChain.verify(invokerAdapter, times(1)).invoke(any());
        orderedChain.verify(apiPlanFlowChain, times(0)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
        orderedChain.verify(apiFlowChain, times(0)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
        orderedChain.verify(apiPostProcessorChain, times(0)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
        orderedChain.verify(apiErrorProcessorChain, times(1)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
        orderedChain.verify(platformFlowChain, times(0)).execute(eq(ctx), eq(ExecutionPhase.RESPONSE));
    }
}
