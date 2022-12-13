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
package io.gravitee.gateway.jupiter.core.v4.endpoint.loadbalancer;

import io.gravitee.gateway.jupiter.core.v4.endpoint.ManagedEndpoint;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RoundRobinLoadBalancer extends AbstractLoadBalancerStrategy {

    private final AtomicInteger counter = new AtomicInteger(0);

    public RoundRobinLoadBalancer(final List<ManagedEndpoint> endpoints) {
        super(endpoints);
        refresh();
    }

    @Override
    public void refresh() {
        super.refresh();
        counter.set(0);
    }

    protected ManagedEndpoint getManagedEndpoint() {
        int size = endpoints.size();
        return endpoints.get(counter.getAndIncrement() % size);
    }
}
