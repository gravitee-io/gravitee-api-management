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
import io.gravitee.gateway.http.core.endpoint.HttpEndpoint;
import io.gravitee.gateway.http.core.endpoint.EndpointLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointLifecycleManagerImpl extends AbstractLifecycleComponent<EndpointLifecycleManager> implements EndpointLifecycleManager<HttpClient>, ApplicationContextAware {

    private final Logger LOGGER = LoggerFactory.getLogger(EndpointLifecycleManagerImpl.class);

    @Autowired
    private Api api;

    private ApplicationContext applicationContext;

    private final Map<String, HttpEndpoint> endpoints = new LinkedHashMap<>();
    private final Map<String, String> endpointsTarget = new LinkedHashMap<>();

    @Override
    protected void doStart() throws Exception {
        api.getProxy().getEndpoints()
                .stream()
                .filter(endpoint -> ! endpoint.isBackup())
                .forEach(endpoint -> {
                    try {
                        LOGGER.debug("Preparing a new target endpoint: {} [{}]", endpoint.getName(), endpoint.getTarget());
                        HttpClient httpClient = applicationContext.getBean(HttpClient.class, endpoint);
                        httpClient.start();
                        endpoints.put(endpoint.getName(), new HttpEndpoint(endpoint, httpClient));
                        endpointsTarget.put(endpoint.getName(), endpoint.getTarget());
                    } catch (Exception ex) {
                        LOGGER.error("Unexpected error while creating endpoint connector", ex);
                    }
                });
    }

    @Override
    protected void doStop() throws Exception {
        for(Map.Entry<String, HttpEndpoint> connectors : endpoints.entrySet()) {
            try {
                LOGGER.debug("Closing target endpoint: {}", connectors.getKey());
                connectors.getValue().getHttpClient().stop();
                endpoints.remove(connectors.getKey());
                endpointsTarget.remove(connectors.getKey());
            } catch (Exception ex) {
                LOGGER.error("Unexpected error while closing endpoint connector", ex);
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
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
}
