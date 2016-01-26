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
package io.gravitee.gateway.core.http.loadbalancer;

import io.gravitee.definition.model.Api;
import io.gravitee.gateway.api.Request;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RoundRobinLoadBalancer extends LoadBalancerSupport {

    private int counter = -1;

    public RoundRobinLoadBalancer(final Api api) {
        super(api);
    }

    @Override
    public String chooseEndpoint(Request request) {
        int size = endpoints().size();
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
