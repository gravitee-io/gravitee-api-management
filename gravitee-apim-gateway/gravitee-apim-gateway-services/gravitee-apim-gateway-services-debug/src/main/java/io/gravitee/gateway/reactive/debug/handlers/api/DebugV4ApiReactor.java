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
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.debug.definition.ReactableDebugApi;
import io.gravitee.gateway.debug.reactor.handler.context.PathTransformer;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.debug.invoker.DebugInvokerHook;
import io.gravitee.gateway.reactive.debug.policy.DebugPolicyHook;
import io.gravitee.gateway.reactive.handlers.api.flow.FlowChainFactory;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.reactive.handlers.api.v4.ApiProductPlanPolicyManagerFactory;
import io.gravitee.gateway.reactive.handlers.api.v4.DefaultApiReactor;
import io.gravitee.gateway.reactive.handlers.api.v4.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.gravitee.gateway.reactor.handler.DefaultHttpAcceptor;
import io.gravitee.gateway.reactor.handler.HttpAcceptorFactory;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.gateway.report.guard.LogGuardService;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.apiservice.ApiServicePluginManager;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.reactivex.rxjava3.core.Completable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DebugV4ApiReactor extends DefaultApiReactor {

    public DebugV4ApiReactor(
        Api api,
        DeploymentContext deploymentContext,
        ComponentProvider componentProvider,
        List<TemplateVariableProvider> ctxTemplateVariableProviders,
        PolicyManager policyManager,
        EntrypointConnectorPluginManager entrypointConnectorPluginManager,
        ApiServicePluginManager apiServicePluginManager,
        EndpointManager endpointManager,
        ResourceLifecycleManager resourceLifecycleManager,
        ApiProcessorChainFactory apiProcessorChainFactory,
        FlowChainFactory flowChainFactory,
        io.gravitee.gateway.reactive.handlers.api.v4.flow.FlowChainFactory v4FlowChainFactory,
        Configuration configuration,
        Node node,
        RequestTimeoutConfiguration requestTimeoutConfiguration,
        ReporterService reporterService,
        AccessPointManager accessPointManager,
        EventManager eventManager,
        HttpAcceptorFactory httpAcceptorFactory,
        TracingContext tracingContext,
        LogGuardService logGuardService,
        ApiProductRegistry apiProductRegistry,
        ApiProductPlanPolicyManagerFactory apiProductPlanPolicyManagerFactory
    ) {
        super(
            api,
            deploymentContext,
            componentProvider,
            ctxTemplateVariableProviders,
            policyManager,
            entrypointConnectorPluginManager,
            apiServicePluginManager,
            endpointManager,
            resourceLifecycleManager,
            apiProcessorChainFactory,
            flowChainFactory,
            v4FlowChainFactory,
            configuration,
            node,
            requestTimeoutConfiguration,
            reporterService,
            accessPointManager,
            eventManager,
            httpAcceptorFactory,
            tracingContext,
            logGuardService,
            apiProductRegistry,
            apiProductPlanPolicyManagerFactory
        );
        invokerHooks.add(new DebugInvokerHook());
    }

    @Override
    public Completable handle(final MutableExecutionContext ctx) {
        /*
         * Debug path contains a generated uuid to isolate each debug request.
         * The code bellow remove this generated uuid from both context path, path and pathInfo and override request attributes to be sure the gateway find the right api
         */
        String debugContextPath = ctx.request().contextPath();
        String cleanContextPath = PathTransformer.removeEventIdFromPath(((ReactableDebugApi<?>) api).getEventId(), debugContextPath);
        ctx.request().debugContextPath(cleanContextPath);

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
                    .getListeners()
                    .stream()
                    .filter(l -> l.getType() == io.gravitee.definition.model.v4.listener.ListenerType.HTTP)
                    .flatMap(l -> ((HttpListener) l).getPaths().stream())
                    .map(p -> new DefaultHttpAcceptor(null, p.getPath(), this, null))
                    .collect(Collectors.toList());
            }
            return acceptors;
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }
}
