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
package io.gravitee.gateway.tcp.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.tcp.reactive.vertx.RecordingWriteStream;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.rxjava3.core.streams.WriteStream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VertxReadStreamUtilTest {

    @Test
    void should_convert_gio_buffer_flowable_into_vertx_rx_read_stream() {
        RecordingWriteStream recorder = new RecordingWriteStream();

        var readStream = VertxReadStreamUtil.toVertxRxReadStream(
            Flowable.just(Buffer.buffer("foo"), Buffer.buffer("bar"), Buffer.buffer("baz"))
        );
        readStream.rxPipeTo(WriteStream.newInstance(recorder)).blockingAwait();

        assertThat(recorder.getRecordedBuffers()).containsExactly("foo", "bar", "baz");
    }
}
