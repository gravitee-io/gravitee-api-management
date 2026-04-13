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
import static io.gravitee.gateway.reactive.api.context.ContextAttributes.ATTR_CONTEXT_PATH;
import static io.gravitee.gateway.reactive.api.context.ContextAttributes.ATTR_REQUEST_METHOD;
import static io.gravitee.gateway.reactive.api.context.ContextAttributes.ATTR_SNI;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_SERVER_ID;
import static io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessor.CLIENT_IDENTIFIER_HEADER_PROPERTY;
import static io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessor.DEFAULT_CLIENT_IDENTIFIER_HEADER;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.connector.Connector;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.tcp.TcpExecutionContext;
import io.gravitee.gateway.reactive.api.invoker.TcpInvoker;
import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.tracing.TracingHook;
import io.gravitee.gateway.reactive.core.v4.analytics.AnalyticsContext;
import io.gravitee.gateway.reactive.core.v4.analytics.LoggingContext;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.core.v4.entrypoint.DefaultEntrypointConnectorResolver;
import io.gravitee.gateway.reactive.core.v4.invoker.TcpEndpointInvoker;
import io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessor;
import io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.LoggingHook;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.gravitee.gateway.reactor.handler.DefaultTcpAcceptor;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.logging.LogEntry;
import io.gravitee.node.logging.LogEntryFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.gravitee.reporter.api.v4.metric.NoopMetrics;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */

@CustomLog
public class TcpApiReactor extends AbstractApiReactor {

    // TODO: as a temporary solution, we use HttpExecutionContextInternal, but it would be better to have a TcpExecutionContextInternal someday
    private static final Set<LogEntry<? extends HttpExecutionContextInternal>> DEFAULT_EXECUTION_CONTEXT_LOG_ENTRIES = Set.of(
        LogEntryFactory.cached("serverId", DefaultExecutionContext.class, context -> context.getInternalAttribute(ATTR_INTERNAL_SERVER_ID)),
        LogEntryFactory.cached("sni", DefaultExecutionContext.class, context -> context.getAttribute(ATTR_SNI))
    );

    private final Node node;
    private final EndpointManager endpointManager;
    private final String loggingExcludedResponseType;
    private final String loggingMaxSize;
    private Lifecycle.State lifecycleState;
    private AnalyticsContext analyticsContext;
    private final TcpInvoker defaultInvoker;
    private final GatewayConfiguration gatewayConfiguration;
    private final ReporterService reporterService;
    private final String clientIdentifierHeader;

    public TcpApiReactor(
        Api api,
        Node node,
        Configuration configuration,
        DeploymentContext deploymentContext,
        EntrypointConnectorPluginManager entrypointConnectorPluginManager,
        EndpointManager endpointManager,
        RequestTimeoutConfiguration requestTimeoutConfiguration,
        TracingContext tracingContext,
        GatewayConfiguration gatewayConfiguration,
        ReporterService reporterService
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
        this.defaultInvoker = new TcpEndpointInvoker(endpointManager);
        this.lifecycleState = Lifecycle.State.INITIALIZED;
        this.loggingExcludedResponseType = configuration.getProperty(
            REPORTERS_LOGGING_EXCLUDED_RESPONSE_TYPES_PROPERTY,
            String.class,
            null
        );
        this.loggingMaxSize = configuration.getProperty(REPORTERS_LOGGING_MAX_SIZE_PROPERTY, String.class, null);
        this.gatewayConfiguration = gatewayConfiguration;
        this.reporterService = reporterService;
        this.clientIdentifierHeader = configuration.getProperty(
            CLIENT_IDENTIFIER_HEADER_PROPERTY,
            String.class,
            DEFAULT_CLIENT_IDENTIFIER_HEADER
        );
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
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_TRACING_VERBOSE_ENABLED, tracingContext.isVerbose());
        ctx.logEntries(DEFAULT_EXECUTION_CONTEXT_LOG_ENTRIES);
        initMetrics(ctx);
        prepareMetrics(ctx);

        return new CompletableReactorChain(handleEntrypointRequest(ctx))
            .chainWith(SubscriptionProcessor.instance(clientIdentifierHeader).execute(ctx))
            .chainWith(reportConnectionAccepted(ctx))
            .chainWith(defaultInvoker.invoke(ctx))
            .chainWith(upstream -> timeout(upstream, ctx))
            .chainWith(handleEntrypointResponse(ctx))
            .chainWith(ctx.response().end(ctx))
            .doOnSubscribe(disposable -> pendingRequests.incrementAndGet())
            .doFinally(pendingRequests::decrementAndGet);
    }

    private void initMetrics(MutableExecutionContext ctx) {
        if (!analyticsContext.isEnabled()) {
            ctx.metrics(new NoopMetrics());
            return;
        }
        var request = ctx.request();
        Metrics.MetricsBuilder<?, ?> builder = Metrics.builder()
            .timestamp(request.timestamp())
            .requestId(request.id())
            .transactionId(request.transactionId())
            .remoteAddress(request.remoteAddress())
            .localAddress(request.localAddress())
            .host(request.host());
        gatewayConfiguration.tenant().ifPresent(builder::tenant);
        gatewayConfiguration.zone().ifPresent(builder::zone);
        ctx.metrics(builder.enabled(true).build());
    }

    private void prepareMetrics(MutableExecutionContext ctx) {
        Metrics metrics = ctx.metrics();
        if (metrics.isEnabled()) {
            metrics.setApiId(api.getId());
            metrics.setApiName(api.getName());
            metrics.setApiType(api.getDefinition().getType().getLabel());
            metrics.setOrganizationId(api.getOrganizationId());
            metrics.setEnvironmentId(api.getEnvironmentId());
        }
    }

    private Completable reportConnectionAccepted(MutableExecutionContext ctx) {
        return Completable.fromRunnable(() -> {
            Metrics metrics = ctx.metrics();
            if (metrics.isEnabled()) {
                Connector connector = ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR);
                if (connector != null) {
                    metrics.setEntrypointId(connector.id());
                }
                reporterService.report(metrics);
            }
        })
            .doOnError(t -> ctx.withLogger(log).warn("An error occurred while reporting connection metrics", t))
            // Reporting failures must never abort a TCP connection — swallowing intentionally.
            .onErrorComplete();
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

        tracingContext.start();
        analyticsContext = createAnalyticsContext();
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
            .flatMap(listener ->
                listener
                    .getHosts()
                    .stream()
                    .map(host -> new DefaultTcpAcceptor(this, host, listener.getServers()))
            )
            .collect(Collectors.<Acceptor<?>>toList());
    }

    protected AnalyticsContext createAnalyticsContext() {
        LoggingContext loggingContext = Optional.ofNullable(api.getDefinition().getAnalytics())
            .map(analytics -> {
                var context = new LoggingContext(analytics.getLogging());
                context.setMaxSizeLogMessage(loggingMaxSize);
                context.setExcludedResponseTypes(loggingExcludedResponseType);
                return context;
            })
            .orElse(null);
        return new AnalyticsContext(api.getDefinition().getAnalytics(), loggingContext, tracingContext);
    }

    @Override
    public String toString() {
        return "TcpApiReactor API id[" + api.getId() + "] name[" + api.getName() + "] version[" + api.getApiVersion() + ']';
    }
}
