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
package io.gravitee.rest.api.service.v4;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.ConnectorMode;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.PluginService;
import java.util.Set;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ConnectorPluginService extends PluginService<ConnectorPluginEntity> {
    Set<ConnectorPluginEntity> findBySupportedApi(final ApiType apiType);

    Set<ConnectorPluginEntity> findByConnectorMode(final ConnectorMode connectorMode);

    /**
     * Looks for the connector plugin from its id and then validate the configuration
     * @param connectorPluginId is the id of the connector
     * @param configuration is the configuration to validate
     * @return the validated configuration
     */
    String validateConnectorConfiguration(final String connectorPluginId, final String configuration);

    /**
     * Validate the configuration for the given connector plugin
     * @param connectorPluginEntity is the connector plugin for which configuration will be validated
     * @param configuration is the configuration to validate
     * @return the validated configuration
     */
    String validateConnectorConfiguration(final ConnectorPluginEntity connectorPluginEntity, final String configuration);
}
