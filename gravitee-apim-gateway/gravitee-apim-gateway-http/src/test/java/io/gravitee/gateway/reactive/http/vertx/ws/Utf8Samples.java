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

/**
 * Text payloads covering every UTF-8 encoding length, plus byte sequences that no UTF-8 decoder accepts.
 *
 * @author GraviteeSource Team
 */
final class Utf8Samples {

    static final String ASCII = "websocket";
    /** U+00E9 "e with acute", two bytes in UTF-8. */
    static final String TWO_BYTE_CHARACTER = Character.toString(0x00E9);
    /** U+20AC euro sign, three bytes in UTF-8. */
    static final String THREE_BYTE_CHARACTER = Character.toString(0x20AC);
    /** U+10348 Gothic letter hwair, four bytes in UTF-8 and a surrogate pair in Java. */
    static final String FOUR_BYTE_CHARACTER = Character.toString(0x10348);

    /** First byte of a two-byte character, followed by an ASCII byte. */
    static final byte[] INVALID_BYTE_SEQUENCE = { (byte) 0xC3, (byte) 0x28 };
    /** A four-byte character cut off after three bytes. */
    static final byte[] TRUNCATED_FOUR_BYTE_SEQUENCE = { (byte) 0xF0, (byte) 0x90, (byte) 0x8D };
    /** U+002F slash written on two bytes instead of one; UTF-8 forbids longer forms. */
    static final byte[] OVERLONG_ENCODING = { (byte) 0xC0, (byte) 0xAF };
    /** U+D800, a UTF-16 surrogate, which UTF-8 forbids. */
    static final byte[] ENCODED_SURROGATE = { (byte) 0xED, (byte) 0xA0, (byte) 0x80 };
    /** U+110000, beyond the last Unicode code point. */
    static final byte[] ABOVE_MAX_CODE_POINT = { (byte) 0xF4, (byte) 0x90, (byte) 0x80, (byte) 0x80 };

    private Utf8Samples() {}
}
