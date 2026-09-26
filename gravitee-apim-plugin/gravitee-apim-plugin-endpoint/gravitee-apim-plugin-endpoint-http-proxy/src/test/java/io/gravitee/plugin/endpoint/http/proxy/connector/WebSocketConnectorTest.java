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
package io.gravitee.plugin.endpoint.http.proxy.connector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpRequest;
import io.gravitee.gateway.reactive.api.context.http.HttpResponse;
import io.gravitee.gateway.reactive.api.tracing.Tracer;
import io.gravitee.gateway.reactive.api.ws.WebSocket;
import io.gravitee.gateway.reactive.http.vertx.ws.VertxWebSocket;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.opentelemetry.tracer.noop.NoOpTracer;
import io.gravitee.plugin.endpoint.http.proxy.client.HttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.ServerWebSocket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

/**
 * Drives the connector against a raw TCP backend that answers the WebSocket handshake the way an RFC 7692 server
 * does, so the real Vert.x/Netty client-side extension negotiation is exercised.
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WebSocketConnectorTest {

    private static final int TIMEOUT_SECONDS = 10;
    private static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private static Vertx vertx;

    @Mock
    private HttpExecutionContext ctx;

    @Mock
    private HttpRequest request;

    @Mock
    private HttpResponse response;

    @Mock
    private Metrics metrics;

    @Mock
    private WebSocket callerWebSocket;

    @Mock
    private VertxWebSocket upgradedCallerWebSocket;

    private HttpHeaders requestHeaders;
    private HttpHeaders responseHeaders;
    private NetServer backend;
    private CompletableFuture<String> backendReceivedExtensions;
    private HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration;
    private HttpProxyEndpointConnectorConfiguration configuration;

    @BeforeAll
    static void startVertx() {
        vertx = Vertx.vertx();
    }

    @AfterAll
    static void stopVertx() {
        vertx.close().blockingAwait(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @BeforeEach
    void init() throws Exception {
        backendReceivedExtensions = new CompletableFuture<>();
        backend = vertx.getDelegate().createNetServer().connectHandler(this::answerHandshakeLikeRfc7692Server);
        backend.listen(0).toCompletionStage().toCompletableFuture().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(ctx.response()).thenReturn(response);
        lenient().when(ctx.metrics()).thenReturn(metrics);
        lenient().when(ctx.getTracer()).thenReturn(new Tracer(null, new NoOpTracer()));
        lenient().when(ctx.withLogger(any())).thenReturn(mock(Logger.class));
        lenient().when(ctx.getComponent(Vertx.class)).thenReturn(vertx);
        lenient().when(ctx.getComponent(Configuration.class)).thenReturn(mock(Configuration.class));
        lenient().when(ctx.interruptWith(any(ExecutionFailure.class))).thenReturn(Completable.complete());

        requestHeaders = HttpHeaders.create();
        lenient().when(request.headers()).thenReturn(requestHeaders);
        lenient().when(request.method()).thenReturn(HttpMethod.GET);
        lenient().when(request.parameters()).thenReturn(new LinkedMultiValueMap<>());
        lenient().when(request.pathInfo()).thenReturn("");
        lenient().when(request.webSocket()).thenReturn(callerWebSocket);
        responseHeaders = HttpHeaders.create();
        lenient().when(response.headers()).thenReturn(responseHeaders);

        lenient().when(callerWebSocket.upgrade()).thenReturn(Single.just(upgradedCallerWebSocket));
        lenient().when(callerWebSocket.close(anyInt())).thenReturn(Completable.complete());
        lenient().when(upgradedCallerWebSocket.getDelegate()).thenReturn(mock(ServerWebSocket.class));

        configuration = new HttpProxyEndpointConnectorConfiguration();
        configuration.setTarget("http://localhost:" + backend.actualPort() + "/ws");
        sharedConfiguration = new HttpProxyEndpointConnectorSharedConfiguration();
    }

    @AfterEach
    void stopBackend() {
        backend.close();
    }

    @Test
    void should_open_backend_websocket_when_caller_offers_client_max_window_bits() throws Exception {
        requestHeaders.set("Sec-WebSocket-Extensions", "permessage-deflate; client_max_window_bits");

        connect();

        verify(ctx, never()).interruptWith(any(ExecutionFailure.class));
        verify(callerWebSocket).upgrade();
    }

    @Test
    void should_keep_caller_extension_offer_for_the_caller_side_handshake() throws Exception {
        requestHeaders.set("Sec-WebSocket-Extensions", "permessage-deflate; client_max_window_bits");

        connect();

        assertThat(requestHeaders.get("Sec-WebSocket-Extensions")).isEqualTo("permessage-deflate; client_max_window_bits");
    }

    @Test
    void should_not_relay_caller_extension_offer_to_backend() throws Exception {
        requestHeaders.set("Sec-WebSocket-Extensions", "permessage-deflate; client_max_window_bits");
        sharedConfiguration.getHttpOptions().setUseCompression(false);

        connect();

        assertThat(backendReceivedExtensions.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isEmpty();
        verify(ctx, never()).interruptWith(any(ExecutionFailure.class));
    }

    @Test
    void should_not_relay_backend_extension_answer_to_caller() throws Exception {
        connect();

        assertThat(backendReceivedExtensions.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).contains("permessage-deflate");
        assertThat(responseHeaders.contains("Sec-WebSocket-Extensions")).isFalse();
        verify(callerWebSocket).upgrade();
    }

    private void connect() {
        new WebSocketConnector(configuration, sharedConfiguration, new HttpClientFactory())
            .connect(ctx)
            .test()
            .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .assertComplete();
    }

    /**
     * Accepts the first permessage-deflate offer and answers with the parameter set an RFC 7692 server commonly returns:
     * both no-context-takeover flags, its own window size, and a client window size only when the offer allowed one.
     */
    private void answerHandshakeLikeRfc7692Server(io.vertx.core.net.NetSocket socket) {
        final Buffer received = Buffer.buffer();
        socket.handler(chunk -> {
            if (backendReceivedExtensions.isDone()) {
                return;
            }
            received.appendBuffer(chunk);
            final String head = received.toString(StandardCharsets.ISO_8859_1);
            if (!head.contains("\r\n\r\n")) {
                return;
            }
            final String key = header(head, "Sec-WebSocket-Key");
            final String offer = header(head, "Sec-WebSocket-Extensions");
            backendReceivedExtensions.complete(offer);

            final StringBuilder handshake = new StringBuilder()
                .append("HTTP/1.1 101 Switching Protocols\r\n")
                .append("Upgrade: websocket\r\n")
                .append("Connection: Upgrade\r\n")
                .append("Sec-WebSocket-Accept: ")
                .append(accept(key))
                .append("\r\n");
            final String firstOffer = Arrays.stream(offer.split(","))
                .map(String::trim)
                .filter(extension -> extension.startsWith("permessage-deflate"))
                .findFirst()
                .orElse("");
            if (!firstOffer.isEmpty()) {
                handshake.append("Sec-WebSocket-Extensions: permessage-deflate; client_no_context_takeover; server_no_context_takeover");
                if (firstOffer.contains("client_max_window_bits")) {
                    handshake.append("; client_max_window_bits=15");
                }
                handshake.append("; server_max_window_bits=15\r\n");
            }
            socket.write(handshake.append("\r\n").toString());
        });
    }

    private static String header(String head, String name) {
        final String prefix = name.toLowerCase(Locale.ROOT) + ":";
        return head
            .lines()
            .filter(line -> line.toLowerCase(Locale.ROOT).startsWith(prefix))
            .map(line -> line.substring(prefix.length()).trim())
            .reduce((first, second) -> first + ", " + second)
            .orElse("");
    }

    private static String accept(String key) {
        try {
            final byte[] sha1 = MessageDigest.getInstance("SHA-1").digest((key + WEBSOCKET_GUID).getBytes(StandardCharsets.US_ASCII));
            return Base64.getEncoder().encodeToString(sha1);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute the WebSocket accept value", e);
        }
    }
}
