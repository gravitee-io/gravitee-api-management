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
import io.gravitee.common.util.ChangeListener;
import io.gravitee.common.util.ObservableCollection;
import io.gravitee.common.util.ObservableSet;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.EndpointType;
import io.gravitee.gateway.api.Connector;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.http.core.endpoint.EndpointLifecycleManager;
import io.gravitee.gateway.http.core.endpoint.HttpEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultEndpointLifecycleManager extends AbstractLifecycleComponent<EndpointLifecycleManager> implements
        EndpointLifecycleManager, ChangeListener<io.gravitee.definition.model.Endpoint>, ApplicationContextAware {

    private final Logger logger = LoggerFactory.getLogger(DefaultEndpointLifecycleManager.class);

    @Autowired
    private Api api;

    private ApplicationContext applicationContext;

    private final Map<String, Endpoint> endpointsByName = new LinkedHashMap<>();
    private final Map<String, String> endpointsTarget = new LinkedHashMap<>();
    private final ObservableCollection<Endpoint> endpoints = new ObservableCollection<>(new ArrayList<>());

    @Override
    protected void doStart() throws Exception {
        // Wrap endpointsByName with an observable collection
        ObservableSet<io.gravitee.definition.model.Endpoint> endpoints = new ObservableSet<>(api.getProxy().getEndpoints());
        endpoints.addListener(this);
        api.getProxy().setEndpoints(endpoints);

        endpoints
                .stream()
                .filter(filter())
                .forEach(this::start);
    }

    protected Predicate<io.gravitee.definition.model.Endpoint> filter() {
        return endpoint -> !endpoint.isBackup();
    }

    @Override
    protected void doStop() throws Exception {
        for (String s : endpointsByName.keySet()) {
            stop(s);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Endpoint get(String endpointName) {
        return endpointsByName.get(endpointName);
    }

    @Override
    public Collection<Endpoint> endpoints() {
        return endpoints;
    }

    @Override
    public Map<String, String> targetByEndpoint() {
        return Collections.unmodifiableMap(endpointsTarget);
    }

    public void start(io.gravitee.definition.model.Endpoint endpoint) {
        try {
            logger.info("Create new endpoint: name[{}] type[{}] target[{}]",
                    endpoint.getName(), endpoint.getType(), endpoint.getTarget());

            if (endpoint.getType() == EndpointType.HTTP) {
                //TODO: Later, when multiple endpoint type will be supported,
                // select the connector factory according to the endpoint type
                Connector connector = applicationContext.getBean(Connector.class, endpoint);
                connector.start();
                HttpEndpoint httpEndpoint = new HttpEndpoint((io.gravitee.definition.model.endpoint.HttpEndpoint) endpoint, connector);
                endpoints.add(httpEndpoint);
                endpointsByName.put(endpoint.getName(), httpEndpoint);
                endpointsTarget.put(endpoint.getName(), endpoint.getTarget());
            }
        } catch (Exception ex) {
            logger.error("Unexpected error while creating endpoint connector", ex);
        }
    }

    public void stop(String endpointName) {
        logger.info("Closing endpoint: name[{}]", endpointName);

        Endpoint endpoint = endpointsByName.remove(endpointName);
        if (endpoint != null) {
            try {
                endpoints.remove(endpoint);
                endpointsTarget.remove(endpointName);
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
}
