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
package io.gravitee.gateway.jupiter.core.v4.entrypoint;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.ListenerHttp;
import io.gravitee.gateway.entrypoint.EntrypointRegistry;
import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.api.entrypoint.EntrypointConnector;
import io.gravitee.gateway.jupiter.api.entrypoint.EntrypointConnectorFactory;
import io.gravitee.gateway.jupiter.api.entrypoint.async.EntrypointAsyncConnector;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpEntrypointResolver {

    private final List<EntrypointAsyncConnector> entrypointConnectors;

    public HttpEntrypointResolver(final Api api, final EntrypointRegistry entrypointRegistry) {
        entrypointConnectors =
            api
                .getListeners()
                .stream()
                .filter(listener -> listener.getType() == ListenerType.HTTP)
                .map(ListenerHttp.class::cast)
                .flatMap(listenerHttp -> listenerHttp.getEntrypoints().stream())
                .map(
                    entrypoint -> {
                        EntrypointConnectorFactory<? extends EntrypointConnector<?>> connectorFactory = entrypointRegistry.getById(
                            entrypoint.getType()
                        );
                        if (connectorFactory.supportedApi() == ApiType.ASYNC) {
                            return (EntrypointAsyncConnector) connectorFactory.createConnector(entrypoint.getConfiguration());
                        }
                        return null;
                    }
                )
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(EntrypointAsyncConnector::matchCriteriaCount).reversed())
                .collect(Collectors.toList());
    }

    public EntrypointAsyncConnector resolve(final MessageExecutionContext messageExecutionContext) {
        Optional<EntrypointAsyncConnector> asyncConnector = entrypointConnectors
            .stream()
            .filter(entrypointAsyncConnector -> entrypointAsyncConnector.matches(messageExecutionContext))
            .findFirst();
        return asyncConnector.orElse(null);
    }
}
