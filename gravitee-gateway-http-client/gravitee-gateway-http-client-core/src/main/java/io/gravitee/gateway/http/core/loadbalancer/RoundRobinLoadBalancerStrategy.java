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

import io.gravitee.definition.model.Endpoint;

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RoundRobinLoadBalancerStrategy extends LoadBalancerSupportStrategy {

    private int counter = -1;

    public RoundRobinLoadBalancerStrategy(final List<Endpoint> endpoints) {
        super(endpoints);
    }

    @Override
    public synchronized Endpoint nextEndpoint() {
        int size = endpoints().size();
        if (size == 0) {
            return null;
        }

        if (++counter >= size) {
            counter = 0;
        }
        return endpoints().get(counter);
    }

    @Override
    public String toString() {
        return "RoundRobinLoadBalancer";
    }
}
