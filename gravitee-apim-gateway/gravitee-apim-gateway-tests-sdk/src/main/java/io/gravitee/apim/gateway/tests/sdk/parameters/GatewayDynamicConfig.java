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
package io.gravitee.apim.gateway.tests.sdk.parameters;

import io.gravitee.node.vertx.server.http.VertxHttpServer;
import io.gravitee.node.vertx.server.tcp.VertxTcpServer;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.core.net.NetServer;
import java.util.Map;
import java.util.stream.Collectors;

public interface GatewayDynamicConfig {
    interface HttpConfig {
        default int httpPort() {
            if (httpPorts().size() != 1) {
                throw new IllegalStateException("We can’t automatically choose a port");
            }
            return httpPorts().values().iterator().next();
        }

        Map<String, Integer> httpPorts();

        default int httpPort(String id) {
            return httpPorts().get(id);
        }
    }

    interface TcpConfig {
        default int tcpPort() {
            if (tcpPorts().size() != 1) {
                throw new IllegalStateException("We can’t automatically choose a port");
            }
            return tcpPorts().values().iterator().next();
        }

        Map<String, Integer> tcpPorts();

        default int tcpPort(String id) {
            return tcpPorts().get(id);
        }
    }

    interface Config extends HttpConfig, TcpConfig {
        default int port(String id) {
            if (httpPorts().containsKey(id)) {
                return httpPorts().get(id);
            } else if (tcpPorts().containsKey(id)) {
                return tcpPorts().get(id);
            } else {
                throw new IllegalStateException();
            }
        }
    }

    record GatewayDynamicConfigImpl(Map<VertxHttpServer, HttpServer> http, Map<VertxTcpServer, NetServer> tcp) implements Config {
        @Override
        public Map<String, Integer> httpPorts() {
            return http.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().id(), e -> e.getValue().actualPort()));
        }

        @Override
        public Map<String, Integer> tcpPorts() {
            return tcp.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().id(), e -> e.getValue().actualPort()));
        }
    }
}
