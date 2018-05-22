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
package io.gravitee.gateway.http.endpoint;

import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.api.Connector;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpEndpoint implements io.gravitee.gateway.api.endpoint.Endpoint {

    private final io.gravitee.definition.model.endpoint.HttpEndpoint endpoint;
    private final Connector connector;

    public HttpEndpoint(final io.gravitee.definition.model.endpoint.HttpEndpoint endpoint, final Connector connector) {
        this.endpoint = endpoint;
        this.connector = connector;
    }

    @Override
    public String name() {
        return endpoint.getName();
    }

    @Override
    public String target() {
        return endpoint.getTarget();
    }

    @Override
    public Connector connector() {
        return connector;
    }

    @Override
    public boolean available() {
        return endpoint.getStatus() != Endpoint.Status.DOWN;
    }

    @Override
    public int weight() {
        return endpoint.getWeight();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HttpEndpoint that = (HttpEndpoint) o;

        return name().equals(that.name());
    }

    @Override
    public int hashCode() {
        return name().hashCode();
    }
}
