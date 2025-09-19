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
package io.gravitee.gateway.reactive.debug.handlers.api;

import io.gravitee.common.event.EventManager;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.endpoint.lifecycle.GroupLifecycleManager;
import io.gravitee.gateway.debug.definition.DebugApiV2;
import io.gravitee.gateway.debug.reactor.handler.context.PathTransformer;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactive.api.hook.ChainHook;
import io.gravitee.gateway.reactive.api.hook.InvokerHook;
import io.gravitee.gateway.reactive.api.invoker.HttpInvoker;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.processor.ProcessorChain;
import io.gravitee.gateway.reactive.core.v4.analytics.AnalyticsContext;
import io.gravitee.gateway.reactive.debug.invoker.DebugInvokerHook;
import io.gravitee.gateway.reactive.debug.policy.DebugPolicyHook;
import io.gravitee.gateway.reactive.handlers.api.SyncApiReactor;
import io.gravitee.gateway.reactive.handlers.api.flow.FlowChain;
import io.gravitee.gateway.reactive.handlers.api.flow.FlowChainFactory;
import io.gravitee.gateway.reactive.handlers.api.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.reactive.handlers.api.security.HttpSecurityChain;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.gravitee.gateway.reactor.handler.DefaultHttpAcceptor;
import io.gravitee.gateway.reactor.handler.HttpAcceptorFactory;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.reactivex.rxjava3.core.Completable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */

public class DebugSyncApiReactor extends SyncApiReactor {

    public DebugSyncApiReactor(
        final Api api,
        final ComponentProvider componentProvider,
        final List<TemplateVariableProvider> templateVariableProviders,
        final HttpInvoker defaultInvoker,
        final ResourceLifecycleManager resourceLifecycleManager,
        final ApiProcessorChainFactory apiProcessorChainFactory,
        final PolicyManager policyManager,
        final FlowChainFactory flowChainFactory,
        final GroupLifecycleManager groupLifecycleManager,
        final Configuration configuration,
        final Node node,
        final RequestTimeoutConfiguration requestTimeoutConfiguration,
        final AccessPointManager accessPointManager,
        final EventManager eventManager,
        final HttpAcceptorFactory httpAcceptorFactory,
        final TracingContext tracingContext
    ) {
        super(
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
            httpAcceptorFactory,
            tracingContext,
            null
        );
        invokerHooks.add(new DebugInvokerHook());
    }

    @Builder(access = AccessLevel.MODULE)
    DebugSyncApiReactor(
        Api api,
        List<ChainHook> processorChainHooks,
        List<InvokerHook> invokerHooks,
        ComponentProvider componentProvider,
        List<TemplateVariableProvider> templateVariableProviders,
        HttpInvoker defaultInvoker,
        ResourceLifecycleManager resourceLifecycleManager,
        PolicyManager policyManager,
        GroupLifecycleManager groupLifecycleManager,
        FlowChain organizationFlowChain,
        FlowChain apiPlanFlowChain,
        FlowChain apiFlowChain,
        ProcessorChain beforeHandleProcessors,
        ProcessorChain afterHandleProcessors,
        ProcessorChain beforeSecurityChainProcessors,
        ProcessorChain beforeApiFlowsProcessors,
        ProcessorChain afterApiFlowsProcessors,
        ProcessorChain onErrorProcessors,
        Configuration configuration,
        Node node,
        RequestTimeoutConfiguration requestTimeoutConfiguration,
        AccessPointManager accessPointManager,
        EventManager eventManager,
        TracingContext tracingContext,
        String loggingExcludedResponseType,
        String loggingMaxSize,
        HttpAcceptorFactory httpAcceptorFactory,
        long pendingRequestsTimeout,
        AnalyticsContext analyticsContext,
        HttpSecurityChain httpSecurityChain,
        List<Acceptor<?>> acceptors
    ) {
        super(
            api,
            processorChainHooks,
            invokerHooks,
            componentProvider,
            templateVariableProviders,
            defaultInvoker,
            resourceLifecycleManager,
            policyManager,
            groupLifecycleManager,
            organizationFlowChain,
            apiPlanFlowChain,
            apiFlowChain,
            beforeHandleProcessors,
            afterHandleProcessors,
            beforeSecurityChainProcessors,
            beforeApiFlowsProcessors,
            afterApiFlowsProcessors,
            onErrorProcessors,
            configuration,
            node,
            requestTimeoutConfiguration,
            accessPointManager,
            eventManager,
            tracingContext,
            loggingExcludedResponseType,
            loggingMaxSize,
            httpAcceptorFactory,
            pendingRequestsTimeout,
            analyticsContext,
            httpSecurityChain,
            acceptors,
            null
        );
    }

    void handleProcess(final MutableExecutionContext ctx) {
        String debugContextPath = ctx.request().contextPath();
        String cleanContextPath = PathTransformer.removeEventIdFromPath(((DebugApiV2) api).getEventId(), debugContextPath);
        ctx.request().debugContextPath(cleanContextPath);
    }

    @Override
    public Completable handle(final MutableExecutionContext ctx) {
        /*
         * Debug path contains a generated uuid to isolate each debug request.
         * The code bellow remove this generated uuid from both context path, path and pathInfo and override request attributes to be sure the gateway find the right api
         */
        handleProcess(ctx);
        return super.handle(ctx);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        httpSecurityChain.addHooks(new DebugPolicyHook());
    }

    @Override
    public List<Acceptor<?>> acceptors() {
        try {
            if (acceptors == null) {
                acceptors = api
                    .getDefinition()
                    .getProxy()
                    .getVirtualHosts()
                    .stream()
                    .map(virtualHost -> new DefaultHttpAcceptor(null, virtualHost.getPath(), this, null))
                    .collect(Collectors.toList());
            }
            return acceptors;
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }
}
