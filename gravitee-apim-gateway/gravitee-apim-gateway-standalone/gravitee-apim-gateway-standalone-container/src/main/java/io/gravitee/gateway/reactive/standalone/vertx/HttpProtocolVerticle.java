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
package io.gravitee.gateway.reactive.standalone.vertx;

import static io.gravitee.gateway.reactive.http.vertx.VertxHttpServerRequest.NETTY_ATTR_CONNECTION_TIME;
import static io.gravitee.gateway.reactive.http.vertx.VertxHttpServerRequest.NETTY_ATTR_REQUEST_CONTEXT;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseExecutionContext;
import io.gravitee.gateway.reactive.core.context.ComponentScope;
import io.gravitee.gateway.reactive.core.context.diagnostic.DiagnosticReportHelper;
import io.gravitee.gateway.reactive.reactor.HttpRequestDispatcher;
import io.gravitee.node.api.server.ServerManager;
import io.gravitee.node.vertx.server.http.VertxHttpServer;
import io.gravitee.reporter.api.v4.metric.Diagnostic;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.vertx.core.http.impl.HttpServerConnection;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.RxHelper;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import io.vertx.rxjava3.core.http.HttpServerResponse;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Main Vertx Verticle in charge of starting the Vertx http server and to listen and dispatch all incoming http requests.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class HttpProtocolVerticle extends AbstractVerticle {

    private final ServerManager serverManager;
    private final HttpRequestDispatcher requestDispatcher;
    private final Map<VertxHttpServer, HttpServer> httpServerMap;

    public HttpProtocolVerticle(
        final ServerManager serverManager,
        @Qualifier("httpRequestDispatcher") HttpRequestDispatcher requestDispatcher
    ) {
        this.serverManager = serverManager;
        this.requestDispatcher = requestDispatcher;
        this.httpServerMap = new HashMap<>();
    }

    @Override
    public Completable rxStart() {
        // Set global error handler to catch everything that has not been properly caught.
        RxJavaPlugins.setErrorHandler(this::logGlobalErrors);

        // Reconfigure RxJava to use Vertx schedulers.
        RxJavaPlugins.setComputationSchedulerHandler(s -> RxHelper.scheduler(vertx));
        RxJavaPlugins.setIoSchedulerHandler(s -> RxHelper.blockingScheduler(vertx));
        RxJavaPlugins.setNewThreadSchedulerHandler(s -> RxHelper.scheduler(vertx));

        final List<VertxHttpServer> servers = this.serverManager.servers(VertxHttpServer.class);

        // Some exceptions can be raised at the Vertx context level (outside the request flow). This is the case when the client
        // closes the connection before the response is fully written. This one can be ignored as it's properly handled at the request level.
        Vertx.currentContext().exceptionHandler(this::logGlobalErrors);

        return Flowable.fromIterable(servers)
            .concatMapCompletable(gioServer -> {
                final HttpServer rxHttpServer = gioServer.newInstance();
                httpServerMap.put(gioServer, rxHttpServer);

                // Listen and dispatch http requests.
                return rxHttpServer
                    .connectionHandler(connection -> {
                        HttpServerConnection delegate = (HttpServerConnection) connection.getDelegate();
                        delegate.channel().attr(AttributeKey.valueOf(NETTY_ATTR_CONNECTION_TIME)).set(System.currentTimeMillis());
                    })
                    .requestHandler(request -> dispatchRequest(request, gioServer.id()))
                    .rxListen()
                    .ignoreElement()
                    .doOnComplete(() ->
                        log.info("HTTP server [{}] ready to accept requests on port {}", gioServer.id(), rxHttpServer.actualPort())
                    )
                    .doOnError(throwable -> log.error("Unable to start HTTP server [{}]", gioServer.id(), throwable.getCause()));
            })
            .doOnSubscribe(disposable -> log.info("Starting HTTP servers..."));
    }

    private void logGlobalErrors(Throwable throwable) {
        if (throwable instanceof IllegalStateException && "Response has already been written".equals(throwable.getMessage())) {
            log.debug("Client has prematurely closed the connection before the response is fully written. Ignoring.");
        } else {
            log.warn("An unexpected error occurred.", throwable);
        }
    }

    /**
     * Dispatches the incoming request to the request dispatcher and completes once the dispatch completes.
     * To avoid any interruption in the request flow, this method makes sure that <b>no error can be emitted by logging the error and completing normally</b>.
     *
     * Eventually, in case of unexpected error during the request dispatch, tries to end the response if not already ended (but it's an exceptional case that should not occur).
     *
     * @param request the current request to dispatch.
     * @param serverId the id of the server handling the request.
     */
    private void dispatchRequest(HttpServerRequest request, String serverId) {
        requestDispatcher
            .dispatch(request, serverId)
            .doOnComplete(() -> log.debug("Request properly dispatched"))
            .onErrorResumeNext(t -> handleError(t, request.response()))
            .doOnSubscribe(dispatchDisposable -> configureConnectionHandlers(request, dispatchDisposable))
            .subscribe();
    }

    /**
     * Logs the given {@link Throwable} and try ending the response.
     *
     * @param throwable the {@link Throwable} to log in error.
     * @param response the response to end.
     *
     * @return a completed {@link Completable} in any circumstances.
     */
    private Completable handleError(Throwable throwable, HttpServerResponse response) {
        log.error("An unexpected error occurred while dispatching request", throwable);

        return tryEndResponse(response);
    }

    /**
     * Try to end the response if not already ended and completes normally in case of error.
     *
     * @param response the response to end.
     * @return a completed {@link Completable} even in case of error trying to end the response.
     */
    private Completable tryEndResponse(HttpServerResponse response) {
        try {
            if (!response.ended()) {
                if (!response.headWritten()) {
                    response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
                }

                // Try to end the response and complete normally in case of error.
                return response
                    .rxEnd()
                    .doOnError(throwable -> log.warn("Failed to properly end response after error", throwable))
                    .onErrorComplete();
            }

            return Completable.complete();
        } catch (Throwable throwable) {
            log.warn("Failed to properly end response after error", throwable);
            return Completable.complete();
        }
    }

    static final String CLIENT_ABORTED_TCP_RESET = "CLIENT_ABORTED_TCP_RESET";
    static final String CLIENT_ABORTED_BROKEN_PIPE = "CLIENT_ABORTED_BROKEN_PIPE";
    static final String CLIENT_ABORTED_IDLE_TIMEOUT = "CLIENT_ABORTED_IDLE_TIMEOUT";
    static final String CLIENT_ABORTED_CHANNEL_CLOSED = "CLIENT_ABORTED_CHANNEL_CLOSED";
    static final String CLIENT_ABORTED_TCP_RESET_MESSAGE = "The client reset the connection";
    static final String CLIENT_ABORTED_BROKEN_PIPE_MESSAGE = "The client closed the connection while the response was being written";
    static final String CLIENT_ABORTED_IDLE_TIMEOUT_MESSAGE = "The connection was closed after the client idle timeout elapsed";
    static final String CLIENT_ABORTED_CHANNEL_CLOSED_MESSAGE = "The client closed the connection";

    /** Name of the inbound handler we install to capture {@link IdleStateEvent} before Vert.x closes the channel. */
    private static final String IDLE_REASON_HANDLER_NAME = "gravitee-client-close-reason";
    /** Name Vert.x 4.5 gives the {@code IdleStateHandler} it installs when {@code http.idleTimeout} is configured. */
    private static final String VERTX_IDLE_HANDLER_NAME = "idle";
    /** Marker stored on the channel so {@code closeHandler} can tell an idle-timeout close apart from a plain FIN. */
    private static final String IDLE_CLOSE_REASON = "IDLE_TIMEOUT";
    private static final AttributeKey<Object> REQUEST_CONTEXT_ATTR_KEY = AttributeKey.valueOf(NETTY_ATTR_REQUEST_CONTEXT);
    private static final AttributeKey<Object> CLIENT_CLOSE_REASON_ATTR_KEY = AttributeKey.valueOf("graviteeClientCloseReason");

    private void configureConnectionHandlers(HttpServerRequest request, Disposable dispatchDisposable) {
        installIdleCloseReasonHandler(request);
        request
            .connection()
            // Must be added to ensure closed connection or error disposes underlying subscription.
            .exceptionHandler(event -> {
                recordClientCloseReason(request, event);
                gracefulDispose(dispatchDisposable);
            })
            .closeHandler(event -> {
                // A Vert.x server idle-timeout close arrives here with no Throwable (it never reaches the
                // exceptionHandler), so we rely on the marker set by IdleCloseReasonHandler to identify it.
                recordIdleCloseReason(request);
                gracefulDispose(dispatchDisposable);
            });
    }

    /**
     * Install, once per connection, a tiny inbound handler right after Vert.x's {@code IdleStateHandler}
     * (named {@value #VERTX_IDLE_HANDLER_NAME}) so we can observe the {@link IdleStateEvent} it fires and
     * record an idle marker on the channel <em>before</em> Vert.x reacts to that event by closing the
     * connection.
     * <p>
     * Without this, a server {@code http.idleTimeout} close is indistinguishable from a normal client FIN at
     * {@code closeHandler} time, and {@link #CLIENT_ABORTED_IDLE_TIMEOUT} would be unreachable (APIM-12769).
     * Only installed when the idle handler is present (i.e. {@code http.idleTimeout} is configured).
     */
    private void installIdleCloseReasonHandler(HttpServerRequest request) {
        try {
            Channel channel = ((HttpServerConnection) request.connection().getDelegate()).channel();
            ChannelPipeline pipeline = channel.pipeline();
            if (pipeline.get(IDLE_REASON_HANDLER_NAME) == null && pipeline.get(VERTX_IDLE_HANDLER_NAME) != null) {
                pipeline.addAfter(VERTX_IDLE_HANDLER_NAME, IDLE_REASON_HANDLER_NAME, new IdleCloseReasonHandler());
            }
        } catch (Exception e) {
            log.debug("Unable to install idle-close-reason handler", e);
        }
    }

    /**
     * Inbound handler that records an idle marker on the channel when Netty's {@code IdleStateHandler} signals
     * inactivity, then forwards the event so Vert.x still closes the connection as it normally would.
     */
    private static class IdleCloseReasonHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                ctx.channel().attr(CLIENT_CLOSE_REASON_ATTR_KEY).set(IDLE_CLOSE_REASON);
            }
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * If the connection was closed because of a server idle timeout (flagged by {@link IdleCloseReasonHandler}),
     * decorate the in-flight request metrics with {@link #CLIENT_ABORTED_IDLE_TIMEOUT}.
     */
    private void recordIdleCloseReason(HttpServerRequest request) {
        try {
            Channel channel = ((HttpServerConnection) request.connection().getDelegate()).channel();
            if (IDLE_CLOSE_REASON.equals(channel.attr(CLIENT_CLOSE_REASON_ATTR_KEY).get())) {
                decorateClientClose(request, CLIENT_ABORTED_IDLE_TIMEOUT, CLIENT_ABORTED_IDLE_TIMEOUT_MESSAGE, null);
            }
        } catch (Exception e) {
            log.debug("Unable to record idle close reason", e);
        }
    }

    /**
     * Best-effort: classify the Vert.x connection-level {@link Throwable} that signaled a client-side close
     * (RST, broken pipe, …) and surface it on the request {@link Metrics} so it appears in access logs and
     * analytics — instead of the generic "CLIENT_ABORTED_DURING_RESPONSE_ERROR" fallback (APIM-12769).
     */
    private void recordClientCloseReason(HttpServerRequest request, Throwable cause) {
        if (cause == null) {
            return;
        }
        ClientCloseReason reason = classifyClientClose(cause);
        decorateClientClose(request, reason.key(), reason.message(), cause);
    }

    /**
     * Decorate the in-flight request {@link Metrics} with a client-close failure, going through
     * {@link DiagnosticReportHelper} — the same path {@code AbstractExecutionContext.interruptWith} uses for
     * upstream errors — so the {@link Diagnostic} written here is shape-compatible with every other V4 failure
     * path. No-op when there is no in-flight context/metrics, or when a more specific failure was already set.
     */
    private void decorateClientClose(HttpServerRequest request, String key, String message, Throwable cause) {
        try {
            Object stashed = ((HttpServerConnection) request.connection().getDelegate()).channel().attr(REQUEST_CONTEXT_ATTR_KEY).get();
            if (stashed instanceof HttpBaseExecutionContext ctx) {
                decorateContext(ctx, key, message, cause);
            }
        } catch (Exception classificationError) {
            log.debug("Failed to classify client close reason", classificationError);
        }
    }

    private void decorateContext(HttpBaseExecutionContext ctx, String key, String message, Throwable cause) {
        Metrics metrics = ctx.metrics();
        if (metrics == null) {
            // MetricsProcessor hasn't run yet (extremely early abort) — nothing to decorate.
            return;
        }
        if (metrics.getFailure() != null || metrics.getErrorKey() != null) {
            // A more specific classifier (e.g. the upstream connector), or another close handler, already set it.
            return;
        }
        ExecutionFailure failure = new ExecutionFailure(499).key(key).message(message);
        if (cause != null) {
            failure.cause(cause);
        }
        ComponentScope.ComponentEntry component = ComponentScope.peek((BaseExecutionContext) ctx);
        Diagnostic diagnostic = DiagnosticReportHelper.fromExecutionFailure(
            component,
            metrics.getErrorKey(),
            metrics.getErrorMessage(),
            failure
        );
        metrics.setFailure(diagnostic);
        // Legacy fields kept in sync so dashboards reading the flat error-key/error-message still work.
        metrics.setErrorKey(diagnostic.getKey());
        metrics.setErrorMessage(diagnostic.getMessage());
        log.debug("Classified client close reason as {}", key);
    }

    static ClientCloseReason classifyClientClose(Throwable cause) {
        if (hasCause(cause, ClosedChannelException.class)) {
            return new ClientCloseReason(CLIENT_ABORTED_CHANNEL_CLOSED, CLIENT_ABORTED_CHANNEL_CLOSED_MESSAGE);
        }
        // Scan the whole cause chain: the close cause is sometimes wrapped, so the top-level message alone is not
        // enough (consistent with the upstream connector's classification).
        String message = collectChainMessages(cause);
        if (message != null) {
            if (message.contains("Connection reset")) {
                return new ClientCloseReason(CLIENT_ABORTED_TCP_RESET, CLIENT_ABORTED_TCP_RESET_MESSAGE);
            }
            if (message.contains("Broken pipe")) {
                return new ClientCloseReason(CLIENT_ABORTED_BROKEN_PIPE, CLIENT_ABORTED_BROKEN_PIPE_MESSAGE);
            }
        }
        // Default for any other IO/connection error reaching the exception handler.
        return new ClientCloseReason(CLIENT_ABORTED_CHANNEL_CLOSED, CLIENT_ABORTED_CHANNEL_CLOSED_MESSAGE);
    }

    /** Walk the cause chain looking for a given exception type (the close cause may be wrapped). */
    private static boolean hasCause(Throwable t, Class<? extends Throwable> type) {
        Throwable current = t;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return false;
    }

    /** Concatenate the messages along the cause chain so message-based detection survives wrapping. */
    private static String collectChainMessages(Throwable t) {
        StringBuilder sb = new StringBuilder();
        Throwable current = t;
        while (current != null) {
            if (current.getMessage() != null) {
                sb.append(current.getMessage()).append('\n');
            }
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    record ClientCloseReason(String key, String message) {}

    private void gracefulDispose(Disposable dispatchDisposable) {
        if (!dispatchDisposable.isDisposed()) {
            try {
                dispatchDisposable.dispose();
            } catch (Exception e) {
                log.warn("Cannot graceful dispose request", e);
            }
        }
    }

    @Override
    public Completable rxStop() {
        return Flowable.fromIterable(httpServerMap.entrySet())
            .flatMapCompletable(entry -> {
                final VertxHttpServer gioServer = entry.getKey();
                final HttpServer rxHttpServer = entry.getValue();
                return rxHttpServer.rxClose().doOnComplete(() -> log.info("HTTP server [{}] has been correctly stopped", gioServer.id()));
            })
            .doOnSubscribe(disposable -> log.info("Stopping HTTP servers..."));
    }
}
