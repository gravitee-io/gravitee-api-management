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
package io.gravitee.gateway.jupiter.policy.adapter.context;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.ws.WebSocketFrame;
import io.gravitee.gateway.jupiter.http.vertx.ws.VertxWebSocket;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class WebSocketAdapterTest {

    protected static final String MOCK_EXCEPTION = "Mock exception";

    @Mock
    private VertxWebSocket webSocket;

    private WebSocketAdapter cut;

    @BeforeEach
    void init() {
        cut = new WebSocketAdapter(webSocket);
    }

    @Test
    void shouldUpgrade() throws ExecutionException, InterruptedException {
        when(webSocket.upgrade()).thenReturn(Single.just(webSocket));

        final CompletableFuture<io.gravitee.gateway.api.ws.WebSocket> future = cut.upgrade();

        assertNotNull(future.get());
    }

    @Test
    void shouldErrorWhenUpgradeFails() {
        when(webSocket.upgrade()).thenReturn(Single.error(new RuntimeException(MOCK_EXCEPTION)));

        assertThrows(ExecutionException.class, () -> cut.upgrade().get());
    }

    @Test
    void shouldClose() {
        when(webSocket.close()).thenReturn(Completable.complete());

        assertSame(cut, cut.close());
    }

    @Test
    void shouldIgnoreAndLogErrorWhenCloseFails() {
        when(webSocket.close()).thenReturn(Completable.complete());

        assertSame(cut, cut.close());
    }

    @Test
    void shouldReject() {
        when(webSocket.close(500)).thenReturn(Completable.complete());

        assertSame(cut, cut.reject(500));
    }

    @Test
    void shouldIgnoreAndLogErrorWhenRejectFails() {
        when(webSocket.close(500)).thenReturn(Completable.error(new RuntimeException("Mock error")));

        assertSame(cut, cut.reject(500));
        verify(webSocket).close(500);
    }

    @Test
    void shouldWriteFrame() {
        final WebSocketFrame frame = mock(WebSocketFrame.class);
        when(webSocket.writeFrame(frame)).thenReturn(Completable.complete());

        assertSame(cut, cut.write(frame));
    }

    @Test
    void shouldIgnoreAndLogErrorWhenWriteFrameFails() {
        final WebSocketFrame frame = mock(WebSocketFrame.class);
        when(webSocket.writeFrame(frame)).thenReturn(Completable.error(new RuntimeException("Mock error")));

        assertSame(cut, cut.write(frame));
    }

    @Test
    void shouldSetFrameHandler() {
        final Handler<WebSocketFrame> frameHandler = frame -> {};

        assertSame(cut, cut.frameHandler(frameHandler));

        verify(webSocket).frameHandler(frameHandler);
    }

    @Test
    void shouldSetCloseHandler() {
        final Handler<Void> closeHandler = useless -> {};

        assertSame(cut, cut.closeHandler(closeHandler));

        verify(webSocket).closeHandler(closeHandler);
    }

    @Test
    void shouldCallUpgraded() {
        assertFalse(cut.upgraded());

        verify(webSocket).upgraded();
    }
}
