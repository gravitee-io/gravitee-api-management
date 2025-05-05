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
package io.gravitee.gateway.buffer.netty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.gravitee.gateway.api.buffer.Buffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class BufferImplTest {

    @Test
    void should_construct_default() {
        BufferImpl buffer = new BufferImpl();
        assertEquals(0, buffer.length());
    }

    @Test
    void should_construct_with_initial_size_hint() {
        BufferImpl buffer = new BufferImpl(10);
        assertEquals(0, buffer.length());
    }

    @Test
    void should_construct_with_byte_array() {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        BufferImpl buffer = new BufferImpl(data);
        assertArrayEquals(data, buffer.getBytes());
    }

    @Test
    void should_construct_with_string_and_charset_name() {
        BufferImpl buffer = new BufferImpl("hello", "UTF-8");
        assertEquals("hello", buffer.toString());
    }

    @Test
    void should_construct_with_string_and_charset() {
        BufferImpl buffer = new BufferImpl("hello", StandardCharsets.UTF_8);
        assertEquals("hello", buffer.toString(StandardCharsets.UTF_8));
    }

    @Test
    void should_construct_with_string_default_charset() {
        BufferImpl buffer = new BufferImpl("hello");
        assertEquals("hello", buffer.toString());
    }

    @Test
    void should_construct_with_null_string_and_charset_throws_null_pointer_exception() {
        assertThrows(NullPointerException.class, () -> new BufferImpl(null, StandardCharsets.UTF_8));
        assertThrows(NullPointerException.class, () -> new BufferImpl("text", (String) null));
    }

    @Test
    void should_construct_with_null_charset_throws_null_pointer_exception() {
        assertThrows(NullPointerException.class, () -> new BufferImpl("text", (Charset) null));
    }

    @Test
    void should_construct_with_null_string_default_charset_throws_null_pointer_exception() {
        assertThrows(NullPointerException.class, () -> new BufferImpl((String) null));
    }

    @Test
    void should_append_buffer_appends_correctly() {
        BufferImpl buffer1 = new BufferImpl("Hello");
        BufferImpl buffer2 = new BufferImpl(" World");

        buffer1.appendBuffer(buffer2);
        assertEquals("Hello World", buffer1.toString());
    }

    @Test
    void should_append_buffer_with_length_limit() {
        BufferImpl buffer1 = new BufferImpl("Hello");
        BufferImpl buffer2 = new BufferImpl(" World");

        buffer1.appendBuffer(buffer2, 3);
        assertEquals("Hello Wo", buffer1.toString());
    }

    @Test
    void should_append_buffer_with_length_zero() {
        BufferImpl buffer1 = new BufferImpl("Hello");
        BufferImpl buffer2 = new BufferImpl(" World");

        buffer1.appendBuffer(buffer2, 0);
        assertEquals("Hello", buffer1.toString());
    }

    @Test
    void should_append_buffer_with_length_greater_than_source() {
        BufferImpl buffer1 = new BufferImpl("Hello");
        BufferImpl buffer2 = new BufferImpl("!");

        buffer1.appendBuffer(buffer2, 100); // only "!" should be appended
        assertEquals("Hello!", buffer1.toString());
    }

    @Test
    void should_append_empty_buffer_to_non_empty() {
        BufferImpl buffer1 = new BufferImpl("Hello");
        BufferImpl buffer2 = new BufferImpl();

        buffer1.appendBuffer(buffer2);
        assertEquals("Hello", buffer1.toString());
    }

    @Test
    void should_append_to_empty_buffer() {
        BufferImpl buffer1 = new BufferImpl();
        BufferImpl buffer2 = new BufferImpl("Data");

        buffer1.appendBuffer(buffer2);
        assertEquals("Data", buffer1.toString());
    }

    @Test
    void should_append_string_with_charset_name() {
        BufferImpl buffer = new BufferImpl("Hello");
        buffer.appendString(" World", "UTF-8");
        assertEquals("Hello World", buffer.toString());
    }

    @Test
    void should_append_string_with_default_charset() {
        BufferImpl buffer = new BufferImpl("Hello");
        buffer.appendString(" World");
        assertEquals("Hello World", buffer.toString());
    }

    @Test
    void should_append_multiple_buffers_in_sequence() {
        BufferImpl buffer = new BufferImpl("Start");
        buffer.appendBuffer(new BufferImpl("->Step1")).appendString("->Step2").appendBuffer(new BufferImpl("->End"));

        assertEquals("Start->Step1->Step2->End", buffer.toString());
    }

    @Test
    void should_append_to_composite_buffer() {
        CompositeByteBuf composite = Unpooled.compositeBuffer();
        composite.addComponent(true, Unpooled.copiedBuffer("Hello ", CharsetUtil.UTF_8));
        BufferImpl buffer1 = new BufferImpl(composite);

        BufferImpl buffer2 = new BufferImpl("World");
        buffer1.appendBuffer(buffer2);

        assertEquals("Hello World", buffer1.toString());
    }

    @Test
    void should_append_large_number_of_buffers() {
        Buffer buffer = new BufferImpl();
        int count = 100000;
        String hello = "Hello";

        for (int i = 0; i < count; i++) {
            buffer = buffer.appendString(hello);
        }

        ByteBuf nativeBuffer = buffer.getNativeBuffer();

        assertThat(nativeBuffer).isInstanceOf(CompositeByteBuf.class);
        assertThat(nativeBuffer.readableBytes()).isEqualTo(count * hello.length());
        Buffer finalBuffer = buffer;
        assertDoesNotThrow(() -> finalBuffer.toString());
    }

    @Test
    void should_append_buffer_null_throws_null_pointer_exception() {
        BufferImpl buffer = new BufferImpl("data");

        assertThrows(NullPointerException.class, () -> buffer.appendBuffer(null));
    }

    @Test
    void should_append_buffer_with_length_null_throws_null_pointer_exception() {
        BufferImpl buffer = new BufferImpl("data");

        assertThrows(NullPointerException.class, () -> buffer.appendBuffer(null, 5));
    }

    @Test
    void should_append_string_with_null_string_throws_null_pointer_exception() {
        BufferImpl buffer = new BufferImpl("base");

        assertThrows(NullPointerException.class, () -> buffer.appendString(null));
    }

    @Test
    void should_append_string_with_null_charset_name_throws_null_pointer_exception() {
        BufferImpl buffer = new BufferImpl("base");

        assertThrows(NullPointerException.class, () -> buffer.appendString("text", null));
    }

    @Test
    void should_append_string_with_invalid_charset_name_throws_exception() {
        BufferImpl buffer = new BufferImpl("base");

        assertThrows(UnsupportedCharsetException.class, () -> buffer.appendString("test", "invalid-charset"));
    }

    @Test
    void should_to_string_with_charset_name() {
        BufferImpl buffer = new BufferImpl("test");
        assertEquals("test", buffer.toString("UTF-8"));
    }

    @Test
    void should_to_string_with_charset() {
        BufferImpl buffer = new BufferImpl("test");
        assertEquals("test", buffer.toString(StandardCharsets.UTF_8));
    }

    @Test
    void should_get_bytes_returns_correct_array() {
        byte[] bytes = "data".getBytes(StandardCharsets.UTF_8);
        BufferImpl buffer = new BufferImpl(bytes);
        assertArrayEquals(bytes, buffer.getBytes());
    }

    @Test
    void should_length_returns_correct_size() {
        BufferImpl buffer = new BufferImpl("abc");
        assertEquals(3, buffer.length());
    }

    @Test
    void should_get_native_buffer_returns_same_instance() {
        ByteBuf nativeBuf = Unpooled.buffer();
        BufferImpl buffer = new BufferImpl(nativeBuf);
        assertSame(nativeBuf, buffer.getNativeBuffer());
    }

    @Test
    void should_to_string_with_null_charset_name_throws_null_pointer_exception() {
        BufferImpl buffer = new BufferImpl("data");

        assertThrows(IllegalArgumentException.class, () -> buffer.toString((String) null));
    }

    @Test
    void should_to_string_with_invalid_charset_name_throws_exception() {
        BufferImpl buffer = new BufferImpl("data");

        assertThrows(UnsupportedCharsetException.class, () -> buffer.toString("invalid-charset"));
    }

    @Test
    void should_to_string_with_null_charset_throws_null_pointer_exception() {
        BufferImpl buffer = new BufferImpl("data");

        assertThrows(NullPointerException.class, () -> buffer.toString((Charset) null));
    }
}
