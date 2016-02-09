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
import io.gravitee.gateway.api.http.loadbalancer.LoadBalancer;
import io.gravitee.gateway.http.core.loadbalancer.RandomLoadBalancer;
import io.gravitee.gateway.http.core.loadbalancer.RoundRobinLoadBalancer;
import io.gravitee.gateway.http.core.loadbalancer.WeightedRandomLoadBalancer;
import io.gravitee.gateway.http.core.loadbalancer.WeightedRoundRobinLoadBalancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class LoadBalancerFactory extends AbstractFactoryBean<LoadBalancer> {

    private final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerFactory.class);

    @Autowired
    private Api api;

    @Override
    public Class<?> getObjectType() {
        return LoadBalancer.class;
    }

    @Override
    protected LoadBalancer createInstance() throws Exception {
        LoadBalancer loadBalancer = null;
        io.gravitee.definition.model.LoadBalancer lb = api.getProxy().getLoadBalancer();

        if (lb != null) {
            switch(lb.getType()) {
                case ROUND_ROBIN:
                    loadBalancer = new RoundRobinLoadBalancer(api);
                    break;
                case RANDOM:
                    loadBalancer = new RandomLoadBalancer(api);
                    break;
                case WEIGHTED_RANDOM:
                    loadBalancer = new WeightedRandomLoadBalancer(api);
                    break;
                case WEIGHTED_ROUND_ROBIN:
                    loadBalancer = new WeightedRoundRobinLoadBalancer(api);
                    break;
            }
        }

        // Set default LB to round-robin even if there is a single endpoint.
        loadBalancer = (loadBalancer != null) ? loadBalancer : new RoundRobinLoadBalancer(api);

        LOGGER.info("Create a load-balancer instance of type {} for API {}", loadBalancer, api.getName());

        return loadBalancer;
    }
}
