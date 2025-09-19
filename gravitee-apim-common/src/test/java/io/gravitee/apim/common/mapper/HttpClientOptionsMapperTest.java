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

import io.gravitee.definition.model.v4.http.HttpClientOptions;
import io.gravitee.definition.model.v4.http.ProtocolVersion;
import io.gravitee.node.vertx.client.http.VertxHttpClientOptions;
import io.gravitee.node.vertx.client.http.VertxHttpProtocolVersion;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpClientOptionsMapperTest {

    @Test
    void should_build_http_client_options_with_default_values() {
        final HttpClientOptions httpClientOptions = HttpClientOptions.builder().build();

        final VertxHttpClientOptions result = HttpClientOptionsMapper.INSTANCE.map(httpClientOptions);
        assertThat(result.getConnectTimeout()).isEqualTo(VertxHttpClientOptions.DEFAULT_CONNECT_TIMEOUT);
        assertThat(result.getIdleTimeout()).isEqualTo(VertxHttpClientOptions.DEFAULT_IDLE_TIMEOUT);
        assertThat(result.isKeepAlive()).isEqualTo(VertxHttpClientOptions.DEFAULT_KEEP_ALIVE);
        assertThat(result.getReadTimeout()).isEqualTo(VertxHttpClientOptions.DEFAULT_READ_TIMEOUT);
        assertThat(result.isPipelining()).isEqualTo(VertxHttpClientOptions.DEFAULT_PIPELINING);
        assertThat(result.getMaxConcurrentConnections()).isEqualTo(VertxHttpClientOptions.DEFAULT_MAX_CONCURRENT_CONNECTIONS);
        assertThat(result.isUseCompression()).isEqualTo(VertxHttpClientOptions.DEFAULT_USE_COMPRESSION);
        assertThat(result.isClearTextUpgrade()).isEqualTo(VertxHttpClientOptions.DEFAULT_CLEAR_TEXT_UPGRADE);
        assertThat(result.getVersion()).isEqualTo(VertxHttpClientOptions.DEFAULT_PROTOCOL_VERSION);
    }

    @Test
    void should_build_http_client_options_with_defined_values() {
        final HttpClientOptions httpClientOptions = HttpClientOptions.builder()
            .connectTimeout(1000)
            .idleTimeout(2000)
            .keepAlive(false)
            .readTimeout(500)
            .pipelining(false)
            .maxConcurrentConnections(1)
            .useCompression(false)
            .clearTextUpgrade(false)
            .version(ProtocolVersion.HTTP_2)
            .build();

        final VertxHttpClientOptions result = HttpClientOptionsMapper.INSTANCE.map(httpClientOptions);
        assertThat(result.getConnectTimeout()).isEqualTo(1000);
        assertThat(result.getIdleTimeout()).isEqualTo(2000);
        assertThat(result.isKeepAlive()).isFalse();
        assertThat(result.getReadTimeout()).isEqualTo(500);
        assertThat(result.isPipelining()).isFalse();
        assertThat(result.getMaxConcurrentConnections()).isOne();
        assertThat(result.isUseCompression()).isFalse();
        assertThat(result.isClearTextUpgrade()).isFalse();
        assertThat(result.getVersion()).isEqualTo(VertxHttpProtocolVersion.HTTP_2);
    }
}
