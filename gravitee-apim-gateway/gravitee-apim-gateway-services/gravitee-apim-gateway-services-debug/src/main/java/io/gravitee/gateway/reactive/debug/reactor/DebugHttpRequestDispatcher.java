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
package io.gravitee.gateway.reactive.debug.reactor;

import io.gravitee.common.http.IdGenerator;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.debug.definition.ReactableDebugApi;
import io.gravitee.gateway.debug.handlers.api.DebugApiReactorHandler;
import io.gravitee.gateway.debug.vertx.VertxHttpServerRequestDebugDecorator;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.env.RequestClientAuthConfiguration;
import io.gravitee.gateway.env.RequestPathConfiguration;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.http.vertx.VertxHttpServerRequestOptions;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.processor.ProcessorChain;
import io.gravitee.gateway.reactive.debug.reactor.context.DebugExecutionContext;
import io.gravitee.gateway.reactive.debug.reactor.processor.DebugCompletionProcessor;
import io.gravitee.gateway.reactive.debug.reactor.processor.DebugPlatformProcessorChainFactory;
import io.gravitee.gateway.reactive.http.vertx.VertxHttpServerRequest;
import io.gravitee.gateway.reactive.reactor.ApiReactor;
import io.gravitee.gateway.reactive.reactor.DefaultHttpRequestDispatcher;
import io.gravitee.gateway.reactive.reactor.handler.HttpAcceptorResolver;
import io.gravitee.gateway.reactive.reactor.processor.NotFoundProcessorChainFactory;
import io.gravitee.gateway.reactor.handler.HttpAcceptor;
import io.gravitee.gateway.reactor.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.ResponseProcessorChainFactory;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.opentelemetry.Tracer;
import io.gravitee.node.opentelemetry.OpenTelemetryFactory;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.Vertx;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import lombok.CustomLog;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class DebugHttpRequestDispatcher extends DefaultHttpRequestDispatcher {

    private final HttpAcceptorResolver httpAcceptorResolver;
    private final NotFoundProcessorChainFactory notFoundProcessorChainFactory;
    private final RequestPathConfiguration requestPathConfiguration;
    private final DebugCompletionProcessor debugCompletionProcessor;

    public DebugHttpRequestDispatcher(
        GatewayConfiguration gatewayConfiguration,
        HttpAcceptorResolver httpAcceptorResolver,
        IdGenerator idGenerator,
        ComponentProvider globalComponentProvider,
        RequestProcessorChainFactory requestProcessorChainFactory,
        ResponseProcessorChainFactory responseProcessorChainFactory,
        DebugPlatformProcessorChainFactory platformProcessorChainFactory,
        NotFoundProcessorChainFactory notFoundProcessorChainFactory,
        RequestTimeoutConfiguration requestTimeoutConfiguration,
        RequestClientAuthConfiguration requestClientAuthConfiguration,
        RequestPathConfiguration requestPathConfiguration,
        DebugCompletionProcessor debugCompletionProcessor,
        Vertx vertx,
        boolean warningsEnabled
    ) {
        super(
            gatewayConfiguration,
            httpAcceptorResolver,
            idGenerator,
            globalComponentProvider,
            requestProcessorChainFactory,
            responseProcessorChainFactory,
            platformProcessorChainFactory,
            notFoundProcessorChainFactory,
            TracingContext.noop(),
            requestTimeoutConfiguration,
            requestClientAuthConfiguration,
            requestPathConfiguration,
            vertx,
            warningsEnabled
        );
        this.httpAcceptorResolver = httpAcceptorResolver;
        this.notFoundProcessorChainFactory = notFoundProcessorChainFactory;
        this.requestPathConfiguration = requestPathConfiguration;
        this.debugCompletionProcessor = debugCompletionProcessor;
    }

    /**
     * Answers a rejected path without abandoning the debug session it belongs to.
     *
     * <p>The gateway refuses a path <em>before</em> the acceptor is resolved — that is the point of
     * the mode, since the routing decision itself is what must not be made on an unresolved path.
     * The consequence is that the dispatcher never learns the request was a debug one: the platform
     * post-processor chain is skipped, {@link DebugCompletionProcessor} never runs, and the debug
     * event is left in {@code DEBUGGING} while the console waits for a result that will never come.
     * The user sees an endless spinner, with nothing in the logs to explain it.
     *
     * <p>This port serves debug traffic and nothing else, so it is the one place where that can be
     * noticed without weakening the production path. The request is refused exactly as it would be
     * upstream — the same chain, the same 400 — and the completion processor is then run on the same
     * context, which reports a session with no policy step and the response the gateway gave.
     *
     * <p>Note this holds for the not-found branch too, which has always had the same dead end. It is
     * left alone deliberately: a debug request carries a context path the daemon built itself, so
     * failing to match an acceptor means something is wrong well upstream of this decision.
     */
    @Override
    protected Completable afterRejectedPath(final Completable rejection, final MutableExecutionContext ctx) {
        // Resolving here decides nothing about routing — the request has already been refused. It
        // only names the session to close. The raw path still matches: the acceptor compares
        // prefixes and the debug context path the daemon prepends sits ahead of whatever was typed.
        final ReactableDebugApi<?> debugApi = resolveDebugApi(ctx);

        if (debugApi == null) {
            // Nothing identifies the session, so there is nothing to close. Answering is still right.
            ctx.withLogger(log).warn("Rejected debug request on path {} could not be matched to a debug API", ctx.request().path());
            return rejection;
        }

        // doFinally rather than andThen, and for one reason: the session must be closed on failure
        // and on cancellation too. Ending the response throws when the client has already gone, and
        // the debug verticle disposes this chain when the connection closes — either would otherwise
        // leave the event in DEBUGGING and the console spinning, which is the dead end this exists
        // to close.
        return rejection.doFinally(() -> {
            // Set here rather than before the rejected chain: two of its processors read this same
            // attribute to decide whether to report, and finding it set would make them follow the
            // API's own analytics configuration instead of handlers.rejected.analytics.enabled.
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API, debugApi);
            debugCompletionProcessor
                .execute((HttpExecutionContextInternal) ctx)
                .subscribe(
                    () -> {},
                    throwable -> ctx.withLogger(log).error("Failed to close the debug session for a rejected path", throwable)
                );
        });
    }

    /**
     * Both engines, deliberately. A v4 definition — and a v2 one under emulation — is deployed
     * behind an {@link ApiReactor}, but a v2 definition whose execution mode is {@code V3} is served
     * by a {@link DebugApiReactorHandler}, which is a plain {@code ReactorHandler} and never an
     * {@code ApiReactor}. Matching the interface alone left that engine with the endless spinner
     * this class exists to remove, and said so in a warning claiming no debug API had matched —
     * while the acceptor had matched perfectly well.
     */
    private ReactableDebugApi<?> resolveDebugApi(final MutableExecutionContext ctx) {
        final HttpAcceptor acceptor = httpAcceptorResolver.resolve(
            ctx.request().host(),
            ctx.request().path(),
            ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SERVER_ID)
        );
        if (acceptor == null) {
            return null;
        }
        return switch (acceptor.reactor()) {
            case ApiReactor<?> apiReactor when apiReactor.api() instanceof ReactableDebugApi<?> api -> api;
            case DebugApiReactorHandler handler -> handler.debugApi();
            case null, default -> null;
        };
    }

    @Override
    protected DefaultExecutionContext createExecutionContext(VertxHttpServerRequest request) {
        return new DebugExecutionContext(request, request.response());
    }

    @Override
    protected io.gravitee.gateway.http.vertx.VertxHttpServerRequest createV3Request(
        final HttpServerRequest httpServerRequest,
        final IdGenerator idGenerator,
        final VertxHttpServerRequestOptions options
    ) {
        io.gravitee.gateway.http.vertx.VertxHttpServerRequest v3Request = super.createV3Request(httpServerRequest, idGenerator, options);
        return new VertxHttpServerRequestDebugDecorator(v3Request, idGenerator);
    }
}
