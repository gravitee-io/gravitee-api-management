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
package io.gravitee.apim.infra.query_service.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.plugin.model.ConnectorPlugin;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.ConnectorFeature;
import io.gravitee.definition.model.v4.ConnectorMode;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.entrypoint.Qos;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EndpointPluginQueryServiceLegacyWrapperTest {

    EndpointConnectorPluginService endpointConnectorPluginService;
    EndpointPluginQueryServiceLegacyWrapper service;

    @BeforeEach
    void setUp() {
        endpointConnectorPluginService = mock(EndpointConnectorPluginService.class);
        service = new EndpointPluginQueryServiceLegacyWrapper(endpointConnectorPluginService);
    }

    @Nested
    class FindAll {

        @Test
        void should_return_plugins_list_with_deployed_status_depending_on_license() {
            when(endpointConnectorPluginService.findAll()).thenAnswer(invocation -> {
                var plugin1 = new ConnectorPluginEntity();
                plugin1.setId("id1");
                plugin1.setName("Plugin 1");
                plugin1.setVersion("1.0.0");
                plugin1.setDescription("description1");
                plugin1.setFeature("apim-feature-plugin1");
                plugin1.setDeployed(true);
                plugin1.setSupportedApiType(ApiType.MESSAGE);
                plugin1.setSupportedListenerType(ListenerType.HTTP);
                plugin1.setSupportedModes(Set.of(ConnectorMode.REQUEST_RESPONSE));
                plugin1.setSupportedQos(Set.of(Qos.AUTO));
                plugin1.setAvailableFeatures(Set.of(ConnectorFeature.LIMIT));

                var plugin2 = new ConnectorPluginEntity();
                plugin2.setId("id2");
                plugin2.setName("Plugin 2");
                plugin2.setVersion("2.0.0");
                plugin2.setDescription("description2");
                plugin2.setFeature("apim-feature-plugin2");
                plugin2.setDeployed(true);
                plugin2.setSupportedApiType(ApiType.MESSAGE);
                plugin2.setSupportedListenerType(ListenerType.HTTP);
                plugin2.setSupportedModes(Set.of(ConnectorMode.REQUEST_RESPONSE));
                plugin2.setSupportedQos(Set.of(Qos.AUTO));
                plugin2.setAvailableFeatures(Set.of(ConnectorFeature.LIMIT));
                return Set.of(plugin1, plugin2);
            });

            var result = service.findAll();

            assertThat(result)
                .hasSize(2)
                .containsExactlyInAnyOrder(
                    ConnectorPlugin.builder()
                        .id("id1")
                        .name("Plugin 1")
                        .version("1.0.0")
                        .description("description1")
                        .feature("apim-feature-plugin1")
                        .deployed(true)
                        .supportedApiType(ApiType.MESSAGE)
                        .supportedListenerType(ListenerType.HTTP)
                        .supportedModes(Set.of(ConnectorMode.REQUEST_RESPONSE))
                        .supportedQos(Set.of(Qos.AUTO))
                        .availableFeatures(Set.of(ConnectorFeature.LIMIT))
                        .build(),
                    ConnectorPlugin.builder()
                        .id("id2")
                        .name("Plugin 2")
                        .version("2.0.0")
                        .description("description2")
                        .feature("apim-feature-plugin2")
                        .deployed(true)
                        .supportedApiType(ApiType.MESSAGE)
                        .supportedListenerType(ListenerType.HTTP)
                        .supportedModes(Set.of(ConnectorMode.REQUEST_RESPONSE))
                        .supportedQos(Set.of(Qos.AUTO))
                        .availableFeatures(Set.of(ConnectorFeature.LIMIT))
                        .build()
                );
        }
    }
}
