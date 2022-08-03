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
package io.gravitee.plugin.entrypoint.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class SseEntrypointConnectorFactoryTest {

    private SseEntrypointConnectorFactory sseEntrypointConnectorFactory;

    @BeforeEach
    void beforeEach() {
        sseEntrypointConnectorFactory = new SseEntrypointConnectorFactory();
    }

    @Test
    void shouldSupportAsyncApi() {
        assertThat(sseEntrypointConnectorFactory.supportedApi()).isEqualTo(ApiType.ASYNC);
    }

    @Test
    void shouldSupportSubscribeMode() {
        assertThat(sseEntrypointConnectorFactory.supportedModes()).contains(ConnectorMode.SUBSCRIBE);
    }

    @ParameterizedTest
    @ValueSource(strings = { "wrong", "", "  " })
    void shouldCreateConnectorWithWrongConfiguration(String configuration) {
        SseEntrypointConnector connector = sseEntrypointConnectorFactory.createConnector(configuration);
        assertThat(connector).isNotNull();
    }

    @Test
    void shouldCreateConnectorWithNullConfiguration() {
        SseEntrypointConnector connector = sseEntrypointConnectorFactory.createConnector(null);
        assertThat(connector).isNotNull();
    }
}
