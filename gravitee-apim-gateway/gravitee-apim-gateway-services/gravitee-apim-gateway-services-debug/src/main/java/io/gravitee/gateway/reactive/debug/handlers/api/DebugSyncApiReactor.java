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
import io.gravitee.gateway.debug.definition.DebugApi;
import io.gravitee.gateway.debug.reactor.handler.context.PathTransformer;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactive.api.invoker.Invoker;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.debug.invoker.DebugInvokerHook;
import io.gravitee.gateway.reactive.debug.policy.DebugPolicyHook;
import io.gravitee.gateway.reactive.handlers.api.SyncApiReactor;
import io.gravitee.gateway.reactive.handlers.api.flow.FlowChainFactory;
import io.gravitee.gateway.reactive.handlers.api.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.gravitee.gateway.reactor.handler.DefaultHttpAcceptor;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.api.opentelemetry.Tracer;
import io.gravitee.node.opentelemetry.OpenTelemetryFactory;
import io.reactivex.rxjava3.core.Completable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugSyncApiReactor extends SyncApiReactor {

    public DebugSyncApiReactor(
        final Api api,
        final ComponentProvider componentProvider,
        final List<TemplateVariableProvider> templateVariableProviders,
        final Invoker defaultInvoker,
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
            tracingContext
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
        String cleanContextPath = PathTransformer.removeEventIdFromPath(((DebugApi) api).getEventId(), debugContextPath);
        ctx.request().contextPath(cleanContextPath);

        return super.handle(ctx);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        securityChain.addHooks(new DebugPolicyHook());
    }

    @Override
    public List<Acceptor<?>> acceptors() {
        try {
            return api
                .getDefinition()
                .getProxy()
                .getVirtualHosts()
                .stream()
                .map(virtualHost -> new DefaultHttpAcceptor(null, virtualHost.getPath(), this, null))
                .collect(Collectors.toList());
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }
}
