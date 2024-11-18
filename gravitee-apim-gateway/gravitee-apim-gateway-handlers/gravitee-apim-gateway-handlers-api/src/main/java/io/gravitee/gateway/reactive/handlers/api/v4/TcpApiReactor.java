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

import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.REPORTERS_LOGGING_EXCLUDED_RESPONSE_TYPES_PROPERTY;
import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.REPORTERS_LOGGING_MAX_SIZE_PROPERTY;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.tracing.TracingHook;
import io.gravitee.gateway.reactive.core.v4.analytics.AnalyticsContext;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.core.v4.entrypoint.DefaultEntrypointConnectorResolver;
import io.gravitee.gateway.reactive.core.v4.invoker.HttpEndpointInvoker;
import io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.LoggingHook;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.gravitee.gateway.reactor.handler.DefaultTcpAcceptor;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */

@Slf4j
public class TcpApiReactor extends AbstractApiReactor {

    private final Node node;
    private final EndpointManager endpointManager;
    private final String loggingExcludedResponseType;
    private final String loggingMaxSize;
    private Lifecycle.State lifecycleState;
    private AnalyticsContext analyticsContext;

    public TcpApiReactor(
        Api api,
        Node node,
        Configuration configuration,
        DeploymentContext deploymentContext,
        EntrypointConnectorPluginManager entrypointConnectorPluginManager,
        EndpointManager endpointManager,
        RequestTimeoutConfiguration requestTimeoutConfiguration,
        TracingContext tracingContext
    ) {
        super(
            configuration,
            api,
            new DefaultEntrypointConnectorResolver(api.getDefinition(), deploymentContext, entrypointConnectorPluginManager),
            requestTimeoutConfiguration,
            tracingContext
        );
        this.node = node;
        this.endpointManager = endpointManager;
        this.defaultInvoker = new HttpEndpointInvoker(endpointManager);
        this.lifecycleState = Lifecycle.State.INITIALIZED;
        this.loggingExcludedResponseType =
            configuration.getProperty(REPORTERS_LOGGING_EXCLUDED_RESPONSE_TYPES_PROPERTY, String.class, null);
        this.loggingMaxSize = configuration.getProperty(REPORTERS_LOGGING_MAX_SIZE_PROPERTY, String.class, null);
    }

    @Override
    public Api api() {
        return this.api;
    }

    @Override
    ExecutionFailure noEntrypointFailure() {
        return new ExecutionFailure().message(NO_ENTRYPOINT_FAILURE_MESSAGE);
    }

    @Override
    public Completable handle(MutableExecutionContext ctx) {
        prepareCommonAttributes(ctx);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_TRACING_ENABLED, analyticsContext.isTracingEnabled());

        // TODO specific Tcp API Request processor chain factory that contains SubscriptionProcessor in beforeApi chain
        return new CompletableReactorChain(handleEntrypointRequest(ctx))
            // configure backend call
            .chainWith(invokeBackend(ctx))
            // setup timeout
            .chainWith(upstream -> timeout(upstream, ctx))
            // handle response
            .chainWith(handleEntrypointResponse(ctx))
            // hook everything up and start piping data
            .chainWith(ctx.response().end(ctx))
            .doOnSubscribe(disposable -> pendingRequests.incrementAndGet())
            .doFinally(pendingRequests::decrementAndGet);
    }

    @Override
    Completable onTimeout(MutableExecutionContext ctx) {
        return ctx.interruptWith(new ExecutionFailure().key(REQUEST_TIMEOUT_KEY).message("Request timeout"));
    }

    @Override
    public Lifecycle.State lifecycleState() {
        return lifecycleState;
    }

    @Override
    protected void doStart() throws Exception {
        long startTime = System.currentTimeMillis();
        endpointManager.start();

        analyticsContext = analyticsContext();

        tracingContext.start();
        analyticsContext = analyticsContext();
        if (analyticsContext.isEnabled()) {
            if (analyticsContext.isLoggingEnabled()) {
                invokerHooks.add(new LoggingHook());
            }
            if (analyticsContext.isTracingEnabled()) {
                invokerHooks.add(new TracingHook("invoker"));
            }
        }

        lifecycleState = Lifecycle.State.STARTED;
        long endTime = System.currentTimeMillis(); // Get the end Time
        log.debug("TCP API reactor started in {} ms", (endTime - startTime));
        dumpAcceptors();
    }

    protected void doStop() throws Exception {
        lifecycleState = Lifecycle.State.STOPPING;

        try {
            entrypointConnectorResolver.preStop();
            endpointManager.preStop();

            if (!node.lifecycleState().equals(Lifecycle.State.STARTED)) {
                log.debug("Current node is not started, API handler will be stopped immediately");
                stopNow();
            } else {
                log.debug("Current node is started, API handler will wait for pending requests before stopping");
                stopUntil().onErrorComplete().subscribe();
            }
        } catch (Exception e) {
            log.warn("An error occurred when trying to stop the TCP API reactor {}", this);
        }
    }

    @Override
    void stopNow() throws Exception {
        log.debug("TCP API reactor is now stopping, closing context for {} ...", this);

        entrypointConnectorResolver.stop();
        endpointManager.stop();
        tracingContext.stop();

        lifecycleState = Lifecycle.State.STOPPED;

        log.debug("TCP API reactor is now stopped: {}", this);
    }

    @Override
    @SuppressWarnings("java:S6204") // no using toList() as it messes with generics
    public List<Acceptor<?>> acceptors() {
        return api
            .getDefinition()
            .getListeners()
            .stream()
            .filter(TcpListener.class::isInstance)
            .map(l -> (TcpListener) l)
            .flatMap(listener -> listener.getHosts().stream().map(host -> new DefaultTcpAcceptor(this, host, listener.getServers())))
            .collect(Collectors.<Acceptor<?>>toList());
    }

    protected AnalyticsContext analyticsContext() {
        return new AnalyticsContext(api.getDefinition().getAnalytics(), loggingMaxSize, loggingExcludedResponseType, tracingContext);
    }

    @Override
    public String toString() {
        return "TcpApiReactor API id[" + api.getId() + "] name[" + api.getName() + "] version[" + api.getApiVersion() + ']';
    }
}
