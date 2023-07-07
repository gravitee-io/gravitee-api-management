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
package io.gravitee.gateway.reactive.core.v4.endpoint.loadbalancer;

import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint;
import java.util.List;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractLoadBalancerStrategy implements LoadBalancerStrategy {

    protected final List<ManagedEndpoint> endpoints;

    AbstractLoadBalancerStrategy(final List<ManagedEndpoint> endpoints) {
        this.endpoints = endpoints;
    }

    @Override
    public ManagedEndpoint next() {
        return next(0);
    }

    private ManagedEndpoint next(int attempt) {
        try {
            if (endpoints == null || endpoints.isEmpty()) {
                return null;
            }
            return getManagedEndpoint();
        } catch (IndexOutOfBoundsException exception) {
            // This has been implemented to avoid putting the method in synchronized and avoid TooManyException
            if (attempt < 2) {
                attempt++;
                return next(attempt);
            }
            return null;
        }
    }

    protected abstract ManagedEndpoint getManagedEndpoint();

    @Override
    public void refresh() {
        // By default, nothing to do
    }
}
