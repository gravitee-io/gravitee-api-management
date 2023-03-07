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
package io.gravitee.gateway.handlers.api.services.dlq;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.listener.entrypoint.Dlq;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.reactive.api.service.dlq.DlqService;
import io.gravitee.gateway.reactive.api.service.dlq.DlqServiceFactory;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointCriteria;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultDlqServiceFactory implements DlqServiceFactory {

    private final EndpointManager endpointManager;
    private final Map<String, String> dlqMapping;

    public DefaultDlqServiceFactory(final Api api, final EndpointManager endpointManager) {
        this.endpointManager = endpointManager;
        this.dlqMapping = new HashMap<>();

        api
            .getListeners()
            .stream()
            .flatMap(listener -> listener.getEntrypoints().stream())
            .forEach(
                entrypoint -> {
                    final Dlq dlq = entrypoint.getDlq();
                    if (dlq != null && dlq.getEndpoint() != null) {
                        dlqMapping.put(entrypoint.getType(), dlq.getEndpoint());
                    }
                }
            );
    }

    @Override
    public @Nullable DlqService create(final EntrypointConnector connector) {
        final String targetEndpoint = dlqMapping.get(connector.id());

        if (targetEndpoint != null) {
            final EndpointCriteria criteria = new EndpointCriteria(targetEndpoint, ApiType.MESSAGE, Set.of(ConnectorMode.PUBLISH));
            final ManagedEndpoint managedEndpoint = endpointManager.next(criteria);

            if (managedEndpoint != null) {
                return new DefaultDlqService(managedEndpoint.getConnector());
            }
        }
        return null;
    }
}
