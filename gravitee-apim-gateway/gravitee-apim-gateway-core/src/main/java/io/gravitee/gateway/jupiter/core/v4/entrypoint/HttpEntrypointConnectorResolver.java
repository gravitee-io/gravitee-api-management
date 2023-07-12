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
package io.gravitee.gateway.jupiter.core.v4.entrypoint;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.gateway.jupiter.api.connector.AbstractConnectorFactory;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.GenericExecutionContext;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@SuppressWarnings("unchecked")
public class HttpEntrypointConnectorResolver {

    private final List<EntrypointConnector> entrypointConnectors;

    public HttpEntrypointConnectorResolver(final Api api, final EntrypointConnectorPluginManager entrypointConnectorPluginManager) {
        entrypointConnectors =
            api
                .getListeners()
                .stream()
                .filter(listener -> listener.getType() == ListenerType.HTTP)
                .map(HttpListener.class::cast)
                .flatMap(httpListener -> httpListener.getEntrypoints().stream())
                .map(entrypoint -> this.<EntrypointConnector>createConnector(entrypointConnectorPluginManager, entrypoint))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(EntrypointConnector::matchCriteriaCount))
                .collect(Collectors.toList());
    }

    public <T extends EntrypointConnector> T resolve(final ExecutionContext ctx) {
        Optional<EntrypointConnector> entrypointConnector = entrypointConnectors
            .stream()
            .filter(connector -> connector.supportedListenerType() == io.gravitee.gateway.jupiter.api.ListenerType.HTTP)
            .filter(connector -> connector.matches(ctx))
            .findFirst();
        return (T) entrypointConnector.orElse(null);
    }

    private <T extends EntrypointConnector> T createConnector(
        EntrypointConnectorPluginManager entrypointConnectorPluginManager,
        Entrypoint entrypoint
    ) {
        AbstractConnectorFactory<? extends EntrypointConnector> connectorFactory = entrypointConnectorPluginManager.getFactoryById(
            entrypoint.getType()
        );

        if (connectorFactory != null) {
            return (T) connectorFactory.createConnector(entrypoint.getConfiguration());
        }
        return null;
    }
}
