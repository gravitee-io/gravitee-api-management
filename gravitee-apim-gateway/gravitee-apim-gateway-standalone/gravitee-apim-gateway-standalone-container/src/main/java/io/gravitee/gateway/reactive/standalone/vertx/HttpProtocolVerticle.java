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
import static io.vertx.core.http.HttpServerRequest.DEFAULT_INVALID_REQUEST_HANDLER;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.reactive.reactor.HttpRequestDispatcher;
import io.gravitee.node.api.server.ServerManager;
import io.gravitee.node.vertx.server.http.VertxHttpServer;
import io.gravitee.node.vertx.server.http.VertxHttpServerOptions;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.TooLongHttpHeaderException;
import io.netty.util.AttributeKey;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.vertx.core.http.impl.HttpServerConnection;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.RxHelper;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import io.vertx.rxjava3.core.http.HttpServerResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Main Vertx Verticle in charge of starting the Vertx http server and to listen
 * and dispatch all incoming http requests.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class HttpProtocolVerticle extends AbstractVerticle {

    static final String HEADERS_TOO_LARGE_MESSAGE = "Request headers exceed the size accepted by the Gateway";
    private static final String HEADERS_TOO_LARGE_LOG =
        "HTTP server [{}] rejected a request with headers larger than {} bytes. Increase {}.maxHeaderSize if clients need to send larger headers.";

    // Shared by all verticle instances, so each server warns once however many event loops serve it.
    private static final Set<VertxHttpServer> HEADERS_TOO_LARGE_WARNED = ConcurrentHashMap.newKeySet();

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
        // Set global error handler to catch everything that has not been properly
        // caught.
        RxJavaPlugins.setErrorHandler(this::logGlobalErrors);

        // Reconfigure RxJava to use Vertx schedulers.
        RxJavaPlugins.setComputationSchedulerHandler(s -> RxHelper.scheduler(vertx));
        RxJavaPlugins.setIoSchedulerHandler(s -> RxHelper.blockingScheduler(vertx));
        RxJavaPlugins.setNewThreadSchedulerHandler(s -> RxHelper.scheduler(vertx));

        final List<VertxHttpServer> servers = this.serverManager.servers(VertxHttpServer.class);

        // Some exceptions can be raised at the Vertx context level (outside the request
        // flow). This is the case when the client
        // closes the connection before the response is fully written. This one can be
        // ignored as it's properly handled at the request level.
        Vertx.currentContext().exceptionHandler(this::logGlobalErrors);

        return Flowable.fromIterable(servers)
            .concatMapCompletable(gioServer -> {
                final HttpServer rxHttpServer = gioServer.newInstance();
                httpServerMap.put(gioServer, rxHttpServer);

                // Listen and dispatch http requests.
                return rxHttpServer
                    // In Vert.x 5, channel() was removed; use channelHandlerContext().channel()
                    // instead
                    .connectionHandler(connection -> {
                        HttpServerConnection delegate = (HttpServerConnection) connection.getDelegate();
                        delegate
                            .channelHandlerContext()
                            .channel()
                            .attr(AttributeKey.valueOf(NETTY_ATTR_CONNECTION_TIME))
                            .set(System.currentTimeMillis());
                    })
                    .requestHandler(request -> dispatchRequest(request, gioServer.id()))
                    .invalidRequestHandler(request -> handleInvalidRequest(request, gioServer))
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
     * Dispatches the incoming request to the request dispatcher and completes once
     * the dispatch completes.
     * To avoid any interruption in the request flow, this method makes sure that
     * <b>no error can be emitted by logging the error and completing normally</b>.
     *
     * Eventually, in case of unexpected error during the request dispatch, tries to
     * end the response if not already ended (but it's an exceptional case that
     * should not occur).
     *
     * @param request  the current request to dispatch.
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
     * Requests Netty fails to decode never reach the dispatcher, so they are missing from API logs and analytics.
     * Oversized headers get a JSON body for the consumer and a warning naming the setting for the Gateway admin,
     * logged once per server then at debug level so a misbehaving client can't flood the logs;
     * any other decoding failure keeps the Vert.x default response.
     * Only HTTP/1.x goes through here: HTTP/2 header limits are enforced by the codec.
     *
     * @param request   the request that failed to decode.
     * @param gioServer the server that received it.
     */
    private void handleInvalidRequest(HttpServerRequest request, VertxHttpServer gioServer) {
        // Decoder result and default handler are only exposed on the core request.
        final var delegate = request.getDelegate();
        if (!(delegate.decoderResult().cause() instanceof TooLongHttpHeaderException)) {
            DEFAULT_INVALID_REQUEST_HANDLER.handle(delegate);
            return;
        }

        // The limit is read from the server's own prefix: "http" for a single server, "servers[N]" for a list.
        final VertxHttpServerOptions options = gioServer.options();
        final Object[] logArgs = {
            gioServer.id(),
            options.getMaxHeaderSize(),
            Objects.requireNonNullElse(options.getPrefix(), VertxHttpServerOptions.HTTP_PREFIX),
        };
        if (HEADERS_TOO_LARGE_WARNED.add(gioServer)) {
            log.warn(HEADERS_TOO_LARGE_LOG, logArgs);
        } else {
            log.debug(HEADERS_TOO_LARGE_LOG, logArgs);
        }

        final int status = HttpResponseStatus.REQUEST_HEADER_FIELDS_TOO_LARGE.code();
        delegate
            .response()
            .setStatusCode(status)
            .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
            .end(new JsonObject().put("message", HEADERS_TOO_LARGE_MESSAGE).put("http_status_code", status).encode())
            .onComplete(ar -> delegate.connection().close());
    }

    /**
     * Logs the given {@link Throwable} and try ending the response.
     *
     * @param throwable the {@link Throwable} to log in error.
     * @param response  the response to end.
     *
     * @return a completed {@link Completable} in any circumstances.
     */
    private Completable handleError(Throwable throwable, HttpServerResponse response) {
        log.error("An unexpected error occurred while dispatching request", throwable);

        return tryEndResponse(response);
    }

    /**
     * Try to end the response if not already ended and completes normally in case
     * of error.
     *
     * @param response the response to end.
     * @return a completed {@link Completable} even in case of error trying to end
     *         the response.
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

    private void configureConnectionHandlers(HttpServerRequest request, Disposable dispatchDisposable) {
        request
            .connection()
            // Must be added to ensure closed connection or error disposes underlying
            // subscription.
            .exceptionHandler(event -> gracefulDispose(dispatchDisposable))
            .closeHandler(event -> gracefulDispose(dispatchDisposable));

        // An HTTP/2 client cancels a single request by resetting its stream and keeps the connection open, so the
        // connection handlers above never fire. The response close handler does, per stream. A response that ended
        // normally is skipped so post-response processing (reporting) is not cut short.
        // The verticle owns this single-slot handler: registering another response close handler elsewhere would
        // silently replace it and stream resets would be ignored again (ClientStreamResetPropagationV4IntegrationTest).
        final HttpServerResponse response = request.response();
        response.closeHandler(event -> {
            if (!response.ended()) {
                gracefulDispose(dispatchDisposable);
            }
        });
    }

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
