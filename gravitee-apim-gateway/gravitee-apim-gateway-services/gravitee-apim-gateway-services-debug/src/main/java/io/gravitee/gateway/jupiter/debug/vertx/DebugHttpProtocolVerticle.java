/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.jupiter.debug.vertx;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.jupiter.reactor.HttpRequestDispatcher;
import io.reactivex.Completable;
import io.reactivex.disposables.Disposable;
import io.vertx.core.http.HttpServer;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Debug Vertx Verticle in charge of starting the Vertx http server and to listen and dispatch all incoming debug http requests.
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugHttpProtocolVerticle extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(DebugHttpProtocolVerticle.class);

    private final io.vertx.reactivex.core.http.HttpServer rxHttpServer;
    private final HttpRequestDispatcher requestDispatcher;
    private Disposable requestDisposable;

    public DebugHttpProtocolVerticle(
        @Qualifier("debugGatewayHttpServer") HttpServer httpServer,
        @Qualifier("debugHttpRequestDispatcher") HttpRequestDispatcher requestDispatcher
    ) {
        this.rxHttpServer = io.vertx.reactivex.core.http.HttpServer.newInstance(httpServer);
        this.requestDispatcher = requestDispatcher;
    }

    @Override
    public Completable rxStart() {
        // Listen and dispatch http requests.
        requestDisposable = rxHttpServer.requestStream().toFlowable().flatMapCompletable(this::dispatchRequest).subscribe();

        return rxHttpServer
            .rxListen()
            .ignoreElement()
            .doOnComplete(() -> log.info("HTTP listener ready to accept requests on port {}", rxHttpServer.actualPort()))
            .doOnError(throwable -> log.error("Unable to start HTTP Server", throwable.getCause()));
    }

    /**
     * Dispatches the incoming request to the request dispatcher and completes once the dispatch completes.
     * To avoid any interruption in the request flow, this method makes sure that <b>no error can be emitted by logging the error and completing normally</b>.
     *
     * Eventually, in case of unexpected error during the request dispatch, tries to end the response if not already ended (but it's an exceptional case that should not occur).
     *
     * @param request the current request to dispatch.
     * @return a {@link Completable} that completes once the request has been ended.
     */
    private Completable dispatchRequest(HttpServerRequest request) {
        return requestDispatcher
            .dispatch(request)
            .doOnComplete(() -> log.debug("Request properly dispatched"))
            .onErrorResumeNext(t -> handleError(t, request.response()))
            .doOnSubscribe(dispatchDisposable -> configureCloseHandler(request, dispatchDisposable));
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

    private void configureCloseHandler(HttpServerRequest request, Disposable dispatchDisposable) {
        request
            .connection()
            // Must be added to ensure closed connection disposes underlying subscription
            .closeHandler(event -> dispatchDisposable.dispose());
    }

    @Override
    public Completable rxStop() {
        log.info("Stopping HTTP Server...");
        return Completable
            .fromRunnable(requestDisposable::dispose)
            .andThen(rxHttpServer.rxClose().doOnComplete(() -> log.info("HTTP Server has been correctly stopped")));
    }
}
