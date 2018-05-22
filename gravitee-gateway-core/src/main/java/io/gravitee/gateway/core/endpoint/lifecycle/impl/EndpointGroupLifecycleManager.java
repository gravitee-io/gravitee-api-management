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
package io.gravitee.gateway.core.endpoint.lifecycle.impl;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.common.util.ChangeListener;
import io.gravitee.common.util.ObservableCollection;
import io.gravitee.common.util.ObservableSet;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.LoadBalancer;
import io.gravitee.gateway.api.lb.LoadBalancerStrategy;
import io.gravitee.gateway.core.endpoint.factory.EndpointFactory;
import io.gravitee.gateway.core.endpoint.lifecycle.EndpointLifecycleManager;
import io.gravitee.gateway.core.endpoint.lifecycle.LoadBalancedEndpointGroup;
import io.gravitee.gateway.core.endpoint.ref.EndpointReference;
import io.gravitee.gateway.core.endpoint.ref.ReferenceRegister;
import io.gravitee.gateway.core.loadbalancer.RandomLoadBalancer;
import io.gravitee.gateway.core.loadbalancer.RoundRobinLoadBalancer;
import io.gravitee.gateway.core.loadbalancer.WeightedRandomLoadBalancer;
import io.gravitee.gateway.core.loadbalancer.WeightedRoundRobinLoadBalancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointGroupLifecycleManager extends AbstractLifecycleComponent<EndpointLifecycleManager>
        implements EndpointLifecycleManager, ChangeListener<Endpoint> {

    private final Logger logger = LoggerFactory.getLogger(EndpointGroupLifecycleManager.class);

    @Autowired
    private EndpointFactory endpointFactory;

    @Autowired
    private ReferenceRegister referenceRegister;

    private final Map<String, io.gravitee.gateway.api.endpoint.Endpoint> endpointsByName = new LinkedHashMap<>();
    private final ObservableCollection<io.gravitee.gateway.api.endpoint.Endpoint> endpoints = new ObservableCollection<>(new ArrayList<>());

    private final EndpointGroup group;
    private LoadBalancedEndpointGroup lbGroup;

    @Autowired
    public EndpointGroupLifecycleManager(EndpointGroup group) {
        this.group = group;
    }

    @Override
    protected void doStart() throws Exception {
        // Wrap endpoints with an observable collection
        ObservableSet<Endpoint> endpoints = new ObservableSet<>(group.getEndpoints());
        endpoints.addListener(EndpointGroupLifecycleManager.this);
        group.setEndpoints(endpoints);

        LoadBalancer loadBalancerDef = group.getLoadBalancer();
        LoadBalancerStrategy strategy;

        if (loadBalancerDef != null) {
            switch (loadBalancerDef.getType()) {
                case RANDOM:
                    strategy = new RandomLoadBalancer(this.endpoints);
                    break;
                case WEIGHTED_RANDOM:
                    strategy = new WeightedRandomLoadBalancer(this.endpoints);
                    break;
                case WEIGHTED_ROUND_ROBIN:
                    strategy = new WeightedRoundRobinLoadBalancer(this.endpoints);
                    break;
                default:
                    strategy = new RoundRobinLoadBalancer(this.endpoints);
                    break;
            }
        } else {
            strategy = new RoundRobinLoadBalancer(this.endpoints);
        }

        lbGroup = new LoadBalancedEndpointGroup(group.getName(), strategy);

        endpoints
                .stream()
                .filter(filter())
                .forEach(this::start);
    }

    @Override
    protected void doStop() throws Exception {
        Iterator<io.gravitee.gateway.api.endpoint.Endpoint> ite = endpointsByName.values().iterator();
        while (ite.hasNext()) {
            stop(ite.next());
            ite.remove();
        }
    }

    protected Predicate<Endpoint> filter() {
        return endpoint -> !endpoint.isBackup();
    }

    public void start(io.gravitee.definition.model.Endpoint model) {
        try {
            logger.info("Create new endpoint: name[{}] type[{}] target[{}]",
                    model.getName(), model.getType(), model.getTarget());

            io.gravitee.gateway.api.endpoint.Endpoint endpoint = endpointFactory.create(model);
            if (endpoint != null) {
                endpoint.connector().start();

                endpoints.add(endpoint);
                endpointsByName.put(endpoint.name(), endpoint);

                referenceRegister.add(new EndpointReference(endpoint));
            }
        } catch (Exception ex) {
            logger.error("Unexpected error while creating endpoint connector", ex);
        }
    }

    public void stop(String endpointName) {
        logger.info("Closing endpoint: name[{}]", endpointName);

        io.gravitee.gateway.api.endpoint.Endpoint endpoint = endpointsByName.remove(endpointName);
        stop(endpoint);
    }

    private void stop(io.gravitee.gateway.api.endpoint.Endpoint endpoint) {
        if (endpoint != null) {
            try {
                endpoints.remove(endpoint);
                referenceRegister.remove(EndpointReference.REFERENCE_PREFIX + endpoint.name());
                endpoint.connector().stop();
            } catch (Exception ex) {
                logger.error("Unexpected error while closing endpoint connector", ex);
            }
        } else {
            logger.error("Unknown endpoint. You should never reach this point!");
        }
    }

    @Override
    public boolean preAdd(io.gravitee.definition.model.Endpoint endpoint) {
        return false;
    }

    @Override
    public boolean postAdd(io.gravitee.definition.model.Endpoint endpoint) {
        start(endpoint);
        return false;
    }

    @Override
    public boolean preRemove(io.gravitee.definition.model.Endpoint endpoint) {
        return false;
    }

    @Override
    public boolean postRemove(io.gravitee.definition.model.Endpoint endpoint) {
        stop(endpoint.getName());
        return false;
    }

    @Override
    public io.gravitee.gateway.api.endpoint.Endpoint get(String endpointName) {
        return endpointsByName.get(endpointName);
    }

    @Override
    public Collection<io.gravitee.gateway.api.endpoint.Endpoint> endpoints() {
        return endpoints;
    }

    public LoadBalancedEndpointGroup getGroup() {
        return lbGroup;
    }

    public void setEndpointFactory(EndpointFactory endpointFactory) {
        this.endpointFactory = endpointFactory;
    }

    public void setReferenceRegister(ReferenceRegister referenceRegister) {
        this.referenceRegister = referenceRegister;
    }
}
