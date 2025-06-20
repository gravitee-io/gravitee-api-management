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
package io.gravitee.gateway.standalone.vertx;

import io.gravitee.node.vertx.server.http.VertxHttpServer;
import io.gravitee.node.vertx.server.tcp.VertxTcpServer;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.core.net.NetServer;
import java.util.function.BiConsumer;

public record ServerRegister(BiConsumer<VertxHttpServer, HttpServer> httpRegister, BiConsumer<VertxTcpServer, NetServer> tcpRegister) {
    public void register(VertxHttpServer vertxHttpServer, HttpServer httpServer) {
        httpRegister.accept(vertxHttpServer, httpServer);
    }

    public void register(VertxTcpServer vertxTcpServer, NetServer netServer) {
        tcpRegister.accept(vertxTcpServer, netServer);
    }
}
