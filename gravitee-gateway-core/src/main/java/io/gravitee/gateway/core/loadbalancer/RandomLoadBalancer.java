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
package io.gravitee.gateway.core.loadbalancer;

import io.gravitee.gateway.api.endpoint.Endpoint;

import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RandomLoadBalancer extends LoadBalancer {

    private transient int index;
    private static final Random RANDOM = new Random();

    public RandomLoadBalancer(Collection<Endpoint> endpoints) {
        super(endpoints);
    }

    @Override
    public synchronized Endpoint nextEndpoint() {
        List<Endpoint> endpoints = endpoints();
        int size = endpoints.size();
        if (size == 0) {
            return null;
        } else if (size == 1) {
            // There is only 1
            return endpoints.get(0);
        }

        index = RANDOM.nextInt(size);
        return endpoints().get(index);
    }

    @Override
    public String toString() {
        return "RandomLoadBalancer";
    }
}
