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
package io.gravitee.gateway.http.core.endpoint.impl;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.definition.model.Api;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.api.http.client.HttpClient;
import io.gravitee.gateway.api.http.loadbalancer.LoadBalancerStrategy;
import io.gravitee.gateway.http.core.endpoint.EndpointLifecycleManager;
import io.gravitee.gateway.http.core.endpoint.HttpEndpoint;
import io.gravitee.gateway.http.core.loadbalancer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultEndpointLifecycleManager extends AbstractLifecycleComponent<EndpointLifecycleManager> implements EndpointLifecycleManager<HttpClient>, ApplicationContextAware {

    private final Logger logger = LoggerFactory.getLogger(DefaultEndpointLifecycleManager.class);

    @Autowired
    private Api api;

    private ApplicationContext applicationContext;

    private final Map<String, HttpEndpoint> endpoints = new LinkedHashMap<>();
    private final Map<String, String> endpointsTarget = new LinkedHashMap<>();

    private LoadBalancerStrategy loadBalancer;

    @Override
    protected void doStart() throws Exception {
        // Select and init endpoints
        if(api.getProxy().getEndpoints() != null) {
            api.getProxy().getEndpoints()
                    .stream()
                    .filter(filter())
                    .forEach(endpoint -> {
                        try {
                            logger.info("Create new target endpoint: {} [{}]", endpoint.getName(), endpoint.getTarget());
                            HttpClient httpClient = applicationContext.getBean(HttpClient.class, endpoint);
                            httpClient.start();
                            endpoints.put(endpoint.getName(), new HttpEndpoint(endpoint, httpClient));
                            endpointsTarget.put(endpoint.getName(), endpoint.getTarget());
                        } catch (Exception ex) {
                            logger.error("Unexpected error while creating endpoint connector", ex);
                        }
                    });
        }

        // Initialize load balancer
        List<io.gravitee.definition.model.Endpoint> filteredEndpoints = this.endpoints
                .values()
                .stream()
                .map(HttpEndpoint::definition)
                .collect(Collectors.toList());

        // Use a LB strategy only if more than one endpoint
        if (filteredEndpoints.size() > 1) {
            io.gravitee.definition.model.LoadBalancer lb = api.getProxy().getLoadBalancer();

            if (lb != null) {
                switch (lb.getType()) {
                    case ROUND_ROBIN:
                        loadBalancer = new RoundRobinLoadBalancerStrategy(filteredEndpoints);
                        break;
                    case RANDOM:
                        loadBalancer = new RandomLoadBalancerStrategy(filteredEndpoints);
                        break;
                    case WEIGHTED_RANDOM:
                        loadBalancer = new WeightedRandomLoadBalancerStrategy(filteredEndpoints);
                        break;
                    case WEIGHTED_ROUND_ROBIN:
                        loadBalancer = new WeightedRoundRobinLoadBalancerStrategy(filteredEndpoints);
                        break;
                }
            }
        } else if (! filteredEndpoints.isEmpty()){
            loadBalancer = new SingleEndpointLoadBalancerStrategy(filteredEndpoints.get(0));
        }

        // Set default LB to round-robin
        loadBalancer = (loadBalancer != null) ? loadBalancer : new RoundRobinLoadBalancerStrategy(filteredEndpoints);

        logger.info("Create a load-balancer instance of type {}", loadBalancer);
    }

    protected Predicate<io.gravitee.definition.model.Endpoint> filter() {
        return endpoint -> !endpoint.isBackup();
    }

    @Override
    protected void doStop() throws Exception {
        for(Iterator<Map.Entry<String, HttpEndpoint>> it = endpoints.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, HttpEndpoint> endpoint = it.next();
            try {
                logger.info("Close target endpoint: {}", endpoint.getKey());
                endpoint.getValue().connector().stop();
                it.remove();
                endpointsTarget.remove(endpoint.getKey());
            } catch (Exception ex) {
                logger.error("Unexpected error while closing endpoint connector", ex);
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Endpoint<HttpClient> get(String endpointName) {
        return endpoints.get(endpointName);
    }

    @Override
    public Endpoint<HttpClient> getOrDefault(String endpointName) {
        Endpoint<HttpClient> endpoint = get(endpointName);
        return (endpoint != null) ? endpoint : endpoints.values().iterator().next();
    }

    @Override
    public Set<String> endpoints() {
        return Collections.unmodifiableSet(endpoints.keySet());
    }

    @Override
    public Map<String, String> targetByEndpoint() {
        return Collections.unmodifiableMap(endpointsTarget);
    }

    @Override
    public LoadBalancerStrategy loadbalancer() {
        return loadBalancer;
    }
}
