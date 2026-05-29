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
package io.gravitee.gateway.reactive.standalone.vertx;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.reactive.standalone.vertx.HttpProtocolVerticle.ClientCloseReason;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link HttpProtocolVerticle#classifyClientClose(Throwable)} — pins the (key, message)
 * mapping for the client-close reasons that reach the connection exception handler (APIM-12769). The idle
 * path is intentionally not covered here: it is signalled out-of-band by a channel attribute, not by a
 * {@link Throwable}, and is validated end-to-end in {@code ConnectionErrorClassificationV4IntegrationTest}.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpProtocolVerticleClassifyTest {

    @Test
    void should_classify_connection_reset_as_tcp_reset() {
        ClientCloseReason reason = HttpProtocolVerticle.classifyClientClose(new IOException("Connection reset by peer"));
        assertThat(reason.key()).isEqualTo(HttpProtocolVerticle.CLIENT_ABORTED_TCP_RESET);
        assertThat(reason.message()).isEqualTo(HttpProtocolVerticle.CLIENT_ABORTED_TCP_RESET_MESSAGE);
    }

    @Test
    void should_classify_wrapped_connection_reset_via_cause_chain() {
        ClientCloseReason reason = HttpProtocolVerticle.classifyClientClose(
            new RuntimeException("wrapper", new IOException("Connection reset by peer"))
        );
        assertThat(reason.key()).isEqualTo(HttpProtocolVerticle.CLIENT_ABORTED_TCP_RESET);
    }

    @Test
    void should_classify_broken_pipe() {
        ClientCloseReason reason = HttpProtocolVerticle.classifyClientClose(new IOException("Broken pipe"));
        assertThat(reason.key()).isEqualTo(HttpProtocolVerticle.CLIENT_ABORTED_BROKEN_PIPE);
        assertThat(reason.message()).isEqualTo(HttpProtocolVerticle.CLIENT_ABORTED_BROKEN_PIPE_MESSAGE);
    }

    @Test
    void should_classify_closed_channel_exception_as_channel_closed() {
        ClientCloseReason reason = HttpProtocolVerticle.classifyClientClose(new ClosedChannelException());
        assertThat(reason.key()).isEqualTo(HttpProtocolVerticle.CLIENT_ABORTED_CHANNEL_CLOSED);
        assertThat(reason.message()).isEqualTo(HttpProtocolVerticle.CLIENT_ABORTED_CHANNEL_CLOSED_MESSAGE);
    }

    @Test
    void should_default_unrecognised_cause_to_channel_closed() {
        ClientCloseReason reason = HttpProtocolVerticle.classifyClientClose(new RuntimeException("something else"));
        assertThat(reason.key()).isEqualTo(HttpProtocolVerticle.CLIENT_ABORTED_CHANNEL_CLOSED);
        assertThat(reason.message()).isEqualTo(HttpProtocolVerticle.CLIENT_ABORTED_CHANNEL_CLOSED_MESSAGE);
    }
}
