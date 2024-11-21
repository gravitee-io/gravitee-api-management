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
package io.gravitee.gateway.reactive.core.v4.invoker;

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_ENDPOINT_CONNECTOR_ID;

import io.gravitee.gateway.reactive.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.reactive.api.connector.endpoint.HttpEndpointConnector;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.tcp.TcpExecutionContext;
import io.gravitee.gateway.reactive.api.invoker.TcpInvoker;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint;
import io.reactivex.rxjava3.core.Completable;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TcpEndpointInvoker implements TcpInvoker {

    public static final String NO_ENDPOINT_FOUND_KEY = "NO_ENDPOINT_FOUND";

    private final EndpointManager endpointManager;

    public TcpEndpointInvoker(final EndpointManager endpointManager) {
        this.endpointManager = endpointManager;
    }

    @Override
    public String getId() {
        return "tcp-endpoint-invoker";
    }

    @Override
    public Completable invoke(final TcpExecutionContext ctx) {
        final HttpEndpointConnector endpointConnector = resolveConnector(ctx);

        if (endpointConnector == null) {
            return Completable.error(new IllegalStateException(NO_ENDPOINT_FOUND_KEY));
        }

        return connect(((EndpointConnector) endpointConnector), ((ExecutionContext) ctx));
    }

    private <T extends EndpointConnector> T resolveConnector(final TcpExecutionContext ctx) {
        final ManagedEndpoint managedEndpoint = endpointManager.next();

        if (managedEndpoint != null) {
            EndpointConnector endpointConnector = managedEndpoint.getConnector();
            ctx.setInternalAttribute(ATTR_INTERNAL_ENDPOINT_CONNECTOR_ID, endpointConnector.id());
            return (T) endpointConnector;
        }

        return null;
    }

    // Do not remove this signature until all connectors are migrated to HttpEndpointConnectors#connect(HttpExecutionContext ctx)
    protected Completable connect(final EndpointConnector endpointConnector, final ExecutionContext ctx) {
        return endpointConnector.connect(ctx);
    }
}
