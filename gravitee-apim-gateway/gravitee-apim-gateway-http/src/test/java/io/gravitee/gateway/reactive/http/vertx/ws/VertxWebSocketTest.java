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

import static io.vertx.core.http.WebSocketFrameType.CLOSE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.reactive.api.ws.WebSocket;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClosedException;
import io.vertx.core.http.WebSocketFrameType;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import io.vertx.rxjava3.core.http.ServerWebSocket;
import io.vertx.rxjava3.core.http.WebSocketFrame;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class VertxWebSocketTest {

    protected static final String FRAME_DATA = "Test";
    protected static final String MOCK_EXCEPTION = "Mock exception";

    @Mock
    private HttpServerRequest httpServerRequest;

    @Mock
    private ServerWebSocket webSocket;

    @Mock
    private WebSocketFrame webSocketFrame;

    private VertxWebSocket cut;

    @BeforeEach
    void init() {
        cut = new VertxWebSocket(httpServerRequest);
    }

    @Nested
    class Upgrade {

        @Test
        void should_upgrade_to_websocket() throws ExecutionException, InterruptedException {
            when(httpServerRequest.rxToWebSocket()).thenReturn(Single.just(mock(ServerWebSocket.class)));

            final TestObserver<WebSocket> obs = cut.upgrade().test();

            obs.assertValue(cut);
            assertTrue(cut.upgraded());
        }

        @Test
        void should_fail_when_upgrade_to_web_socket_fails() {
            when(httpServerRequest.rxToWebSocket()).thenReturn(Single.error(new RuntimeException(MOCK_EXCEPTION)));

            final TestObserver<WebSocket> obs = cut.upgrade().test();

            obs.assertError(RuntimeException.class);
            obs.assertError(t -> MOCK_EXCEPTION.equals(t.getMessage()));
            assertFalse(cut.upgraded());
        }

        @Test
        void should_upgrade_to_websocket_only_once() {
            when(httpServerRequest.rxToWebSocket()).thenReturn(Single.just(mock(ServerWebSocket.class)));

            for (int i = 0; i < 10; i++) {
                final TestObserver<WebSocket> obs = cut.upgrade().test();

                obs.assertValue(cut);
                assertTrue(cut.upgraded());
            }

            verify(httpServerRequest).rxToWebSocket();
        }
    }

    @Nested
    class Close {

        @Test
        void should_close_with_status() {
            ReflectionTestUtils.setField(cut, "upgraded", true);
            ReflectionTestUtils.setField(cut, "webSocket", webSocket);

            when(webSocket.rxClose((short) 400)).thenReturn(Completable.complete());
            final TestObserver<Void> obs = cut.close(400).test();

            obs.assertComplete();
        }

        @Test
        void should_close_without_status() {
            ReflectionTestUtils.setField(cut, "upgraded", true);
            ReflectionTestUtils.setField(cut, "webSocket", webSocket);

            when(webSocket.rxClose()).thenReturn(Completable.complete());
            final TestObserver<Void> obs = cut.close().test();

            obs.assertComplete();
        }

        @Test
        void should_close_with_status_and_reason() {
            ReflectionTestUtils.setField(cut, "upgraded", true);
            ReflectionTestUtils.setField(cut, "webSocket", webSocket);

            when(webSocket.rxClose((short) 1001, "error")).thenReturn(Completable.complete());
            final TestObserver<Void> obs = cut.close(1001, "error").test();

            obs.assertComplete();
        }

        @Test
        void should_not_reject_if_not_upgraded() {
            ReflectionTestUtils.setField(cut, "upgraded", false);

            cut.close().test().assertComplete();
            cut.close(400).test().assertComplete();
            cut.close(1001, "error").test().assertComplete();

            verify(webSocket, never()).rxClose(anyShort());
        }

        @Test
        void should_not_close_if_already_closed() {
            ReflectionTestUtils.setField(cut, "upgraded", true);
            ReflectionTestUtils.setField(cut, "webSocket", webSocket);

            when(webSocket.isClosed()).thenReturn(true);
            cut.close().test().assertComplete();
            cut.close(400).test().assertComplete();
            cut.close(1001, "error").test().assertComplete();

            verify(webSocket, never()).rxClose();
        }
    }

    @Nested
    class CloseHandler {

        @Test
        void should_set_close_handler_on_web_socket() {
            ReflectionTestUtils.setField(cut, "upgraded", true);
            ReflectionTestUtils.setField(cut, "webSocket", webSocket);

            cut.closeHandler(result -> {});

            verify(webSocket).closeHandler(any(Handler.class));
        }

        @Test
        void should_not_set_close_handler_if_not_upgraded() {
            ReflectionTestUtils.setField(cut, "upgraded", false);

            cut.closeHandler(result -> {});

            verifyNoInteractions(webSocket);
        }
    }

    @Nested
    class FrameHandler {

        @Test
        void should_set_frame_handler_on_web_socket() {
            ReflectionTestUtils.setField(cut, "upgraded", true);
            ReflectionTestUtils.setField(cut, "webSocket", webSocket);

            cut.frameHandler(result -> {});

            verify(webSocket).frameHandler(any(Handler.class));
        }

        @Test
        void should_not_set_frame_handler_if_not_upgraded() {
            ReflectionTestUtils.setField(cut, "upgraded", false);

            cut.frameHandler(result -> {});

            verifyNoInteractions(webSocket);
        }
    }

    @Nested
    class Write {

        @Test
        void should_write_buffer() {
            ReflectionTestUtils.setField(cut, "upgraded", true);
            ReflectionTestUtils.setField(cut, "webSocket", webSocket);

            final io.gravitee.gateway.api.buffer.Buffer buffer = io.gravitee.gateway.api.buffer.Buffer.buffer("Test");
            when(webSocket.rxWrite(any(Buffer.class))).thenReturn(Completable.complete());

            final TestObserver<Void> obs = cut.write(buffer).test();

            obs.assertComplete();
            verify(webSocket).rxWrite(argThat(b -> b.toString().equals(buffer.toString())));
        }

        @Test
        void should_not_write_if_not_upgraded() {
            ReflectionTestUtils.setField(cut, "upgraded", false);

            final TestObserver<Void> obs = cut.write(io.gravitee.gateway.api.buffer.Buffer.buffer("test")).test();
            obs.assertComplete();
            verifyNoInteractions(webSocket);
        }
    }

    @Nested
    class WriteFrame {

        @ParameterizedTest
        @EnumSource(value = WebSocketFrameType.class, names = { "CLOSE" }, mode = EnumSource.Mode.EXCLUDE)
        void should_write_frame(WebSocketFrameType frameType) {
            ReflectionTestUtils.setField(cut, "upgraded", true);
            ReflectionTestUtils.setField(cut, "webSocket", webSocket);

            when(webSocketFrame.type()).thenReturn(frameType);
            when(webSocketFrame.binaryData()).thenReturn(Buffer.buffer(FRAME_DATA));
            when(webSocket.rxWriteFrame(any())).thenReturn(Completable.complete());

            final TestObserver<Void> obs = cut.writeFrame(new VertxWebSocketFrame(webSocketFrame)).test();
            obs.assertComplete();
            verify(webSocket).rxWriteFrame(argThat(frame -> frame.type() == frameType && FRAME_DATA.equals(frame.textData())));
        }

        @Test
        void should_write_close_frame() {
            ReflectionTestUtils.setField(cut, "upgraded", true);
            ReflectionTestUtils.setField(cut, "webSocket", webSocket);

            when(webSocketFrame.type()).thenReturn(CLOSE);
            when(webSocket.rxClose()).thenReturn(Completable.complete());

            final TestObserver<Void> obs = cut.writeFrame(new VertxWebSocketFrame(webSocketFrame)).test();
            obs.assertComplete();
            verify(webSocket).rxClose();
        }

        @Test
        void should_not_write_frame_if_not_upgraded() {
            ReflectionTestUtils.setField(cut, "upgraded", false);

            final TestObserver<Void> obs = cut.writeFrame(new VertxWebSocketFrame(webSocketFrame)).test();
            obs.assertComplete();
            verifyNoInteractions(webSocket);
        }
    }

    @Nested
    class WritePing {

        @Test
        void should_write_ping_frame() {
            ReflectionTestUtils.setField(cut, "upgraded", true);
            ReflectionTestUtils.setField(cut, "webSocket", webSocket);

            when(webSocket.writePing(any(Buffer.class))).thenReturn(Completable.complete());

            cut.writePing().test().assertComplete();
            verify(webSocket).writePing(eq(Buffer.buffer("ping_pong")));
        }

        @Test
        void should_not_write_ping_frame_if_not_upgraded() {
            ReflectionTestUtils.setField(cut, "upgraded", false);

            cut.writePing().test().assertComplete();
            verifyNoInteractions(webSocket);
        }
    }

    @Nested
    class Read {

        @Test
        void should_read_buffers() {
            ReflectionTestUtils.setField(cut, "upgraded", true);
            ReflectionTestUtils.setField(cut, "webSocket", webSocket);

            final Buffer buffer1 = Buffer.buffer("Test1");
            final Buffer buffer2 = Buffer.buffer("Test2");
            final Buffer buffer3 = Buffer.buffer("Test3");

            when(webSocket.toFlowable()).thenReturn(Flowable.just(buffer1, buffer2, buffer3));

            final TestSubscriber<io.gravitee.gateway.api.buffer.Buffer> obs = cut.read().test();

            obs.assertComplete();
            obs.assertValueCount(3);
            obs.assertValueAt(0, buffer -> buffer.toString().equals(buffer1.toString()));
            obs.assertValueAt(1, buffer -> buffer.toString().equals(buffer2.toString()));
            obs.assertValueAt(2, buffer -> buffer.toString().equals(buffer3.toString()));
        }

        @Test
        void should_read_empty_if_not_upgraded() {
            ReflectionTestUtils.setField(cut, "upgraded", false);

            final TestSubscriber<io.gravitee.gateway.api.buffer.Buffer> obs = cut.read().test();

            obs.assertNoValues();
        }

        @Test
        void should_throw_when_error_occurs_when_reading() {
            ReflectionTestUtils.setField(cut, "upgraded", true);
            ReflectionTestUtils.setField(cut, "webSocket", webSocket);

            final Buffer buffer1 = Buffer.buffer("Test1");
            final Buffer buffer2 = Buffer.buffer("Test2");
            final Buffer buffer3 = Buffer.buffer("Test3");

            when(webSocket.toFlowable()).thenReturn(
                Flowable.just(buffer1, buffer2, buffer3).concatWith(Flowable.error(new RuntimeException(MOCK_EXCEPTION)))
            );

            final TestSubscriber<io.gravitee.gateway.api.buffer.Buffer> obs = cut.read().test();

            obs.assertValueCount(3);
            obs.assertValueAt(0, buffer -> buffer.toString().equals(buffer1.toString()));
            obs.assertValueAt(1, buffer -> buffer.toString().equals(buffer2.toString()));
            obs.assertValueAt(2, buffer -> buffer.toString().equals(buffer3.toString()));
            obs.assertError(RuntimeException.class);
            obs.assertError(t -> MOCK_EXCEPTION.equals(t.getMessage()));
        }

        @Test
        void should_end_when_http_close_occurs_when_reading() {
            ReflectionTestUtils.setField(cut, "upgraded", true);
            ReflectionTestUtils.setField(cut, "webSocket", webSocket);

            final Buffer buffer1 = Buffer.buffer("Test1");
            final Buffer buffer2 = Buffer.buffer("Test2");
            final Buffer buffer3 = Buffer.buffer("Test3");

            when(webSocket.toFlowable()).thenReturn(
                Flowable.just(buffer1, buffer2, buffer3).concatWith(Flowable.error(new HttpClosedException("")))
            );

            final TestSubscriber<io.gravitee.gateway.api.buffer.Buffer> obs = cut.read().test();

            obs.assertComplete();
            obs.assertValueCount(3);
            obs.assertValueAt(0, buffer -> buffer.toString().equals(buffer1.toString()));
            obs.assertValueAt(1, buffer -> buffer.toString().equals(buffer2.toString()));
            obs.assertValueAt(2, buffer -> buffer.toString().equals(buffer3.toString()));
        }
    }

    @Test
    void shouldCheckClosed() {
        ReflectionTestUtils.setField(cut, "webSocket", webSocket);

        when(webSocket.isClosed()).thenReturn(true);
        assertTrue(cut.closed());
    }
}
