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
package io.gravitee.gateway.core.buffer.netty;

import io.gravitee.gateway.api.buffer.Buffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class BufferImpl implements Buffer {

    private ByteBuf buffer;

    BufferImpl() {
        this(0);
    }

    BufferImpl(int initialSizeHint) {
        buffer = Unpooled.unreleasableBuffer(Unpooled.buffer(initialSizeHint, Integer.MAX_VALUE));
    }

    BufferImpl(byte[] bytes) {
        buffer = Unpooled.unreleasableBuffer(Unpooled.buffer(bytes.length, Integer.MAX_VALUE)).writeBytes(bytes);
    }

    BufferImpl(String str, String enc) {
        this(str.getBytes(Charset.forName(Objects.requireNonNull(enc))));
    }

    BufferImpl(String str, Charset cs) {
        this(str.getBytes(cs));
    }

    BufferImpl(String str) {
        this(str, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Override
    public String toString(String enc) {
        return buffer.toString(Charset.forName(enc));
    }

    @Override
    public String toString(Charset enc) {
        return buffer.toString(enc);
    }

    @Override
    public byte[] getBytes() {
        byte[] arr = new byte[buffer.writerIndex()];
        buffer.getBytes(0, arr);
        return arr;
    }

    @Override
    public int length() {
        return buffer.writerIndex();
    }
}
