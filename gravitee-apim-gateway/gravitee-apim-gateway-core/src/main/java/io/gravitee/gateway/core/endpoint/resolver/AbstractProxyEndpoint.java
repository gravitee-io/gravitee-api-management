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
package io.gravitee.gateway.core.endpoint.resolver;

import io.gravitee.gateway.api.Connector;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.api.endpoint.EndpointAvailabilityListener;
import io.gravitee.gateway.api.endpoint.resolver.ProxyEndpoint;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
abstract class AbstractProxyEndpoint implements ProxyEndpoint {

    private final Endpoint endpoint;

    public AbstractProxyEndpoint(final Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public Connector connector() {
        return endpoint.connector();
    }

    @Override
    public String name() {
        return endpoint.name();
    }

    @Override
    public String target() {
        return endpoint.target();
    }

    @Override
    public int weight() {
        return endpoint.weight();
    }

    @Override
    public boolean available() {
        return endpoint.available();
    }

    @Override
    public boolean primary() {
        return endpoint.primary();
    }

    @Override
    public void addEndpointAvailabilityListener(EndpointAvailabilityListener listener) {
        endpoint.addEndpointAvailabilityListener(listener);
    }

    @Override
    public void removeEndpointAvailabilityListener(EndpointAvailabilityListener listener) {
        endpoint.removeEndpointAvailabilityListener(listener);
    }
}
