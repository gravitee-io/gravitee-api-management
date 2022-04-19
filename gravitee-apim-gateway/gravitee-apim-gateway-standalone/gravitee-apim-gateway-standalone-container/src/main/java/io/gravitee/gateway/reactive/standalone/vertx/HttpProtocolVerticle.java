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

import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.utils.Hex;
import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.reactive.reactor.HttpRequestDispatcher;
import io.gravitee.node.vertx.configuration.HttpServerConfiguration;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.plugins.RxJavaPlugins;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpProtocolVerticle extends AbstractVerticle {

    private static final String HEX_FORMAT = "hex";

    private final Logger log = LoggerFactory.getLogger(HttpProtocolVerticle.class);

    @Autowired
    @Qualifier("gatewayHttpServer")
    private HttpServer httpServer;

    @Autowired
    @Qualifier("httpRequestDispatcher")
    private HttpRequestDispatcher requestDispatcher;

    @Autowired
    @Qualifier("httpServerConfiguration")
    private HttpServerConfiguration httpServerConfiguration;

    @Autowired
    private Vertx vertx;

    @Value("${handlers.request.format:uuid}")
    private String requestFormat;

    private Disposable disposable;
    private Disposable requestDisposable;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        final io.vertx.reactivex.core.http.HttpServer rxServer = io.vertx.reactivex.core.http.HttpServer.newInstance(httpServer);
        final Flowable<HttpServerRequest> requestFlowable = rxServer.requestStream().toFlowable();
        final IdGenerator idGenerator;

        if (HEX_FORMAT.equals(requestFormat)) {
            idGenerator = new Hex();
        } else {
            idGenerator = new UUID();
        }

        // Set global error handler to catch everything that has not been properly caught.
        RxJavaPlugins.setErrorHandler(e -> log.warn("An unexpected error occurred", e));

        // Reconfigure RxJava to use Vertx schedulers.
        RxJavaPlugins.setComputationSchedulerHandler(s -> RxHelper.scheduler(vertx));
        RxJavaPlugins.setIoSchedulerHandler(s -> RxHelper.blockingScheduler(vertx));
        RxJavaPlugins.setNewThreadSchedulerHandler(s -> RxHelper.scheduler(vertx));

        requestDisposable =
            requestFlowable
                .flatMapCompletable(requestDispatcher::dispatch)
                .subscribe(() -> {}, e -> log.error("An unexpected error occurred", e));

        disposable =
            rxServer
                .rxListen()
                .subscribe(
                    (s, throwable) -> {
                        if (throwable == null) {
                            log.info("HTTP listener ready to accept requests on port {}", httpServerConfiguration.getPort());
                            startPromise.complete();
                        } else {
                            log.error("Unable to start HTTP Server", throwable.getCause());
                            startPromise.fail(throwable.getCause());
                        }
                    }
                );
    }

    @Override
    public void stop() throws Exception {
        log.info("Stopping HTTP Server...");
        httpServer.close(voidAsyncResult -> log.info("HTTP Server has been correctly stopped"));
    }
}
