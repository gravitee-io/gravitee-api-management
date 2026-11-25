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
package io.vertx.core.http.impl;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import io.vertx.core.http.HttpServerOptions;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VertxHttpRequestDecoderTest {

    private final EmbeddedChannel channel = new EmbeddedChannel(new VertxHttpRequestDecoder(new HttpServerOptions()));

    @AfterEach
    void close() {
        channel.finishAndReleaseAll();
    }

    @ParameterizedTest
    @ValueSource(strings = { "host", "accept", "connection", "content-type", "content-length" })
    void should_keep_header_name_as_sent_for_every_case_variation(String name) {
        for (String variant : caseVariations(name)) {
            assertThat(decodeHeaderName(variant)).hasToString(variant);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "host", "accept", "connection", "content-type", "content-length" })
    void should_keep_header_name_as_sent_when_one_letter_differs(String name) {
        for (String capitalized : List.of(name, capitalize(name))) {
            for (int i = 0; i < capitalized.length(); i++) {
                if (!Character.isLetter(capitalized.charAt(i))) {
                    continue;
                }
                for (char c = 'a'; c <= 'z'; c++) {
                    String mutated = capitalized.substring(0, i) + c + capitalized.substring(i + 1);
                    assertThat(decodeHeaderName(mutated)).hasToString(mutated);
                }
            }
        }
    }

    @Test
    void should_intern_lower_case_well_known_header_names() {
        assertThat(decodeHeaderName("content-length")).isSameAs(HttpHeaderNames.CONTENT_LENGTH);
        assertThat(decodeHeaderName("content-type")).isSameAs(HttpHeaderNames.CONTENT_TYPE);
        assertThat(decodeHeaderName("host")).isSameAs(HttpHeaderNames.HOST);
    }

    @Test
    void should_not_frame_the_body_with_a_lookalike_content_length_header() {
        channel.writeInbound(
            Unpooled.copiedBuffer("POST / HTTP/1.1\r\nHost: localhost\r\nContent-Xength: 5\r\n\r\nhello", StandardCharsets.US_ASCII)
        );

        HttpRequest request = channel.readInbound();
        assertThat(request.headers().contains(HttpHeaderNames.CONTENT_LENGTH)).isFalse();
        HttpContent content = channel.readInbound();
        try {
            assertThat(content).isInstanceOf(LastHttpContent.class);
            assertThat(content.content().readableBytes()).isZero();
        } finally {
            ReferenceCountUtil.release(content);
        }
    }

    private CharSequence decodeHeaderName(String name) {
        channel.writeInbound(Unpooled.copiedBuffer("GET / HTTP/1.1\r\n" + name + ": 0\r\n\r\n", StandardCharsets.US_ASCII));
        HttpRequest request = channel.readInbound();
        Object last = channel.readInbound();
        ReferenceCountUtil.release(last);
        return request.headers().iteratorCharSequence().next().getKey();
    }

    private static String capitalize(String name) {
        StringBuilder sb = new StringBuilder(name);
        for (int i = 0; i < sb.length(); i++) {
            if (i == 0 || sb.charAt(i - 1) == '-') {
                sb.setCharAt(i, Character.toUpperCase(sb.charAt(i)));
            }
        }
        return sb.toString();
    }

    private static List<String> caseVariations(String name) {
        List<String> variants = new ArrayList<>();
        variants.add("");
        for (char c : name.toCharArray()) {
            List<String> next = new ArrayList<>();
            for (String prefix : variants) {
                next.add(prefix + Character.toLowerCase(c));
                if (Character.isLetter(c)) {
                    next.add(prefix + Character.toUpperCase(c));
                }
            }
            variants = next;
        }
        return variants;
    }
}
