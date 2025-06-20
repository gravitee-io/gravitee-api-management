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

import io.gravitee.gateway.reactive.reactor.TcpSocketDispatcher;
import io.gravitee.node.api.server.ServerManager;
import io.gravitee.node.vertx.server.tcp.VertxTcpServer;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableSource;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.net.NetServer;
import io.vertx.rxjava3.core.net.NetSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class TcpProtocolVerticle extends AbstractVerticle {

    private final ServerManager serverManager;
    private final TcpSocketDispatcher socketDispatcher;
    private final Map<VertxTcpServer, NetServer> tcpServerMap = new ConcurrentHashMap<>();

    public TcpProtocolVerticle(ServerManager serverManager, @Qualifier("tcpSocketDispatcher") TcpSocketDispatcher socketDispatcher) {
        this.serverManager = serverManager;
        this.socketDispatcher = socketDispatcher;
    }

    @Override
    public Completable rxStart() {
        final List<VertxTcpServer> servers = this.serverManager.servers(VertxTcpServer.class);
        return Flowable
            .fromIterable(servers)
            .concatMapCompletable(gioServer -> {
                log.info("Starting TCP server...");
                NetServer tcpServer = gioServer.newInstance();
                tcpServerMap.put(gioServer, tcpServer);

                // Listen and dispatch TCP requests.
                return tcpServer
                    .connectHandler(socket -> this.dispatchSocket(socket, gioServer.id()))
                    .rxListen()
                    .ignoreElement()
                    .doOnComplete(() ->
                        log.info("TCP server [{}] ready to accept connections on port {}", gioServer.id(), tcpServer.actualPort())
                    )
                    .doOnError(throwable -> log.error("Unable to start TCP server [{}]", gioServer.id(), throwable.getCause()));
            });
    }

    public void dispatchSocket(NetSocket proxySocket, String id) {
        socketDispatcher
            .dispatch(proxySocket, id)
            .doOnComplete(() -> log.debug("TCP Socket properly dispatched"))
            .onErrorResumeNext(t -> handleError(t, proxySocket))
            .subscribe();
    }

    private CompletableSource handleError(Throwable err, NetSocket proxySocket) {
        log.error("Unexpected TCP dispatch error", err);
        return proxySocket.rxClose();
    }

    @Override
    public Completable rxStop() {
        return Flowable
            .fromIterable(tcpServerMap.entrySet())
            .flatMapCompletable(entry -> {
                final VertxTcpServer gioServer = entry.getKey();
                final NetServer rxHttpServer = entry.getValue();
                return rxHttpServer.rxClose().doOnComplete(() -> log.info("TCP server [{}] has been correctly stopped", gioServer.id()));
            })
            .doOnSubscribe(disposable -> log.info("Stopping TCP servers..."));
    }
}
