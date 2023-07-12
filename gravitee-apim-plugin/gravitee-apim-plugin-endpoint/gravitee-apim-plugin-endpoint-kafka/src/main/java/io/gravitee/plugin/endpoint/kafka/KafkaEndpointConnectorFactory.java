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
package io.gravitee.plugin.endpoint.kafka;

import static io.gravitee.plugin.endpoint.kafka.KafkaEndpointConnector.SUPPORTED_MODES;

import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.endpoint.async.EndpointAsyncConnectorFactory;
import io.gravitee.gateway.jupiter.api.exception.PluginConfigurationException;
import io.gravitee.plugin.endpoint.kafka.configuration.KafkaEndpointConnectorConfiguration;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class KafkaEndpointConnectorFactory extends EndpointAsyncConnectorFactory {

    public KafkaEndpointConnectorFactory() {
        super(KafkaEndpointConnectorConfiguration.class);
    }

    @Override
    public Set<ConnectorMode> supportedModes() {
        return SUPPORTED_MODES;
    }

    @Override
    public KafkaEndpointConnector createConnector(final String configuration) {
        try {
            return new KafkaEndpointConnector(getConfiguration(configuration));
        } catch (PluginConfigurationException e) {
            log.error("Can't create connector cause no valid configuration", e);
            return null;
        }
    }
}
