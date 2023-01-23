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
package io.gravitee.plugin.entrypoint.internal.fake;

import static io.gravitee.plugin.entrypoint.internal.fake.FakeEntrypointConnector.SUPPORTED_API;
import static io.gravitee.plugin.entrypoint.internal.fake.FakeEntrypointConnector.SUPPORTED_MODES;
import static io.gravitee.plugin.entrypoint.internal.fake.FakeEntrypointConnector.SUPPORTED_QOS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnectorFactory;
import io.gravitee.gateway.jupiter.api.context.DeploymentContext;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import java.util.Set;
import lombok.NoArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
public class FakeEntrypointConnectorFactory implements EntrypointAsyncConnectorFactory<FakeEntrypointConnector> {

    @Override
    public ApiType supportedApi() {
        return SUPPORTED_API;
    }

    @Override
    public Set<ConnectorMode> supportedModes() {
        return SUPPORTED_MODES;
    }

    @Override
    public Set<Qos> supportedQos() {
        return SUPPORTED_QOS;
    }

    @Override
    public ListenerType supportedListenerType() {
        return FakeEntrypointConnector.SUPPORTED_LISTENER_TYPE;
    }

    @Override
    public FakeEntrypointConnector createConnector(final DeploymentContext deploymentContext, final Qos qos, final String configuration) {
        FakeEntrypointConnector.FakeEntrypointConnectorBuilder builder = FakeEntrypointConnector.builder();
        if (configuration != null) {
            try {
                builder.configuration(new ObjectMapper().readValue(configuration, FakeEntrypointConnectorConfiguration.class));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Bad configuration");
            }
        }

        return builder.build();
    }
}
