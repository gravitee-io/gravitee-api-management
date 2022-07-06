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
package io.gravitee.gateway.jupiter.http.vertx.ws;

import static io.vertx.core.http.WebSocketFrameType.CLOSE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.ws.WebSocket;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.WebSocketFrameType;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.ServerWebSocket;
import io.vertx.reactivex.core.http.WebSocketFrame;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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

    @Mock
    private HttpServerRequest httpServerRequest;

    @Mock
    private ServerWebSocket webSocket;

    @Mock
    private WebSocketFrame webSocketFrame;

    @Captor
    private ArgumentCaptor<Handler<AsyncResult<ServerWebSocket>>> handlerCaptor;

    private VertxWebSocket cut;

    @BeforeEach
    void init() throws Exception {
        lenient().doNothing().when(httpServerRequest).toWebSocket(handlerCaptor.capture());
        cut = new VertxWebSocket(httpServerRequest);
    }

    @Test
    void shouldUpgradeToWebSocket() throws ExecutionException, InterruptedException {
        final CompletableFuture<WebSocket> upgradeFuture = cut.upgrade();
        final AsyncResult<ServerWebSocket> asyncResult = mock(AsyncResult.class);
        when(asyncResult.failed()).thenReturn(false);
        when(asyncResult.result()).thenReturn(webSocket);

        handlerCaptor.getValue().handle(asyncResult);

        assertEquals(cut, upgradeFuture.get());
        assertTrue(cut.upgraded());
    }

    @Test
    void shouldFailWhenUpgradeToWebSocketFails() {
        final CompletableFuture<WebSocket> upgradeFuture = cut.upgrade();
        final AsyncResult<ServerWebSocket> asyncResult = mock(AsyncResult.class);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(new RuntimeException("Mock exception"));

        handlerCaptor.getValue().handle(asyncResult);

        assertThrows(Exception.class, upgradeFuture::get, "Mock exception");
        assertFalse(cut.upgraded());
    }

    @Test
    void shouldUpgradeToWebSocketOnce() throws ExecutionException, InterruptedException {
        final CompletableFuture<WebSocket> upgradeFuture = cut.upgrade();
        final AsyncResult<ServerWebSocket> asyncResult = mock(AsyncResult.class);
        when(asyncResult.failed()).thenReturn(false);
        when(asyncResult.result()).thenReturn(webSocket);

        handlerCaptor.getValue().handle(asyncResult);

        assertEquals(cut, upgradeFuture.get());
        assertTrue(cut.upgraded());

        for (int i = 0; i < 10; i++) {
            cut.upgrade().get();
        }

        verify(httpServerRequest).toWebSocket(any(Handler.class));
    }

    @Test
    void shouldReject() {
        ReflectionTestUtils.setField(cut, "upgraded", true);
        ReflectionTestUtils.setField(cut, "webSocket", webSocket);

        cut.reject(400);
        verify(webSocket).close(eq((short) 400));
    }

    @Test
    void shouldNotRejectIfNotUpgraded() {
        ReflectionTestUtils.setField(cut, "upgraded", false);

        cut.reject(400);
        verifyNoInteractions(webSocket);
    }

    @Test
    void shouldClose() {
        ReflectionTestUtils.setField(cut, "upgraded", true);
        ReflectionTestUtils.setField(cut, "webSocket", webSocket);

        cut.close();
        verify(webSocket).close();
    }

    @Test
    void shouldNotCloseIfNotUpgraded() {
        ReflectionTestUtils.setField(cut, "upgraded", false);

        cut.close();
        verifyNoInteractions(webSocket);
    }

    @Test
    void shouldNotCloseIfAlreadyClosed() {
        ReflectionTestUtils.setField(cut, "upgraded", true);
        ReflectionTestUtils.setField(cut, "closed", true);

        cut.close();
        verifyNoInteractions(webSocket);
    }

    @Test
    void shouldSetCloseHandlerOnWebSocket() {
        ReflectionTestUtils.setField(cut, "upgraded", true);
        ReflectionTestUtils.setField(cut, "webSocket", webSocket);

        cut.closeHandler(result -> {});

        verify(webSocket).closeHandler(any(Handler.class));
    }

    @Test
    void shouldInvokeCloseHandler() {
        ReflectionTestUtils.setField(cut, "upgraded", true);
        ReflectionTestUtils.setField(cut, "webSocket", webSocket);

        final io.gravitee.gateway.api.handler.Handler<Void> closeHandler = mock(io.gravitee.gateway.api.handler.Handler.class);
        cut.closeHandler(closeHandler);

        final ArgumentCaptor<Handler> closeHandlerCaptor = ArgumentCaptor.forClass(Handler.class);
        verify(webSocket).closeHandler(closeHandlerCaptor.capture());

        closeHandlerCaptor.getValue().handle(null);
        verify(closeHandler).handle(null);
        assertTrue((Boolean) ReflectionTestUtils.getField(cut, "closed"));
    }

    @Test
    void shouldNotSetCloseHandlerOnWebSocketIfNotUpgraded() {
        ReflectionTestUtils.setField(cut, "upgraded", false);

        cut.closeHandler(result -> {});

        verifyNoInteractions(webSocket);
    }

    @Test
    void shouldSetFrameHandlerOnWebSocket() {
        ReflectionTestUtils.setField(cut, "upgraded", true);
        ReflectionTestUtils.setField(cut, "webSocket", webSocket);

        cut.frameHandler(result -> {});

        verify(webSocket).frameHandler(any(Handler.class));
    }

    @Test
    void shouldNotSetFrameHandlerOnWebSocketIfNotUpgraded() {
        ReflectionTestUtils.setField(cut, "upgraded", false);

        cut.frameHandler(result -> {});

        verifyNoInteractions(webSocket);
    }

    @ParameterizedTest
    @EnumSource(value = WebSocketFrameType.class, names = { "CLOSE" }, mode = EnumSource.Mode.EXCLUDE)
    void shouldWriteFrame(WebSocketFrameType frameType) {
        ReflectionTestUtils.setField(cut, "upgraded", true);
        ReflectionTestUtils.setField(cut, "webSocket", webSocket);

        when(webSocketFrame.type()).thenReturn(frameType);
        when(webSocketFrame.binaryData()).thenReturn(Buffer.buffer(FRAME_DATA));

        cut.write(new VertxWebSocketFrame(webSocketFrame));
        verify(webSocket).writeFrame(argThat(frame -> frame.type() == frameType && FRAME_DATA.equals(frame.textData())));
    }

    @Test
    void shouldWriteCloseFrame() {
        ReflectionTestUtils.setField(cut, "upgraded", true);
        ReflectionTestUtils.setField(cut, "webSocket", webSocket);

        when(webSocketFrame.type()).thenReturn(CLOSE);

        cut.write(new VertxWebSocketFrame(webSocketFrame));
        verify(webSocket).close();
    }

    @Test
    void shouldNotWriteFrameIfNotUpgraded() {
        ReflectionTestUtils.setField(cut, "upgraded", false);

        cut.write(new VertxWebSocketFrame(webSocketFrame));
        verifyNoInteractions(webSocket);
    }
}
