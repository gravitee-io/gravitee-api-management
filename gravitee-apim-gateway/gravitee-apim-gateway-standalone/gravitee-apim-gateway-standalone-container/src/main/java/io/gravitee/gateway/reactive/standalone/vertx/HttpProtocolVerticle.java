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

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.reactive.reactor.HttpRequestDispatcher;
import io.gravitee.node.api.server.ServerManager;
import io.gravitee.node.vertx.server.http.VertxHttpServer;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.RxHelper;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import io.vertx.rxjava3.core.http.HttpServerResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Main Vertx Verticle in charge of starting the Vertx http server and to listen and dispatch all incoming http requests.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpProtocolVerticle extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(HttpProtocolVerticle.class);

    private final ServerManager serverManager;
    private final HttpRequestDispatcher requestDispatcher;

    @Getter
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
        RxJavaPlugins.setErrorHandler(throwable -> log.warn("An unexpected error occurred", throwable));

        // Reconfigure RxJava to use Vertx schedulers.
        RxJavaPlugins.setComputationSchedulerHandler(s -> RxHelper.scheduler(vertx));
        RxJavaPlugins.setIoSchedulerHandler(s -> RxHelper.blockingScheduler(vertx));
        RxJavaPlugins.setNewThreadSchedulerHandler(s -> RxHelper.scheduler(vertx));

        final List<VertxHttpServer> servers = this.serverManager.servers(VertxHttpServer.class);

        return Flowable
            .fromIterable(servers)
            .concatMapCompletable(gioServer -> {
                final HttpServer rxHttpServer = gioServer.newInstance();
                httpServerMap.put(gioServer, rxHttpServer);

                // Listen and dispatch http requests.
                return rxHttpServer
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
                    .doOnError(throwable -> log.error("Failed to properly end response after error", throwable))
                    .onErrorComplete();
            }

            return Completable.complete();
        } catch (Throwable throwable) {
            log.error("Failed to properly end response after error", throwable);
            return Completable.complete();
        }
    }

    private void configureConnectionHandlers(HttpServerRequest request, Disposable dispatchDisposable) {
        request
            .connection()
            // Must be added to ensure closed connection or error disposes underlying subscription.
            .exceptionHandler(event -> dispatchDisposable.dispose())
            .closeHandler(event -> dispatchDisposable.dispose());
    }

    @Override
    public Completable rxStop() {
        return Flowable
            .fromIterable(httpServerMap.entrySet())
            .flatMapCompletable(entry -> {
                final VertxHttpServer gioServer = entry.getKey();
                final HttpServer rxHttpServer = entry.getValue();
                return rxHttpServer.rxClose().doOnComplete(() -> log.info("HTTP server [{}] has been correctly stopped", gioServer.id()));
            })
            .doOnSubscribe(disposable -> log.info("Stopping HTTP servers..."));
    }
}
