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

import io.gravitee.common.util.ChangeListener;
import io.gravitee.common.util.ObservableCollection;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.api.endpoint.EndpointAvailabilityListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class LoadBalancer implements LoadBalancerStrategy, EndpointAvailabilityListener, ChangeListener<Endpoint> {

    /**
     * Primary endpoints
     */
    protected final List<Endpoint> endpoints = new ArrayList<>();

    /**
     * Secondary (ie. backup) endpoints
     * @param endpoints
     */
    private final List<Endpoint> secondaryEndpoints = new ArrayList<>();

    private final AtomicInteger secondaryCounter = new AtomicInteger(0);

    LoadBalancer(Collection<Endpoint> endpoints) {
        if (endpoints instanceof ObservableCollection) {
            ((ObservableCollection<Endpoint>) endpoints).addListener(this);
        }

        endpoints.forEach(this::postAdd);
    }

    @Override
    public void onAvailabilityChange(Endpoint endpoint, boolean available) {
        if (available && !endpoints.contains(endpoint)) {
            endpoints.add(endpoint);
        } else if (!available) {
            endpoints.remove(endpoint);
        }
    }

    @Override
    public Endpoint next() {
        Endpoint endpoint = nextEndpoint();
        return (endpoint != null) ? endpoint : nextSecondary();
    }

    private Endpoint nextSecondary() {
        int size = secondaryEndpoints.size();
        if (size == 0) {
            return null;
        }

        return secondaryEndpoints.get(Math.abs(secondaryCounter.getAndIncrement() % size));
    }

    @Override
    public boolean preAdd(Endpoint object) {
        return false;
    }

    @Override
    public boolean preRemove(Endpoint object) {
        return false;
    }

    @Override
    public boolean postAdd(Endpoint endpoint) {
        if (endpoint.primary()) {
            endpoint.addEndpointAvailabilityListener(LoadBalancer.this);
            endpoints.add(endpoint);
        } else {
            secondaryEndpoints.add(endpoint);
        }

        return false;
    }

    @Override
    public boolean postRemove(Endpoint endpoint) {
        if (endpoint.primary()) {
            endpoint.removeEndpointAvailabilityListener(LoadBalancer.this);
            endpoints.remove(endpoint);
        } else {
            secondaryEndpoints.remove(endpoint);
        }

        return false;
    }

    abstract Endpoint nextEndpoint();
}
