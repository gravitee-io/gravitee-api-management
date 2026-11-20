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
package io.gravitee.gateway.reactive.core.v4.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.api.buffer.Buffer;
import org.junit.jupiter.api.Test;

class BufferUtilsTest {

    @Test
    void should_append_whole_chunk_when_no_max_size() {
        Buffer buffer = Buffer.buffer("abc");

        boolean truncated = BufferUtils.appendBuffer(buffer, Buffer.buffer("defgh"), -1);

        assertThat(truncated).isFalse();
        assertThat(buffer).hasToString("abcdefgh");
    }

    @Test
    void should_append_whole_chunk_when_it_reaches_max_size_exactly() {
        Buffer buffer = Buffer.buffer("abc");

        boolean truncated = BufferUtils.appendBuffer(buffer, Buffer.buffer("de"), 5);

        assertThat(truncated).isFalse();
        assertThat(buffer).hasToString("abcde");
    }

    @Test
    void should_cut_chunk_when_it_exceeds_max_size() {
        Buffer buffer = Buffer.buffer("abc");

        boolean truncated = BufferUtils.appendBuffer(buffer, Buffer.buffer("defgh"), 5);

        assertThat(truncated).isTrue();
        assertThat(buffer).hasToString("abcde");
    }

    @Test
    void should_drop_chunk_when_buffer_is_already_full() {
        Buffer buffer = Buffer.buffer("abcde");

        boolean truncated = BufferUtils.appendBuffer(buffer, Buffer.buffer("f"), 5);

        assertThat(truncated).isTrue();
        assertThat(buffer).hasToString("abcde");
    }

    @Test
    void should_not_report_truncation_for_empty_chunk_when_buffer_is_full() {
        Buffer buffer = Buffer.buffer("abcde");

        boolean truncated = BufferUtils.appendBuffer(buffer, Buffer.buffer(), 5);

        assertThat(truncated).isFalse();
        assertThat(buffer).hasToString("abcde");
    }
}
