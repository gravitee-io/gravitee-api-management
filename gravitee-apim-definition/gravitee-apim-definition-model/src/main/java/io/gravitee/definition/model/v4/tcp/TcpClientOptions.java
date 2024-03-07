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
package io.gravitee.definition.model.v4.tcp;

import static io.gravitee.node.vertx.client.tcp.VertxTcpClientOptions.DEFAULT_CONNECT_TIMEOUT;
import static io.gravitee.node.vertx.client.tcp.VertxTcpClientOptions.DEFAULT_IDLE_TIMEOUT;
import static io.gravitee.node.vertx.client.tcp.VertxTcpClientOptions.DEFAULT_READ_IDLE_TIMEOUT;
import static io.gravitee.node.vertx.client.tcp.VertxTcpClientOptions.DEFAULT_RECONNECT_ATTEMPTS;
import static io.gravitee.node.vertx.client.tcp.VertxTcpClientOptions.DEFAULT_RECONNECT_INTERVAL;
import static io.gravitee.node.vertx.client.tcp.VertxTcpClientOptions.DEFAULT_WRITE_IDLE_TIMEOUT;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TcpClientOptions {

    @Builder.Default
    int connectTimeout = DEFAULT_CONNECT_TIMEOUT;

    @Builder.Default
    private int reconnectAttempts = DEFAULT_RECONNECT_ATTEMPTS;

    @Builder.Default
    private int reconnectInterval = DEFAULT_RECONNECT_INTERVAL;

    @Builder.Default
    private int idleTimeout = DEFAULT_IDLE_TIMEOUT;

    @Builder.Default
    private int readIdleTimeout = DEFAULT_READ_IDLE_TIMEOUT;

    @Builder.Default
    private int writeIdleTimeout = DEFAULT_WRITE_IDLE_TIMEOUT;
}
