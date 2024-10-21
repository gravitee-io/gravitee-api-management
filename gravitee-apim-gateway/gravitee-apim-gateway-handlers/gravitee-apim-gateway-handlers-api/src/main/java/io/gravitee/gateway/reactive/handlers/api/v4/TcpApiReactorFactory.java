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

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactive.core.context.DefaultDeploymentContext;
import io.gravitee.gateway.reactive.core.v4.analytics.AnalyticsUtils;
import io.gravitee.gateway.reactive.core.v4.endpoint.DefaultEndpointManager;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.api.opentelemetry.InstrumenterTracerFactory;
import io.gravitee.node.api.opentelemetry.Tracer;
import io.gravitee.node.opentelemetry.OpenTelemetryFactory;
import io.gravitee.node.opentelemetry.configuration.OpenTelemetryConfiguration;
import io.gravitee.node.opentelemetry.tracer.noop.NoOpTracer;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TcpApiReactorFactory implements ReactorFactory<Api> {

    private final Configuration configuration;
    private final Node node;
    private final EntrypointConnectorPluginManager entrypointConnectorPluginManager;
    private final EndpointConnectorPluginManager endpointConnectorPluginManager;
    private final RequestTimeoutConfiguration requestTimeoutConfiguration;
    private final OpenTelemetryConfiguration openTelemetryConfiguration;
    private final OpenTelemetryFactory openTelemetryFactory;
    private final List<InstrumenterTracerFactory> instrumenterTracerFactories;

    @Override
    public boolean support(final Class<? extends Reactable> clazz) {
        return Api.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canCreate(Api api) {
        // Check that the API contains at least one subscription listener.
        return (
            api.getDefinitionVersion() == DefinitionVersion.V4 &&
            api.getDefinition().getType() == ApiType.PROXY &&
            api.getDefinition().getListeners().stream().anyMatch(listener -> listener.getType() == ListenerType.TCP)
        );
    }

    @Override
    public ReactorHandler create(Api api) {
        DefaultDeploymentContext deploymentContext = new DefaultDeploymentContext();
        final DefaultEndpointManager endpointManager = new DefaultEndpointManager(
            api.getDefinition(),
            endpointConnectorPluginManager,
            deploymentContext
        );

        return new TcpApiReactor(
            api,
            node,
            configuration,
            deploymentContext,
            entrypointConnectorPluginManager,
            endpointManager,
            requestTimeoutConfiguration,
            createTracingContext(api)
        );
    }

    protected TracingContext createTracingContext(final Api api) {
        if (isApiTracingEnabled(api)) {
            Tracer tracer = openTelemetryFactory.createTracer(
                api.getId(),
                api.getName(),
                "API_V4_TCP",
                api.getApiVersion(),
                instrumenterTracerFactories
            );
            return new TracingContext(tracer, isApiTracingEnabled(api), isApiTracingVerboseEnabled(api));
        } else {
            return TracingContext.noop();
        }
    }

    protected boolean isApiTracingEnabled(final Api api) {
        return AnalyticsUtils.isTracingEnabled(
            openTelemetryConfiguration,
            api.getDefinition() != null ? api.getDefinition().getAnalytics() : null
        );
    }

    protected boolean isApiTracingVerboseEnabled(final Api api) {
        return AnalyticsUtils.isTracingVerbose(
            openTelemetryConfiguration,
            api.getDefinition() != null ? api.getDefinition().getAnalytics() : null
        );
    }
}
