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

import io.gravitee.definition.model.v4.http.HttpProxyOptions;
import io.gravitee.definition.model.v4.http.HttpProxyType;
import io.gravitee.node.vertx.client.http.VertxHttpProxyOptions;
import io.gravitee.node.vertx.client.http.VertxHttpProxyType;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpProxyOptionsMapperTest {

    @Test
    void should_build_http_client_options_with_defined_values() {
        final HttpProxyOptions httpProxyOptions = HttpProxyOptions.builder()
            .enabled(true)
            .useSystemProxy(false)
            .host("host")
            .port(5555)
            .username("username")
            .password("password")
            .type(HttpProxyType.SOCKS4)
            .build();

        final VertxHttpProxyOptions result = HttpProxyOptionsMapper.INSTANCE.map(httpProxyOptions);
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.isUseSystemProxy()).isFalse();
        assertThat(result.getHost()).isEqualTo("host");
        assertThat(result.getPort()).isEqualTo(5555);
        assertThat(result.getUsername()).isEqualTo("username");
        assertThat(result.getPassword()).isEqualTo("password");
        assertThat(result.getType()).isEqualTo(VertxHttpProxyType.SOCKS4);
    }
}
