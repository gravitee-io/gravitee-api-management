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
package io.gravitee.gateway.http.vertx.ws;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.ws.WebSocketFrame;
import io.vertx.core.http.WebSocketFrameType;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
class VertxWebSocketFrame implements WebSocketFrame {

    private final io.vertx.core.http.WebSocketFrame frame;

    VertxWebSocketFrame(io.vertx.core.http.WebSocketFrame frame) {
        this.frame = frame;
    }

    @Override
    public Type type() {
        return frame.isBinary()
            ? Type.BINARY
            : frame.isClose()
                ? Type.CLOSE
                : frame.isContinuation()
                    ? Type.CONTINUATION
                    : frame.isText()
                        ? Type.TEXT
                        : frame.isPing() ? Type.PING : frame.type() == WebSocketFrameType.PONG ? Type.PONG : Type.CLOSE;
    }

    @Override
    public Buffer data() {
        return Buffer.buffer(frame.binaryData().getBytes());
    }

    @Override
    public boolean isFinal() {
        return frame.isFinal();
    }
}
