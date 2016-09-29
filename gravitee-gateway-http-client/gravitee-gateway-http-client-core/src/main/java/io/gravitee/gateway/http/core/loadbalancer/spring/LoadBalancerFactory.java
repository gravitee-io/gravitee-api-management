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
package io.gravitee.gateway.http.core.loadbalancer.spring;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.api.http.loadbalancer.LoadBalancerStrategy;
import io.gravitee.gateway.http.core.loadbalancer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LoadBalancerFactory extends AbstractFactoryBean<LoadBalancerStrategy> {

    private final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerFactory.class);

    @Autowired
    private Api api;

    @Override
    public Class<?> getObjectType() {
        return LoadBalancerStrategy.class;
    }

    @Override
    protected LoadBalancerStrategy createInstance() throws Exception {
        LoadBalancerStrategy loadBalancer = null;

        List<Endpoint> endpoints = api.getProxy().getEndpoints()
                .stream()
                .filter(endpoint -> !endpoint.isBackup())
                .collect(Collectors.toList());

        // Use a LB strategy only if more than one endpoint
        if (endpoints.size() > 1) {
            io.gravitee.definition.model.LoadBalancer lb = api.getProxy().getLoadBalancer();

            if (lb != null) {
                switch (lb.getType()) {
                    case ROUND_ROBIN:
                        loadBalancer = new RoundRobinLoadBalancerStrategy(endpoints);
                        break;
                    case RANDOM:
                        loadBalancer = new RandomLoadBalancerStrategy(endpoints);
                        break;
                    case WEIGHTED_RANDOM:
                        loadBalancer = new WeightedRandomLoadBalancerStrategy(endpoints);
                        break;
                    case WEIGHTED_ROUND_ROBIN:
                        loadBalancer = new WeightedRoundRobinLoadBalancerStrategy(endpoints);
                        break;
                }
            }
        } else {
            loadBalancer = new SingleEndpointLoadBalancerStrategy(endpoints.get(0));
        }

        // Set default LB to round-robin
        loadBalancer = (loadBalancer != null) ? loadBalancer : new RoundRobinLoadBalancerStrategy(endpoints);

        LOGGER.info("Create a load-balancer instance of type {}", loadBalancer);

        return loadBalancer;
    }
}
