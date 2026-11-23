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
package io.gravitee.apim.integration.tests.websocket.v4;

import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.http.WebSocketFrame;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.WebSocketClient;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ParameterContext;

/**
 * The backend does not negotiate permessage-deflate: the frame limit applies to the compressed size on the wire, so a
 * compressible payload would get through whatever the limit.
 */
abstract class AbstractWebsocketLargeFrameTest extends AbstractWebsocketV4GatewayTest {

    static final int SIXTEEN_MIB = 16 * 1024 * 1024;
    static final int VERTX_DEFAULT_FRAME_SIZE = 65_536;

    @Override
    @BeforeAll
    public void startWebsocketServer(Vertx vertx, VertxTestContext context) {
        final HttpServerOptions options = new HttpServerOptions()
            .setPerMessageWebSocketCompressionSupported(false)
            .setPerFrameWebSocketCompressionSupported(false);
        vertx
            .createHttpServer(options)
            .webSocketHandler(serverWebSocket -> {
                if (websocketServerHandler != null) {
                    websocketServerHandler.handle(serverWebSocket);
                }
            })
            .listen(getAvailablePort())
            .subscribe(
                server -> {
                    httpServer = server;
                    context.completeNow();
                },
                context::failNow
            );
    }

    @Override
    protected void configureWebSocketClient(
        WebSocketClientOptions options,
        GatewayDynamicConfig.Config gatewayConfig,
        ParameterContext parameterContext
    ) {
        super.configureWebSocketClient(options, gatewayConfig, parameterContext);
        options.setMaxFrameSize(SIXTEEN_MIB).setMaxMessageSize(SIXTEEN_MIB);
    }

    void backendSendsOneFrameOfRequestedSize(VertxTestContext testContext) {
        websocketServerHandler = serverWebSocket -> {
            serverWebSocket.textMessageHandler(message -> {
                final int size = Integer.parseInt(message.substring("SIZE:".length()));
                final char[] payload = new char[size];
                Arrays.fill(payload, 'a');
                // One single final frame: writeTextMessage would split it by the backend server frame size.
                serverWebSocket.writeFrame(WebSocketFrame.textFrame(new String(payload), true)).doOnError(testContext::failNow).subscribe();
            });
        };
    }

    record Outcome(Integer receivedFrameSize, Short closeStatusCode, String closeReason) {}

    Outcome askBackendForOneFrame(WebSocketClient webSocketClient, int size) throws InterruptedException {
        return webSocketClient
            .connect("/test")
            .flatMap(webSocket ->
                Single.<Outcome>create(emitter -> {
                    webSocket.frameHandler(frame -> {
                        if (frame.isText()) {
                            emitter.onSuccess(new Outcome(frame.binaryData().length(), null, null));
                            webSocket.close().subscribe();
                        }
                    });
                    webSocket.closeHandler(v -> emitter.onSuccess(new Outcome(null, webSocket.closeStatusCode(), webSocket.closeReason())));
                    webSocket.exceptionHandler(emitter::tryOnError);
                    webSocket.writeTextMessage("SIZE:" + size).subscribe(() -> {}, emitter::tryOnError);
                })
            )
            .timeout(30, TimeUnit.SECONDS)
            .blockingGet();
    }
}
