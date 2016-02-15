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
package io.gravitee.gateway.http.vertx;

import io.gravitee.gateway.api.http.BodyPart;
import io.vertx.core.buffer.Buffer;

import java.nio.ByteBuffer;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
class VertxBufferBodyPart implements BodyPart<ByteBuffer> {

    private final byte[] bytes;

    public VertxBufferBodyPart(Buffer buffer) {
        bytes = buffer.getBytes();
    }

    @Override
    public int length() {
        return bytes.length;
    }

    @Override
    public byte[] getBodyPartAsBytes() {
        return bytes;
    }

    @Override
    public ByteBuffer getBodyPart() {
        return ByteBuffer.wrap(bytes);
    }
}
