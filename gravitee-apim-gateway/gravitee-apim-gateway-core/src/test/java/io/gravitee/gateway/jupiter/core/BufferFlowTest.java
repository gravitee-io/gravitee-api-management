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
package io.gravitee.gateway.jupiter.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.api.buffer.Buffer;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class BufferFlowTest {

    BufferFlow cut;

    @Nested
    class ConstructorTest {

        @Test
        void should_init_buffer_flow_without_chunks() {
            cut = new BufferFlow(() -> true);
            assertThat(cut.hasChunks()).isFalse();
        }

        @Test
        void should_init_buffer_flow_with_chunks() {
            cut = new BufferFlow(Flowable.empty(), () -> true);
            assertThat(cut.hasChunks()).isTrue();
        }
    }

    @Nested
    class BodyTest {

        @Test
        void should_return_body_when_streaming_is_false() {
            Buffer buffer = Buffer.buffer();
            cut = new BufferFlow(Flowable.just(buffer), () -> false);
            cut.body().test().assertValue(buffer).assertComplete();
        }

        @Test
        void should_return_emtpy_body_when_streaming_is_true() {
            Buffer buffer = Buffer.buffer();
            cut = new BufferFlow(Flowable.just(buffer), () -> true);
            cut.body().test().assertNoValues().assertComplete();
            cut.chunks().test().assertValue(buffer).assertComplete();
        }

        @Test
        void should_not_set_chunks_when_streaming_is_true() {
            cut = new BufferFlow(() -> true);
            assertThat(cut.hasChunks()).isFalse();
            cut.body(Buffer.buffer());
            assertThat(cut.hasChunks()).isFalse();
        }

        @Test
        void should_set_chunks_when_streaming_is_false() {
            Buffer buffer = Buffer.buffer();
            cut = new BufferFlow(() -> false);
            assertThat(cut.hasChunks()).isFalse();
            cut.body(buffer);
            cut.body().test().assertValue(buffer).assertComplete();
            cut.chunks().test().assertValue(buffer).assertComplete();
        }

        @Test
        void should_not_apply_onBody_and_set_chunks_when_streaming_is_true() {
            cut = new BufferFlow(() -> true);
            assertThat(cut.hasChunks()).isFalse();
            cut.onBody(buffer -> Maybe.just(Buffer.buffer())).test().assertComplete();
            assertThat(cut.hasChunks()).isFalse();
        }

        @Test
        void should_apply_onBody_and_set_chunks_when_streaming_is_false() {
            Buffer chunks = Buffer.buffer("init");
            Buffer buffer = Buffer.buffer("mapped");
            cut = new BufferFlow(Flowable.just(chunks), () -> false);
            cut.onBody(body -> Maybe.just(buffer)).test().assertComplete();
            cut.body().test().assertValue(buffer).assertComplete();
            cut.chunks().test().assertValue(buffer).assertComplete();
        }

        @Test
        void should_return_single_empty_body_when_streaming_is_true() {
            Buffer chunks = Buffer.buffer("init");
            cut = new BufferFlow(Flowable.just(chunks), () -> true);
            cut.bodyOrEmpty().test().assertValue(buffer -> buffer.length() == 0);
        }

        @Test
        void should_return_single_body_when_streaming_is_false() {
            Buffer chunks = Buffer.buffer("init");
            cut = new BufferFlow(Flowable.just(chunks), () -> false);
            cut.bodyOrEmpty().test().assertValue(chunks);
        }
    }
}
