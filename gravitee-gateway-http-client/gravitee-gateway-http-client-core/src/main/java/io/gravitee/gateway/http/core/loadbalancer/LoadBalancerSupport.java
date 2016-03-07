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
package io.gravitee.gateway.http.core.loadbalancer;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.api.http.loadbalancer.LoadBalancer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public abstract class LoadBalancerSupport implements LoadBalancer {

    protected final Api api;
    private List<Endpoint> endpoints;

    protected LoadBalancerSupport(final Api api) {
        this.api = api;
    }

    /**
     * Returns a list of non-backup endpoints
     *
     * @return List of non-backup endpoints.
     */
    protected List<Endpoint> availableEndpoints() {
        if (endpoints == null) {
            endpoints = api.getProxy().getEndpoints()
                    .stream()
                    .filter(endpoint -> !endpoint.isBackup())
                    .collect(Collectors.toList());
        }

        return endpoints;
    }
}
