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
package io.gravitee.gateway.standalone;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.handler.codec.compression.ZlibCodecFactory;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WebSocketCompressionClasspathTest {

    /**
     * Netty accepts a permessage-deflate client_max_window_bits parameter from a backend only when it can honour a
     * custom window size, which it ties to JZlib being on the classpath. Without it, the gateway rejects the RFC 7692
     * handshake answer of many WebSocket servers.
     */
    @Test
    void should_support_custom_deflate_window_size_for_websocket_compression() {
        assertThat(ZlibCodecFactory.isSupportingWindowSizeAndMemLevel()).isTrue();
    }
}
