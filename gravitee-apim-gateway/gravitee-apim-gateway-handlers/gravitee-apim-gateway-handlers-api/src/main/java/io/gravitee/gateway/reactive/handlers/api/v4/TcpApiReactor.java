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
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.*;
import static io.gravitee.gateway.reactive.handlers.api.v4.DefaultApiReactor.*;
import static io.reactivex.rxjava3.core.Completable.defer;
import static io.reactivex.rxjava3.core.Observable.interval;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.definition.model.v4.listener.tls.Tls;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.hook.InvokerHook;
import io.gravitee.gateway.reactive.api.invoker.Invoker;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.hook.HookHelper;
import io.gravitee.gateway.reactive.core.tracing.TracingHook;
import io.gravitee.gateway.reactive.core.v4.analytics.AnalyticsContext;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.core.v4.entrypoint.DefaultEntrypointConnectorResolver;
import io.gravitee.gateway.reactive.core.v4.invoker.EndpointInvoker;
import io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.LoggingHook;
import io.gravitee.gateway.reactive.handlers.api.v4.certificates.ApiKeyStoreLoaderManager;
import io.gravitee.gateway.reactive.handlers.api.v4.certificates.loaders.ApiKeyStoreLoader;
import io.gravitee.gateway.reactive.handlers.api.v4.certificates.loaders.ApiTrustStoreLoader;
import io.gravitee.gateway.reactive.reactor.ApiReactor;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.gravitee.gateway.reactor.handler.DefaultTcpAcceptor;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.api.server.ServerManager;
import io.gravitee.node.vertx.server.VertxServer;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.reactivex.rxjava3.core.Completable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */

@Slf4j
public class TcpApiReactor extends AbstractLifecycleComponent<ReactorHandler> implements ApiReactor<Api> {

    private final Api api;
    private final Node node;
    private final ApiKeyStoreLoaderManager apiKeyStoreLoaderManager;
    private final DefaultEntrypointConnectorResolver entrypointConnectorResolver;
    private final EndpointManager endpointManager;
    private final EndpointInvoker defaultInvoker;

    private final RequestTimeoutConfiguration requestTimeoutConfiguration;
    private final String loggingExcludedResponseType;
    private final String loggingMaxSize;
    private Lifecycle.State lifecycleState;
    private final AtomicLong pendingRequests = new AtomicLong(0);

    private final List<InvokerHook> invokerHooks = new ArrayList<>();
    private boolean tracingEnabled;
    private long pendingRequestsTimeout;

    public TcpApiReactor(
        Api api,
        Node node,
        Configuration configuration,
        DeploymentContext deploymentContext,
        ApiKeyStoreLoaderManager apiKeyStoreLoaderManager,
        EntrypointConnectorPluginManager entrypointConnectorPluginManager,
        EndpointManager endpointManager,
        RequestTimeoutConfiguration requestTimeoutConfiguration
    ) {
        this.api = api;
        this.node = node;
        this.apiKeyStoreLoaderManager = apiKeyStoreLoaderManager;
        this.requestTimeoutConfiguration = requestTimeoutConfiguration;
        this.entrypointConnectorResolver =
            new DefaultEntrypointConnectorResolver(api.getDefinition(), deploymentContext, entrypointConnectorPluginManager);
        this.endpointManager = endpointManager;
        this.defaultInvoker = new EndpointInvoker(endpointManager);
        this.tracingEnabled = configuration.getProperty(SERVICES_TRACING_ENABLED_PROPERTY, Boolean.class, false);
        this.pendingRequestsTimeout = configuration.getProperty(PENDING_REQUESTS_TIMEOUT_PROPERTY, Long.class, 10_000L);
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
    public Completable handle(MutableExecutionContext ctx) {
        // TODO: might need a bit of refactoring (duplication)
        ctx.setAttribute(ContextAttributes.ATTR_API, api.getId());
        ctx.setAttribute(ContextAttributes.ATTR_API_DEPLOYED_AT, api.getDeployedAt().getTime());
        ctx.setAttribute(ContextAttributes.ATTR_ORGANIZATION, api.getOrganizationId());
        ctx.setAttribute(ContextAttributes.ATTR_ENVIRONMENT, api.getEnvironmentId());
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_INVOKER, defaultInvoker);

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

    protected Completable handleEntrypointRequest(final MutableExecutionContext ctx) {
        // TODO: code is duplicated with ApiReactor
        return Completable.defer(() -> {
            final EntrypointConnector entrypointConnector = entrypointConnectorResolver.resolve(ctx);
            if (entrypointConnector == null) {
                return ctx.interruptWith(new ExecutionFailure().message("No entrypoint matches the incoming request"));
            }
            // Add the resolved entrypoint connector into the internal attributes, so it can be used later (ex: for endpoint connector resolution).
            ctx.setInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR, entrypointConnector);

            return entrypointConnector.handleRequest(ctx);
        });
    }

    protected Completable invokeBackend(final MutableExecutionContext ctx) {
        return defer(() ->
            getInvoker(ctx)
                .map(invoker -> HookHelper.hook(() -> invoker.invoke(ctx), invoker.getId(), invokerHooks, ctx, null))
                .orElse(Completable.complete())
        );
    }

    private Optional<Invoker> getInvoker(final MutableExecutionContext ctx) {
        if (ctx.getInternalAttribute(ATTR_INTERNAL_INVOKER) instanceof Invoker invoker) {
            return Optional.of(invoker);
        }
        return Optional.empty();
    }

    protected Completable handleEntrypointResponse(final MutableExecutionContext ctx) {
        // TODO: code is duplicated with ApiReactor
        return Completable
            .defer(() -> {
                if (ctx.getInternalAttribute(ATTR_INTERNAL_EXECUTION_FAILURE) == null) {
                    final EntrypointConnector entrypointConnector = ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR);
                    if (entrypointConnector != null) {
                        return entrypointConnector.handleResponse(ctx);
                    }
                }
                return Completable.complete();
            })
            .compose(upstream -> timeout(upstream, ctx));
    }

    protected Completable timeout(final Completable upstream, MutableExecutionContext ctx) {
        // TODO: code is duplicated with ApiReactor
        // When timeout is configured with 0 or less, consider it as infinity: no timeout operator to use in the chain.
        if (requestTimeoutConfiguration.getRequestTimeout() <= 0) {
            return upstream;
        }

        return Completable.defer(() ->
            upstream.timeout(
                Math.max(
                    requestTimeoutConfiguration.getRequestTimeoutGraceDelay(),
                    requestTimeoutConfiguration.getRequestTimeout() - (System.currentTimeMillis() - ctx.request().timestamp())
                ),
                TimeUnit.MILLISECONDS,
                ctx.interruptWith(new ExecutionFailure().key(REQUEST_TIMEOUT_KEY).message("Request timeout"))
            )
        );
    }

    @Override
    public Lifecycle.State lifecycleState() {
        return lifecycleState;
    }

    @Override
    protected void doStart() throws Exception {
        long startTime = System.currentTimeMillis();

        apiKeyStoreLoaderManager.start();

        endpointManager.start();
        if (tracingEnabled) {
            invokerHooks.add(new TracingHook("invoker"));
        }

        var analyticsContext = analyticsContext();
        if (analyticsContext.isEnabled() && (analyticsContext.isLoggingEnabled())) {
            invokerHooks.add(new LoggingHook());
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

    private Completable stopUntil() {
        // TODO: code is duplicated with ApiReactor
        return interval(STOP_UNTIL_INTERVAL_PERIOD_MS, TimeUnit.MILLISECONDS)
            .timestamp()
            .takeWhile(t -> pendingRequests.get() > 0 && (t.value() + 1) * STOP_UNTIL_INTERVAL_PERIOD_MS < pendingRequestsTimeout)
            .ignoreElements()
            .onErrorComplete()
            .doFinally(this::stopNow);
    }

    private void stopNow() throws Exception {
        log.debug("TCP API reactor is now stopping, closing context for {} ...", this);

        entrypointConnectorResolver.stop();
        endpointManager.stop();
        apiKeyStoreLoaderManager.stop();

        lifecycleState = Lifecycle.State.STOPPED;

        log.debug("TCP API reactor is now stopped: {}", this);
    }

    @Override
    public ReactorHandler stop() throws Exception {
        return this;
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
        return new AnalyticsContext(api.getDefinition().getAnalytics(), loggingMaxSize, loggingExcludedResponseType);
    }

    protected void dumpAcceptors() {
        // TODO: code is duplicated with ApiReactor
        List<Acceptor<?>> acceptors = acceptors();
        log.debug("{} ready to accept buffers on:", this);
        acceptors.forEach(acceptor -> log.debug("\t{}", acceptor));
    }

    @Override
    public String toString() {
        return "TcpApiReactor API id[" + api.getId() + "] name[" + api.getName() + "] version[" + api.getApiVersion() + ']';
    }
}
