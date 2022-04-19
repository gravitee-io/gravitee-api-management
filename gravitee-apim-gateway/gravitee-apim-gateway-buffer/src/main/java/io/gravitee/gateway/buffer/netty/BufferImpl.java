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
package io.gravitee.gateway.buffer.netty;

import io.gravitee.gateway.api.buffer.Buffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class BufferImpl implements Buffer {

    private final ByteBuf buffer;

    BufferImpl() {
        this(0);
    }

    BufferImpl(ByteBuf nativeBuffer) {
        this.buffer = nativeBuffer;
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
    public Buffer appendBuffer(Buffer buff) {
        ByteBuf cb = buff.getNativeBuffer();
        return appendBuf(cb, cb.readableBytes());
    }

    @Override
    public Buffer appendBuffer(Buffer buff, int length) {
        ByteBuf cb = buff.getNativeBuffer();
        return appendBuf(cb, Math.min(buff.length(), length));
    }

    @Override
    public Buffer appendString(String str, String enc) {
        return append(str, Charset.forName(Objects.requireNonNull(enc)));
    }

    @Override
    public Buffer appendString(String str) {
        return append(str, CharsetUtil.UTF_8);
    }

    private Buffer append(String str, Charset charset) {
        byte[] bytes = str.getBytes(charset);
        buffer.writeBytes(bytes);
        return this;
    }

    private Buffer appendBuf(ByteBuf cb, int length) {
        final int currIndex = cb.readerIndex();
        buffer.writeBytes(cb, length);
        cb.readerIndex(currIndex); // Need to reset reader index since Netty write modifies readerIndex of source!
        return this;
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
        byte[] arr = new byte[buffer.writerIndex() - buffer.readerIndex()];
        buffer.getBytes(buffer.readerIndex(), arr);
        return arr;
    }

    @Override
    public int length() {
        return buffer.writerIndex();
    }

    @Override
    public ByteBuf getNativeBuffer() {
        return buffer;
    }
}
