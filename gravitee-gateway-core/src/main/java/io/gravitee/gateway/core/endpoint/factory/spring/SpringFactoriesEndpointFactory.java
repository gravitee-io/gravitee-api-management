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
package io.gravitee.gateway.core.endpoint.factory.spring;

import io.gravitee.common.spring.factory.SpringFactoriesLoader;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.core.endpoint.factory.EndpointFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class SpringFactoriesEndpointFactory extends SpringFactoriesLoader<EndpointFactory> implements EndpointFactory {

    @Override
    public boolean support(io.gravitee.definition.model.Endpoint endpoint) {
        return true;
    }

    @Override
    public Endpoint create(io.gravitee.definition.model.Endpoint model) {
        for (EndpointFactory factory : getFactoriesInstances()) {
            if (factory.support(model)) {
                return factory.create(model);
            }
        }

        throw new IllegalStateException(
                String.format("Unable to create an endpoint for %s", model));
    }

    @Override
    protected Class<EndpointFactory> getObjectType() {
        return EndpointFactory.class;
    }
}
