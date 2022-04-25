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
package io.gravitee.gateway.reactive.standalone.vertx;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.reactive.reactor.HttpRequestDispatcher;
import io.gravitee.node.vertx.configuration.HttpServerConfiguration;
import io.reactivex.Completable;
import io.reactivex.disposables.Disposable;
import io.reactivex.plugins.RxJavaPlugins;
import io.vertx.core.http.HttpServer;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.core.http.HttpServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpProtocolVerticle extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(HttpProtocolVerticle.class);

    private final io.vertx.reactivex.core.http.HttpServer rxHttpServer;
    private final HttpRequestDispatcher requestDispatcher;
    private Disposable requestDisposable;

    public HttpProtocolVerticle(
        @Qualifier("gatewayHttpServer") HttpServer httpServer,
        @Qualifier("httpRequestDispatcher") HttpRequestDispatcher requestDispatcher
    ) {
        this.rxHttpServer = io.vertx.reactivex.core.http.HttpServer.newInstance(httpServer);
        this.requestDispatcher = requestDispatcher;
    }

    @Override
    public Completable rxStart() {
        // Set global error handler to catch everything that has not been properly caught.
        RxJavaPlugins.setErrorHandler(e -> log.warn("An unexpected error occurred", e));

        // Reconfigure RxJava to use Vertx schedulers.
        RxJavaPlugins.setComputationSchedulerHandler(s -> RxHelper.scheduler(vertx));
        RxJavaPlugins.setIoSchedulerHandler(s -> RxHelper.blockingScheduler(vertx));
        RxJavaPlugins.setNewThreadSchedulerHandler(s -> RxHelper.scheduler(vertx));

        requestDisposable =
            rxHttpServer
                .requestStream()
                .toFlowable()
                .flatMapCompletable(
                    request ->
                        requestDispatcher
                            .dispatch(request)
                            .doOnComplete(() -> log.debug("Request properly dispatched"))
                            .doOnSubscribe(
                                dispatchDisposable ->
                                    request
                                        .connection()
                                        // Must be added to ensure closed connection disposes underlying subscription
                                        .closeHandler(event -> dispatchDisposable.dispose())
                            )
                            .onErrorResumeNext(
                                throwable -> {
                                    log.error("An unexpected error occurred while dispatching the incoming request", throwable);
                                    HttpServerResponse response = request.response();
                                    if (!response.headWritten()) {
                                        response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
                                    }
                                    return response.rxEnd().onErrorResumeNext(endError -> Completable.complete());
                                }
                            )
                            .andThen(
                                Completable.defer(
                                    () -> {
                                        HttpServerResponse response = request.response();
                                        if (!response.ended()) {
                                            if (!response.headWritten()) {
                                                response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
                                            }
                                            return response.rxEnd().onErrorResumeNext(endError -> Completable.complete());
                                        } else {
                                            return Completable.complete();
                                        }
                                    }
                                )
                            )
                )
                .subscribe();

        return rxHttpServer
            .rxListen()
            .ignoreElement()
            .doOnComplete(() -> log.info("HTTP listener ready to accept requests on port {}", rxHttpServer.actualPort()))
            .doOnError(throwable -> log.error("Unable to start HTTP Server", throwable.getCause()));
    }

    @Override
    public Completable rxStop() {
        log.info("Stopping HTTP Server...");
        return Completable
            .fromRunnable(() -> requestDisposable.dispose())
            .andThen(rxHttpServer.rxClose().doOnComplete(() -> log.info("HTTP Server has been correctly stopped")));
    }
}
