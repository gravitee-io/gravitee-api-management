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
package io.gravitee.gateway.reactive.http.vertx.ws;

import static io.gravitee.gateway.reactive.http.vertx.ws.Utf8Samples.FOUR_BYTE_CHARACTER;
import static io.gravitee.gateway.reactive.http.vertx.ws.Utf8Samples.THREE_BYTE_CHARACTER;
import static io.gravitee.gateway.reactive.http.vertx.ws.Utf8Samples.TWO_BYTE_CHARACTER;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.gravitee.gateway.api.buffer.Buffer;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.WebSocketFrame;
import io.vertx.core.http.WebSocketFrameType;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import io.vertx.rxjava3.core.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Fragmentation of a text message is Vert.x behavior, so these cases run through a real server and client.
 *
 * @author GraviteeSource Team
 */
class VertxWebSocketTextFramesTest {

    private static final int MAX_FRAME_SIZE = 16;
    private static final long TIMEOUT_SECONDS = 10;
    private static final String ASCII_FILLER = "a";

    private static final String TEXT_FITTING_ONE_FRAME = TWO_BYTE_CHARACTER + THREE_BYTE_CHARACTER + FOUR_BYTE_CHARACTER;
    private static final String TEXT_SPANNING_THREE_FRAMES = ASCII_FILLER.repeat(MAX_FRAME_SIZE * 3);
    /** The 4-byte character starts at byte 14, so bytes 16 and 17 fall into the second frame. */
    private static final String TEXT_WITH_CHARACTER_ACROSS_FRAME_BOUNDARY =
        ASCII_FILLER.repeat(MAX_FRAME_SIZE - 2) + FOUR_BYTE_CHARACTER + ASCII_FILLER.repeat(2);

    private Vertx vertx;

    @BeforeEach
    void init() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    void cleanup() {
        assertTrue(vertx.close().blockingAwait(TIMEOUT_SECONDS, SECONDS));
    }

    @Test
    void should_deliver_text_message_with_multibyte_characters_in_a_single_frame() throws Exception {
        assertTrue(TEXT_FITTING_ONE_FRAME.getBytes(StandardCharsets.UTF_8).length <= MAX_FRAME_SIZE);

        final Received received = sendText(TEXT_FITTING_ONE_FRAME);

        assertEquals(TEXT_FITTING_ONE_FRAME, received.message());
        assertEquals(
            List.of(new ReceivedFrame(WebSocketFrameType.TEXT, TEXT_FITTING_ONE_FRAME.getBytes(StandardCharsets.UTF_8).length)),
            received.frames()
        );
    }

    @Test
    void should_deliver_large_text_as_one_message_split_in_several_frames() throws Exception {
        final Received received = sendText(TEXT_SPANNING_THREE_FRAMES);

        assertEquals(TEXT_SPANNING_THREE_FRAMES, received.message());
        assertEquals(
            List.of(
                new ReceivedFrame(WebSocketFrameType.TEXT, MAX_FRAME_SIZE),
                new ReceivedFrame(WebSocketFrameType.CONTINUATION, MAX_FRAME_SIZE),
                new ReceivedFrame(WebSocketFrameType.CONTINUATION, MAX_FRAME_SIZE)
            ),
            received.frames()
        );
    }

    @Test
    void should_keep_a_four_byte_character_split_across_frames_intact() throws Exception {
        assertTrue(TEXT_WITH_CHARACTER_ACROSS_FRAME_BOUNDARY.getBytes(StandardCharsets.UTF_8).length > MAX_FRAME_SIZE);

        final Received received = sendText(TEXT_WITH_CHARACTER_ACROSS_FRAME_BOUNDARY);

        assertEquals(TEXT_WITH_CHARACTER_ACROSS_FRAME_BOUNDARY, received.message());
        final int remainingBytes = TEXT_WITH_CHARACTER_ACROSS_FRAME_BOUNDARY.getBytes(StandardCharsets.UTF_8).length - MAX_FRAME_SIZE;
        assertEquals(
            List.of(
                new ReceivedFrame(WebSocketFrameType.TEXT, MAX_FRAME_SIZE),
                new ReceivedFrame(WebSocketFrameType.CONTINUATION, remainingBytes)
            ),
            received.frames()
        );
    }

    private Received sendText(String text) throws Exception {
        final ClientProbe probe = new ClientProbe();
        final int port = startServer(request -> writeTextOnUpgrade(request, text, probe));
        connectClient(port, probe);
        return probe.await();
    }

    private int startServer(Handler<HttpServerRequest> requestHandler) {
        return vertx
            .createHttpServer(new HttpServerOptions().setMaxWebSocketFrameSize(MAX_FRAME_SIZE))
            .requestHandler(requestHandler)
            .rxListen(0)
            .blockingGet()
            .actualPort();
    }

    private static void writeTextOnUpgrade(HttpServerRequest request, String text, ClientProbe probe) {
        new VertxWebSocket(request)
            .upgrade()
            .flatMapCompletable(webSocket -> webSocket.writeText(Buffer.buffer(text)))
            .subscribe(() -> {}, probe::fail);
    }

    private void connectClient(int port, ClientProbe probe) {
        vertx.createWebSocketClient().rxConnect(port, "localhost", "/").subscribe(probe::listenTo, probe::fail);
    }

    /** What the client receives: the reassembled text message, and every frame up to the final one. */
    private static final class ClientProbe {

        private final CompletableFuture<String> message = new CompletableFuture<>();
        private final CompletableFuture<List<ReceivedFrame>> frames = new CompletableFuture<>();
        private final List<ReceivedFrame> seenFrames = new CopyOnWriteArrayList<>();

        void listenTo(WebSocket socket) {
            socket.frameHandler(this::onFrame);
            socket.textMessageHandler(message::complete);
        }

        void fail(Throwable error) {
            message.completeExceptionally(error);
            frames.completeExceptionally(error);
        }

        Received await() throws Exception {
            return new Received(message.get(TIMEOUT_SECONDS, SECONDS), frames.get(TIMEOUT_SECONDS, SECONDS));
        }

        private void onFrame(WebSocketFrame frame) {
            seenFrames.add(new ReceivedFrame(frame.type(), frame.binaryData().length()));
            if (frame.isFinal()) {
                frames.complete(List.copyOf(seenFrames));
            }
        }
    }

    private record ReceivedFrame(WebSocketFrameType type, int size) {}

    private record Received(String message, List<ReceivedFrame> frames) {}
}
