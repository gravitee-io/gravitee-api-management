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
package io.gravitee.gateway.http.connector.http.ws;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.proxy.ws.WebSocketProxyRequest;
import io.gravitee.gateway.api.stream.WriteStream;
import io.gravitee.gateway.core.proxy.EmptyProxyResponse;
import io.gravitee.gateway.core.proxy.ws.SwitchProtocolProxyResponse;
import io.gravitee.gateway.http.connector.AbstractHttpProxyConnection;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.UpgradeRejectedException;
import io.vertx.core.http.WebSocket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class WebSocketProxyConnection extends AbstractHttpProxyConnection {

    private static final Set<CharSequence> WS_HOP_HEADERS;

    static {
        Set<CharSequence> wsHopHeaders = new HashSet<>();

        // Hop-by-hop headers Websocket
        wsHopHeaders.add(HttpHeaderNames.KEEP_ALIVE);
        wsHopHeaders.add(HttpHeaderNames.PROXY_AUTHORIZATION);
        wsHopHeaders.add(HttpHeaderNames.PROXY_AUTHENTICATE);
        wsHopHeaders.add(HttpHeaderNames.PROXY_CONNECTION);
        wsHopHeaders.add(HttpHeaderNames.TE);
        wsHopHeaders.add(HttpHeaderNames.TRAILER);

        WS_HOP_HEADERS = Collections.unmodifiableSet(wsHopHeaders);
    }

    private final WebSocketProxyRequest wsProxyRequest;

    public WebSocketProxyConnection(HttpEndpoint endpoint, ProxyRequest proxyRequest) {
        super(endpoint);
        this.wsProxyRequest = (WebSocketProxyRequest) proxyRequest;
    }

    @Override
    public ProxyConnection connect(HttpClient httpClient, int port, String host, String uri, Handler<Void> tracker) {
        // Remove hop-by-hop headers.
        for (CharSequence header : WS_HOP_HEADERS) {
            wsProxyRequest.headers().remove(header);
        }

        httpClient.connectionHandler(
            new io.vertx.core.Handler<HttpConnection>() {
                @Override
                public void handle(HttpConnection event) {
                    //    event.
                }
            }
        );

        httpClient.websocket(
            port,
            host,
            uri,
            new io.vertx.core.Handler<WebSocket>() {
                @Override
                public void handle(WebSocket event) {
                    // The client -> gateway connection must be upgraded now that the one between gateway -> upstream
                    // has been accepted
                    wsProxyRequest.upgrade();

                    // From server to client
                    wsProxyRequest.frameHandler(
                        frame -> {
                            if (frame.type() == io.gravitee.gateway.api.ws.WebSocketFrame.Type.BINARY) {
                                event.writeFrame(
                                    io.vertx.core.http.WebSocketFrame.binaryFrame(
                                        io.vertx.core.buffer.Buffer.buffer(frame.data().getBytes()),
                                        frame.isFinal()
                                    )
                                );
                            } else if (frame.type() == io.gravitee.gateway.api.ws.WebSocketFrame.Type.TEXT) {
                                event.writeFrame(io.vertx.core.http.WebSocketFrame.textFrame(frame.data().toString(), frame.isFinal()));
                            } else if (frame.type() == io.gravitee.gateway.api.ws.WebSocketFrame.Type.CONTINUATION) {
                                event.writeFrame(
                                    io.vertx.core.http.WebSocketFrame.continuationFrame(
                                        io.vertx.core.buffer.Buffer.buffer(frame.data().toString()),
                                        frame.isFinal()
                                    )
                                );
                            }
                        }
                    );

                    wsProxyRequest.closeHandler(result -> event.close());

                    // From client to server
                    event.frameHandler(frame -> wsProxyRequest.write(new WebSocketFrame(frame)));

                    event.closeHandler(
                        event1 -> {
                            wsProxyRequest.close();
                            tracker.handle(null);
                        }
                    );

                    event.exceptionHandler(
                        new io.vertx.core.Handler<Throwable>() {
                            @Override
                            public void handle(Throwable throwable) {
                                wsProxyRequest.reject(HttpStatusCode.BAD_REQUEST_400);
                                sendToClient(new EmptyProxyResponse(HttpStatusCode.BAD_REQUEST_400));
                                tracker.handle(null);
                            }
                        }
                    );

                    // Tell the reactor that the request has been handled by the HTTP client
                    sendToClient(new SwitchProtocolProxyResponse());
                }
            },
            throwable -> {
                if (throwable instanceof UpgradeRejectedException) {
                    wsProxyRequest.reject(((UpgradeRejectedException) throwable).getStatus());
                    sendToClient(new EmptyProxyResponse(((UpgradeRejectedException) throwable).getStatus()));
                } else {
                    wsProxyRequest.reject(HttpStatusCode.BAD_GATEWAY_502);
                    sendToClient(new EmptyProxyResponse(HttpStatusCode.BAD_GATEWAY_502));
                }

                tracker.handle(null);
            }
        );

        return this;
    }

    @Override
    public WriteStream<Buffer> write(Buffer content) {
        return this;
    }

    @Override
    public void end() {}
}
