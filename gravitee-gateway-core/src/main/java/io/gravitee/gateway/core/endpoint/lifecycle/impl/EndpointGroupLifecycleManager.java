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
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.LoadBalancer;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.gateway.core.endpoint.EndpointException;
import io.gravitee.gateway.core.endpoint.factory.EndpointFactory;
import io.gravitee.gateway.core.endpoint.factory.template.EndpointContext;
import io.gravitee.gateway.core.endpoint.lifecycle.EndpointLifecycleManager;
import io.gravitee.gateway.core.endpoint.lifecycle.LoadBalancedEndpointGroup;
import io.gravitee.gateway.core.endpoint.ref.EndpointReference;
import io.gravitee.gateway.core.endpoint.ref.ReferenceRegister;
import io.gravitee.gateway.core.loadbalancer.*;
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
    private Api api;

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
        Set<Endpoint> groupEndpoints = group.getEndpoints();
        if (groupEndpoints == null) {
            groupEndpoints = new LinkedHashSet<>();
        }
        final ObservableSet<Endpoint> endpoints = new ObservableSet<>(groupEndpoints);
        endpoints.addListener(EndpointGroupLifecycleManager.this);
        group.setEndpoints(endpoints);

        endpoints
                .stream()
                .filter(filter())
                .peek(endpoint -> {
                    if (HttpEndpoint.class.isAssignableFrom(endpoint.getClass())) {
                        final HttpEndpoint httpEndpoint = ((HttpEndpoint) endpoint);
                        final boolean inherit = endpoint.getInherit() != null && endpoint.getInherit();
                        // inherit or discovered endpoints
                        if (inherit || httpEndpoint.getHttpClientOptions() == null) {
                            httpEndpoint.setHttpClientOptions(group.getHttpClientOptions());
                            httpEndpoint.setHttpClientSslOptions(group.getHttpClientSslOptions());
                            httpEndpoint.setHttpProxy(group.getHttpProxy());
                            httpEndpoint.setHeaders(group.getHeaders());
                        }
                    }
                })
                .forEach(this::start);

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
        return endpoint -> true;
    }

    public void start(io.gravitee.definition.model.Endpoint model) {
        try {
            logger.debug("Create new endpoint: name[{}] type[{}] target[{}] primary[{}]",
                    model.getName(), model.getType(), model.getTarget(), !model.isBackup());

            EndpointContext context = new EndpointContext();
            if (api.getProperties() != null) {
                context.setProperties(api.getProperties().getValues());
            }
            try {
                io.gravitee.gateway.api.endpoint.Endpoint endpoint = endpointFactory.create(model, context);
                if (endpoint != null) {
                    endpoint.connector().start();

                    endpoints.add(endpoint);
                    endpointsByName.put(endpoint.name(), endpoint);

                    referenceRegister.add(new EndpointReference(endpoint));
                }
            } catch (EndpointException ee) {
                logger.error("An endpoint error occurs while configuring or starting endpoint " + model.getName()
                        + ". Endpoint will not be available to forward requests.", ee);
            }
        } catch (Exception ex) {
            logger.error("Unexpected error while creating endpoint connector", ex);
        }
    }

    public void stop(String endpointName) {
        logger.debug("Closing endpoint: name[{}]", endpointName);

        io.gravitee.gateway.api.endpoint.Endpoint endpoint = endpointsByName.remove(endpointName);
        stop(endpoint);
    }

    private void stop(io.gravitee.gateway.api.endpoint.Endpoint endpoint) {
        if (endpoint != null) {
            try {
                endpoints.remove(endpoint);
                referenceRegister.remove(endpoint.name());
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

    public io.gravitee.gateway.api.endpoint.Endpoint get(String endpointName) {
        return endpointsByName.get(endpointName);
    }

    public Collection<io.gravitee.gateway.api.endpoint.Endpoint> endpoints() {
        return endpoints;
    }

    public LoadBalancedEndpointGroup getLBGroup() {
        return lbGroup;
    }

    public EndpointGroup getGroup() {
        return group;
    }

    public void setEndpointFactory(EndpointFactory endpointFactory) {
        this.endpointFactory = endpointFactory;
    }

    public void setReferenceRegister(ReferenceRegister referenceRegister) {
        this.referenceRegister = referenceRegister;
    }

    public void setApi(Api api) {
        this.api = api;
    }
}
