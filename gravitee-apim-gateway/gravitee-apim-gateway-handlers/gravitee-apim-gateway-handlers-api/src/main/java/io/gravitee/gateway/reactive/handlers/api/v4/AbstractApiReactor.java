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
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.invoker.Invoker;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.v4.entrypoint.DefaultEntrypointConnectorResolver;
import io.gravitee.gateway.reactive.reactor.ApiReactor;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.node.api.configuration.Configuration;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractApiReactor extends AbstractLifecycleComponent<ReactorHandler> implements ApiReactor<Api> {

    public static final int STOP_UNTIL_INTERVAL_PERIOD_MS = 100;
    public static final String PENDING_REQUESTS_TIMEOUT_PROPERTY = "api.pending_requests_timeout";
    public static final String REQUEST_TIMEOUT_KEY = "REQUEST_TIMEOUT";
    public static final String REQUEST_TIMEOUT_MESSAGE = "Request timeout";
    public static final String NO_ENTRYPOINT_FAILURE_MESSAGE = "No entrypoint matches the incoming request";

    protected final Api api;
    protected final DefaultEntrypointConnectorResolver entrypointConnectorResolver;
    protected final AtomicLong pendingRequests = new AtomicLong(0);
    protected Invoker defaultInvoker;
    private final RequestTimeoutConfiguration requestTimeoutConfiguration;
    private final long pendingRequestsTimeout;

    AbstractApiReactor(
        Configuration configuration,
        Api api,
        DefaultEntrypointConnectorResolver entrypointConnectorResolver,
        RequestTimeoutConfiguration requestTimeoutConfiguration
    ) {
        this.api = api;
        this.entrypointConnectorResolver = entrypointConnectorResolver;
        this.requestTimeoutConfiguration = requestTimeoutConfiguration;
        this.pendingRequestsTimeout = configuration.getProperty(PENDING_REQUESTS_TIMEOUT_PROPERTY, Long.class, 10_000L);
    }

    abstract ExecutionFailure noEntrypointFailure();

    protected Completable handleEntrypointRequest(final MutableExecutionContext ctx) {
        return Completable.defer(() -> {
            final EntrypointConnector entrypointConnector = entrypointConnectorResolver.resolve(ctx);
            if (entrypointConnector == null) {
                return ctx.interruptWith(noEntrypointFailure());
            }
            // Add the resolved entrypoint connector into the internal attributes, so it can be used later (ex: for endpoint connector resolution).
            ctx.setInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR, entrypointConnector);

            return entrypointConnector.handleRequest(ctx);
        });
    }

    protected Completable handleEntrypointResponse(final MutableExecutionContext ctx) {
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

    protected Completable stopUntil() {
        return interval(STOP_UNTIL_INTERVAL_PERIOD_MS, TimeUnit.MILLISECONDS)
            .timestamp()
            .takeWhile(t -> pendingRequests.get() > 0 && (t.value() + 1) * STOP_UNTIL_INTERVAL_PERIOD_MS < pendingRequestsTimeout)
            .ignoreElements()
            .onErrorComplete()
            .doFinally(this::stopNow);
    }

    protected void prepareCommonAttributes(MutableExecutionContext ctx) {
        ctx.setAttribute(ContextAttributes.ATTR_API, api.getId());
        ctx.setAttribute(ContextAttributes.ATTR_API_NAME, api.getName());
        ctx.setAttribute(ContextAttributes.ATTR_API_DEPLOYED_AT, api.getDeployedAt().getTime());
        ctx.setAttribute(ContextAttributes.ATTR_ORGANIZATION, api.getOrganizationId());
        ctx.setAttribute(ContextAttributes.ATTR_ENVIRONMENT, api.getEnvironmentId());
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_INVOKER, defaultInvoker);
    }

    protected void dumpAcceptors() {
        List<Acceptor<?>> acceptors = acceptors();
        log.debug("{} ready to accept traffic on:", this);
        acceptors.forEach(acceptor -> log.debug("\t{}", acceptor));
    }
}
