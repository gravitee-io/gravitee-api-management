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
package io.gravitee.gateway.jupiter.core.v4.endpoint;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.gateway.jupiter.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@SuppressWarnings("unchecked")
public class DefaultEndpointConnectorResolver {

    public static final String ATTR_INTERNAL_ENTRYPOINT_CONNECTOR = "entrypointConnector";

    private final Map<EndpointGroup, List<EndpointConnector>> connectorsByGroup;

    public DefaultEndpointConnectorResolver(final Api api, final EndpointConnectorPluginManager endpointConnectorPluginManager) {
        connectorsByGroup =
            api
                .getEndpointGroups()
                .stream()
                .flatMap(endpointGroup ->
                    endpointGroup
                        .getEndpoints()
                        .stream()
                        .map(endpoint -> {
                            String configuration = getEndpointConfiguration(endpointGroup, endpoint);
                            return Map.entry(
                                endpointGroup,
                                endpointConnectorPluginManager.getFactoryById(endpoint.getType()).createConnector(configuration)
                            );
                        })
                )
                .filter(e -> e.getValue() != null)
                .collect(Collectors.groupingBy(Map.Entry::getKey, LinkedHashMap::new, mapping(Map.Entry::getValue, toList())));
    }

    public <T extends EndpointConnector> T resolve(final ExecutionContext ctx) {
        EntrypointConnector entrypointConnector = ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR);

        return (T) connectorsByGroup
            .entrySet()
            .stream()
            .flatMap(e ->
                e
                    .getValue()
                    .stream()
                    .filter(connector -> Objects.equals(connector.supportedApi(), entrypointConnector.supportedApi()))
                    .filter(connector -> connector.supportedModes().containsAll(entrypointConnector.supportedModes()))
            )
            .findFirst()
            .orElse(null);
    }

    private String getEndpointConfiguration(EndpointGroup endpointGroup, Endpoint endpoint) {
        return endpoint.isInheritConfiguration() ? endpointGroup.getSharedConfiguration() : endpoint.getConfiguration();
    }
}
