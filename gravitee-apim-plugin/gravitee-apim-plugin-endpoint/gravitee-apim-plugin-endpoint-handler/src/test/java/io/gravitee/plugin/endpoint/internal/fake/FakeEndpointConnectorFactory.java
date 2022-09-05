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
package io.gravitee.plugin.endpoint.internal.fake;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.ConnectorFactory;
import io.gravitee.gateway.jupiter.api.connector.endpoint.EndpointConnectorFactory;
import java.util.Set;

/**
 * @author GraviteeSource Team
 */
public class FakeEndpointConnectorFactory implements EndpointConnectorFactory<FakeEndpointConnector> {

    @Override
    public ApiType supportedApi() {
        return FakeEndpointConnector.SUPPORTED_API;
    }

    @Override
    public Set<ConnectorMode> supportedModes() {
        return FakeEndpointConnector.SUPPORTED_MODES;
    }

    @Override
    public FakeEndpointConnector createConnector(final String configuration) {
        FakeEndpointConnector.FakeEndpointConnectorBuilder builder = FakeEndpointConnector.builder();
        if (configuration != null) {
            try {
                builder.configuration(new ObjectMapper().readValue(configuration, FakeEndpointConnectorConfiguration.class));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Bad configuration");
            }
        }

        return builder.build();
    }
}
