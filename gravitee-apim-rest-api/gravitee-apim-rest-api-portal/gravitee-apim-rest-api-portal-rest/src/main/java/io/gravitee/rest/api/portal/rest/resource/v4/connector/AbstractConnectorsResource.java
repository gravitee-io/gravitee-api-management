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
package io.gravitee.rest.api.portal.rest.resource.v4.connector;

import io.gravitee.rest.api.model.v4.connector.ConnectorExpandPluginEntity;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Common code to handle connectors resources.
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractConnectorsResource {

    protected Collection<ConnectorExpandPluginEntity> expand(Set<ConnectorPluginEntity> connectorPluginEntities, List<String> expands) {
        Stream<ConnectorExpandPluginEntity> stream = connectorPluginEntities.stream().map(this::convert);

        if (expands != null && !expands.isEmpty()) {
            for (String expand : expands) {
                if ("schema".equals(expand)) {
                    stream = stream.peek(connectorListItem -> connectorListItem.setSchema(getSchema(connectorListItem.getId())));
                } else if ("icon".equals(expand)) {
                    stream = stream.peek(connectorListItem -> connectorListItem.setIcon(getIcon(connectorListItem.getId())));
                }
            }
        }

        return stream.sorted(Comparator.comparing(ConnectorExpandPluginEntity::getName)).collect(Collectors.toList());
    }

    private ConnectorExpandPluginEntity convert(ConnectorPluginEntity endpointPluginEntity) {
        ConnectorExpandPluginEntity connectorExpandPluginEntity = new ConnectorExpandPluginEntity();

        connectorExpandPluginEntity.setId(endpointPluginEntity.getId());
        connectorExpandPluginEntity.setName(endpointPluginEntity.getName());
        connectorExpandPluginEntity.setDescription(endpointPluginEntity.getDescription());
        connectorExpandPluginEntity.setVersion(endpointPluginEntity.getVersion());
        connectorExpandPluginEntity.setSupportedApiType(endpointPluginEntity.getSupportedApiType());
        connectorExpandPluginEntity.setSupportedModes(endpointPluginEntity.getSupportedModes());
        connectorExpandPluginEntity.setAvailableFeatures(endpointPluginEntity.getAvailableFeatures());
        connectorExpandPluginEntity.setSupportedListenerType(endpointPluginEntity.getSupportedListenerType());

        return connectorExpandPluginEntity;
    }

    /**
     * Return the schema according to the connector id
     * @param connectorId
     * @return Schema found as a string
     */
    protected abstract String getSchema(final String connectorId);

    /**
     * Return the icon according to the connector id
     * @param connectorId
     * @return Icon found as a string
     */
    protected abstract String getIcon(final String connectorId);
}
