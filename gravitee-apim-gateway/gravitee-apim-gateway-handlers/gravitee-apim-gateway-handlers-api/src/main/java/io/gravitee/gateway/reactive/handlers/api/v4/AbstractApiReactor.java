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

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE;
import static io.reactivex.rxjava3.core.Observable.interval;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.connector.entrypoint.BaseEntrypointConnector;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.hook.InvokerHook;
import io.gravitee.gateway.reactive.api.invoker.HttpInvoker;
import io.gravitee.gateway.reactive.api.tracing.Tracer;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.v4.entrypoint.DefaultEntrypointConnectorResolver;
import io.gravitee.gateway.reactive.reactor.ApiReactor;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.node.api.configuration.Configuration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.CustomLog;

@CustomLog
public abstract class AbstractApiReactor extends AbstractLifecycleComponent<ReactorHandler> implements ApiReactor<Api> {

    public static final int STOP_UNTIL_INTERVAL_PERIOD_MS = 100;
    public static final String PENDING_REQUESTS_TIMEOUT_PROPERTY = "api.pending_requests_timeout";
    public static final String REQUEST_TIMEOUT_KEY = "REQUEST_TIMEOUT";
    public static final String REQUEST_TIMEOUT_MESSAGE = "Request timeout";
    public static final String NO_ENTRYPOINT_FAILURE_MESSAGE = "No entrypoint matches the incoming request";

    protected final Configuration configuration;
    protected final Api api;
    protected final DefaultEntrypointConnectorResolver entrypointConnectorResolver;
    protected final AtomicLong pendingRequests = new AtomicLong(0);
    protected final TracingContext tracingContext;
    protected HttpInvoker defaultInvoker;
    private final RequestTimeoutConfiguration requestTimeoutConfiguration;
    private final long pendingRequestsTimeout;
    protected final List<InvokerHook> invokerHooks = new ArrayList<>();

    AbstractApiReactor(
        Configuration configuration,
        Api api,
        DefaultEntrypointConnectorResolver entrypointConnectorResolver,
        RequestTimeoutConfiguration requestTimeoutConfiguration,
        TracingContext tracingContext
    ) {
        this.configuration = configuration;
        this.api = api;
        this.entrypointConnectorResolver = entrypointConnectorResolver;
        this.requestTimeoutConfiguration = requestTimeoutConfiguration;
        this.pendingRequestsTimeout = configuration.getProperty(PENDING_REQUESTS_TIMEOUT_PROPERTY, Long.class, 10_000L);
        this.tracingContext = tracingContext;
    }

    @Override
    public TracingContext tracingContext() {
        return tracingContext;
    }

    abstract ExecutionFailure noEntrypointFailure();

    protected <C extends BaseExecutionContext> Completable handleEntrypointRequest(final MutableExecutionContext ctx) {
        return Completable.defer(() -> {
            final BaseEntrypointConnector<C> entrypointConnector = entrypointConnectorResolver.resolve((C) ctx);
            if (entrypointConnector == null) {
                return ctx.interruptWith(noEntrypointFailure());
            }
            // Add the resolved entrypoint connector into the internal attributes, so it can be used later (ex: for endpoint connector resolution).
            ctx.setInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR, entrypointConnector);

            // Record which entrypoint served the request on the root span. Deferred so it lands on the span
            // owned by the dispatcher, and stamped generically here so every API type gets it.
            final Tracer tracer = ctx.getTracer();
            if (tracer != null) {
                tracer.deferRootSpanAttribute("gravitee.entrypoint.id", entrypointConnector.id());
            }

            return entrypointConnector.handleRequest((C) ctx);
        });
    }

    protected Completable handleEntrypointResponse(final MutableExecutionContext ctx) {
        return Completable.defer(() -> {
            if (ctx.getInternalAttribute(ATTR_INTERNAL_EXECUTION_FAILURE) == null) {
                final BaseEntrypointConnector entrypointConnector = ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR);
                if (entrypointConnector != null) {
                    return entrypointConnector.handleResponse(ctx);
                }
            }
            return Completable.complete();
        }).compose(upstream -> timeout(upstream, ctx));
    }

    protected ExecutionPhase endpointExecutionPhase() {
        return ExecutionPhase.REQUEST;
    }

    abstract Completable onTimeout(MutableExecutionContext ctx);

    protected Completable timeout(final Completable upstream, MutableExecutionContext ctx) {
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
                onTimeout(ctx)
            )
        );
    }

    abstract void stopNow() throws Exception;

    /**
     * Warn when the reactor is torn down while requests are still running. Stopping closes the endpoint connections,
     * the policies and the resources from under them, so those requests end with an upstream error — an outcome that
     * looks like a runtime fault in analytics but is really the reactor going away (an API redeploy, a node shutdown,
     * a scale down).
     * <p>
     * Without this line there is nothing in the logs tying the two together, and the error rate simply appears to
     * spike for no reason at the same instant as every deployment.
     *
     * @param stillRunning how many requests were running when the decision to stop anyway was taken. It has to be
     *   captured at that point and passed in: by the time the teardown actually runs, the counter no longer reflects
     *   the requests about to be cut.
     */
    protected void warnAboutRequestsCutShort(final long stillRunning) {
        if (stillRunning > 0) {
            log.warn(
                "API reactor is stopping while {} request(s) are still being handled: they will be cut short and " +
                    "reported as errors. Waited up to {} ms for them ({}). [{}]",
                stillRunning,
                pendingRequestsTimeout,
                PENDING_REQUESTS_TIMEOUT_PROPERTY,
                this
            );
        }
    }

    protected Completable stopUntil() {
        // Remembers what the loop last saw, because that is the only place the count is meaningful: the predicate
        // returns false either because the requests are gone (0) or because the grace period expired while they were
        // still running (> 0) — and only the second case is worth a warning. Reading the counter after the loop, in
        // the teardown itself, always yields 0.
        final AtomicLong lastSeenPendingRequests = new AtomicLong();

        return interval(STOP_UNTIL_INTERVAL_PERIOD_MS, TimeUnit.MILLISECONDS)
            .timestamp()
            .observeOn(Schedulers.io())
            .takeWhile(t -> {
                final long stillRunning = pendingRequests.get();
                lastSeenPendingRequests.set(stillRunning);
                return stillRunning > 0 && (t.value() + 1) * STOP_UNTIL_INTERVAL_PERIOD_MS < pendingRequestsTimeout;
            })
            .ignoreElements()
            .onErrorComplete()
            .doFinally(() -> {
                warnAboutRequestsCutShort(lastSeenPendingRequests.get());
                stopNow();
            });
    }

    protected void prepareCommonAttributes(MutableExecutionContext ctx) {
        ctx.setAttribute(ContextAttributes.ATTR_API, api.getId());
        ctx.setAttribute(ContextAttributes.ATTR_API_NAME, api.getName());
        ctx.setAttribute(ContextAttributes.ATTR_API_DEPLOYED_AT, api.getDeployedAt().getTime());
        ctx.setAttribute(ContextAttributes.ATTR_ORGANIZATION, api.getOrganizationId());
        ctx.setAttribute(ContextAttributes.ATTR_ENVIRONMENT, api.getEnvironmentId());
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_API_TYPE, api.getDefinition().getType().name());
    }

    protected void dumpAcceptors() {
        List<Acceptor<?>> acceptors = acceptors();
        log.debug("{} ready to accept traffic on:", this);
        acceptors.forEach(acceptor -> log.debug("\t{}", acceptor));
    }
}
