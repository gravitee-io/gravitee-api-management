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

package io.gravitee.plugin.endpoint.http.proxy.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.server.DefaultServerManager;
import io.gravitee.node.vertx.server.http.VertxHttpServer;
import io.gravitee.node.vertx.server.http.VertxHttpServerOptions;
import io.vertx.core.http.WebSocketClientOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WebSocketSizeLimitsTest {

    private static final int ONE_MIB = 1024 * 1024;

    private DefaultServerManager serverManager;

    @BeforeEach
    void setUp() {
        serverManager = new DefaultServerManager();
    }

    @Test
    void should_keep_vertx_client_defaults_without_http_server() {
        assertThat(WebSocketSizeLimits.from(serverManager)).isEqualTo(
            new WebSocketSizeLimits(WebSocketClientOptions.DEFAULT_MAX_FRAME_SIZE, WebSocketClientOptions.DEFAULT_MAX_MESSAGE_SIZE)
        );
    }

    @Test
    void should_reuse_server_limits_when_raised() {
        register("http", true, 16 * ONE_MIB, 32 * ONE_MIB);

        assertThat(WebSocketSizeLimits.from(serverManager)).isEqualTo(new WebSocketSizeLimits(16 * ONE_MIB, 32 * ONE_MIB));
    }

    @Test
    void should_never_go_below_vertx_client_defaults() {
        register("http", true, 16 * 1024, 32 * 1024);

        assertThat(WebSocketSizeLimits.from(serverManager)).isEqualTo(
            new WebSocketSizeLimits(WebSocketClientOptions.DEFAULT_MAX_FRAME_SIZE, WebSocketClientOptions.DEFAULT_MAX_MESSAGE_SIZE)
        );
    }

    @Test
    void should_take_the_largest_limits_across_servers() {
        register("public", true, ONE_MIB, 8 * ONE_MIB);
        register("internal", true, 16 * ONE_MIB, 4 * ONE_MIB);

        assertThat(WebSocketSizeLimits.from(serverManager)).isEqualTo(new WebSocketSizeLimits(16 * ONE_MIB, 8 * ONE_MIB));
    }

    @Test
    void should_ignore_servers_without_websocket() {
        register("public", true, ONE_MIB, ONE_MIB);
        register("no-websocket", false, 16 * ONE_MIB, 16 * ONE_MIB);

        assertThat(WebSocketSizeLimits.from(serverManager)).isEqualTo(new WebSocketSizeLimits(ONE_MIB, ONE_MIB));
    }

    private void register(String id, boolean websocketEnabled, int maxFrameSize, int maxMessageSize) {
        final VertxHttpServer server = mock(VertxHttpServer.class);
        when(server.id()).thenReturn(id);
        when(server.options()).thenReturn(
            VertxHttpServerOptions.builder()
                .id(id)
                .websocketEnabled(websocketEnabled)
                .maxWebSocketFrameSize(maxFrameSize)
                .maxWebSocketMessageSize(maxMessageSize)
                .build()
        );
        serverManager.register(server);
    }
}
