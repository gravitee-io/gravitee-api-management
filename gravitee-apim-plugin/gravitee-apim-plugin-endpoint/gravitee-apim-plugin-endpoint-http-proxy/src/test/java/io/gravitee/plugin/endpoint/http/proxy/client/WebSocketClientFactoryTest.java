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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.node.vertx.client.http.VertxWebSocketClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.vertx.rxjava3.core.http.WebSocketClient;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WebSocketClientFactoryTest {

    private static final int TIMEOUT_SECONDS = 60;

    @Mock
    private HttpExecutionContext ctx;

    @Test
    void should_close_the_websocket_client_being_built_when_stopping() throws Exception {
        WebSocketClient webSocketClient = mock(WebSocketClient.class);
        VertxWebSocketClientFactory vertxWebSocketClientFactory = mock(VertxWebSocketClientFactory.class);
        when(vertxWebSocketClientFactory.createWebSocketClient()).thenReturn(webSocketClient);
        VertxWebSocketClientFactory.VertxWebSocketClientFactoryBuilder builder = mock(
            VertxWebSocketClientFactory.VertxWebSocketClientFactoryBuilder.class
        );
        when(builder.build()).thenReturn(vertxWebSocketClientFactory);
        CountDownLatch building = new CountDownLatch(1);
        CountDownLatch built = new CountDownLatch(1);
        var cut = new WebSocketClientFactory() {
            @Override
            protected VertxWebSocketClientFactory.VertxWebSocketClientFactoryBuilder buildWebSocketClient(
                HttpExecutionContext ctx,
                HttpProxyEndpointConnectorConfiguration configuration,
                HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration
            ) {
                building.countDown();
                await()
                    .atMost(TIMEOUT_SECONDS, SECONDS)
                    .until(() -> built.getCount() == 0);
                return builder;
            }
        };
        var configuration = new HttpProxyEndpointConnectorConfiguration();
        configuration.setTarget("http://target");
        var sharedConfiguration = new HttpProxyEndpointConnectorSharedConfiguration();

        Thread request = new Thread(() -> cut.getOrBuildWebSocketClient(ctx, configuration, sharedConfiguration));
        request.start();
        assertTrue(building.await(TIMEOUT_SECONDS, SECONDS));
        Thread stop = new Thread(cut::close);
        stop.start();
        // close() needs the lock held by the build: it can only wait for the client being built.
        await()
            .atMost(TIMEOUT_SECONDS, SECONDS)
            .until(() -> stop.getState() == Thread.State.BLOCKED);
        built.countDown();
        request.join(SECONDS.toMillis(TIMEOUT_SECONDS));
        stop.join(SECONDS.toMillis(TIMEOUT_SECONDS));

        verify(webSocketClient).close();
    }
}
