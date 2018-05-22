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

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class WeightedRoundRobinLoadBalancer extends WeightedLoadBalancer {

    private int counter;

    public WeightedRoundRobinLoadBalancer(Collection<Endpoint> endpoints) {
        super(endpoints);
    }

    @Override
    public synchronized Endpoint nextEndpoint() {
        List<Endpoint> endpoints = endpoints();
        if (endpoints.isEmpty()) {
            return null;
        }

        if (isRuntimeRatiosZeroed())  {
            resetRuntimeRatios();
            counter = 0;
        }

        boolean found = false;
        while (!found) {
            if (counter >= getRuntimeRatios().size()) {
                counter = 0;
            }

            if (getRuntimeRatios().get(counter).getRuntime() > 0) {
                getRuntimeRatios().get(counter).setRuntime((getRuntimeRatios().get(counter).getRuntime()) - 1);
                found = true;
            } else {
                counter++;
            }
        }

        lastIndex = counter;

        return endpoints.get(counter++);
    }

    @Override
    public String toString() {
        return "WeightedRoundRobinLoadBalancer";
    }
}
