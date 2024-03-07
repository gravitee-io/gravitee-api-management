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
package io.gravitee.apim.common.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.tcp.TcpClientOptions;
import io.gravitee.node.vertx.client.tcp.VertxTcpClientOptions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TcpClientOptionsMapperTest {

    @Test
    void should_build_http_client_options_with_default_values() {
        final TcpClientOptions tcpClientOptions = TcpClientOptions.builder().build();

        final var result = TcpClientOptionsMapper.INSTANCE.map(tcpClientOptions);
        assertThat(result.getConnectTimeout()).isEqualTo(VertxTcpClientOptions.DEFAULT_CONNECT_TIMEOUT);
        assertThat(result.getReconnectAttempts()).isEqualTo(VertxTcpClientOptions.DEFAULT_RECONNECT_ATTEMPTS);
        assertThat(result.getReconnectInterval()).isEqualTo(VertxTcpClientOptions.DEFAULT_RECONNECT_INTERVAL);
        assertThat(result.getIdleTimeout()).isEqualTo(VertxTcpClientOptions.DEFAULT_IDLE_TIMEOUT);
        assertThat(result.getReadIdleTimeout()).isEqualTo(VertxTcpClientOptions.DEFAULT_READ_IDLE_TIMEOUT);
        assertThat(result.getWriteIdleTimeout()).isEqualTo(VertxTcpClientOptions.DEFAULT_WRITE_IDLE_TIMEOUT);
    }
}
