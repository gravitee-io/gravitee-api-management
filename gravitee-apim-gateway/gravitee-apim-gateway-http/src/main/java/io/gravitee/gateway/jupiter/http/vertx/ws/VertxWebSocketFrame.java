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

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.ws.WebSocketFrame;

/**
 *  @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class VertxWebSocketFrame implements WebSocketFrame {

    private final io.vertx.reactivex.core.http.WebSocketFrame frame;

    VertxWebSocketFrame(io.vertx.reactivex.core.http.WebSocketFrame frame) {
        this.frame = frame;
    }

    @Override
    public Type type() {
        switch (frame.type()) {
            case TEXT:
                return Type.TEXT;
            case BINARY:
                return Type.BINARY;
            case PING:
                return Type.PING;
            case PONG:
                return Type.PONG;
            case CONTINUATION:
                return Type.CONTINUATION;
            default:
                return Type.CLOSE;
        }
    }

    @Override
    public Buffer data() {
        return Buffer.buffer(frame.binaryData().getByteBuf());
    }

    @Override
    public boolean isFinal() {
        return frame.isFinal();
    }
}
