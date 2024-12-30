/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactive.core.v4.endpoint;

import io.gravitee.common.service.AbstractService;
import io.gravitee.common.utils.UUID;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.reactive.api.connector.endpoint.BaseEndpointConnector;
import io.gravitee.gateway.reactive.api.connector.endpoint.BaseEndpointConnectorFactory;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultEndpointManager extends AbstractService<EndpointManager> implements EndpointManager, TemplateVariableProvider {

    private static final Logger log = LoggerFactory.getLogger(DefaultEndpointManager.class);
    public static final String TEMPLATE_VARIABLE_ENDPOINTS = "endpoints";

    private final Api api;
    private final EndpointConnectorPluginManager endpointConnectorPluginManager;
    private final DeploymentContext deploymentContext;
    private ManagedEndpointGroup defaultGroup;
    private final Map<String, ManagedEndpointGroup> groupsByName;
    private final Map<String, ManagedEndpoint> endpointsByName;
    private final Set<ManagedEndpoint> disabledEndpoints;
    private final Map<String, String> endpointVariables;
    private final Map<String, BiConsumer<Event, ManagedEndpoint>> listeners;

    public DefaultEndpointManager(
        final Api api,
        final EndpointConnectorPluginManager endpointConnectorPluginManager,
        final DeploymentContext deploymentContext
    ) {
        this.api = api;
        this.endpointsByName = new ConcurrentHashMap<>(1);
        this.groupsByName = new ConcurrentHashMap<>(1);
        this.endpointVariables = new ConcurrentHashMap<>(1);
        this.listeners = new ConcurrentHashMap<>(1);
        this.endpointConnectorPluginManager = endpointConnectorPluginManager;
        this.deploymentContext = deploymentContext;
        this.disabledEndpoints = ConcurrentHashMap.newKeySet(0);
    }

    @Override
    public ManagedEndpoint next() {
        return next(EndpointCriteria.NO_CRITERIA);
    }

    @Override
    public List<ManagedEndpoint> all() {
        return new ArrayList<>(endpointsByName.values());
    }

    @Override
    public String addListener(BiConsumer<Event, ManagedEndpoint> endpointConsumer) {
        final String listenerId = UUID.random().toString();
        listeners.put(listenerId, endpointConsumer);

        return listenerId;
    }

    @Override
    public void removeListener(String listenerId) {
        listeners.remove(listenerId);
    }

    @Override
    public ManagedEndpoint next(final EndpointCriteria criteria) {
        final String name = criteria.getName();

        if (name == null) {
            if (defaultGroup != null && criteria.matches(defaultGroup)) {
                return defaultGroup.next();
            }
        } else {
            // First try to find an endpoint by name.
            final ManagedEndpoint managedEndpoint = endpointsByName.get(name);

            if (managedEndpoint != null) {
                if (!disabledEndpoints.contains(managedEndpoint) && criteria.matches(managedEndpoint)) {
                    return managedEndpoint;
                }
                return null;
            }

            final ManagedEndpointGroup managedGroup = groupsByName.get(name);

            if (managedGroup != null && criteria.matches(managedGroup)) {
                return managedGroup.next();
            }
        }
        return null;
    }

    @Override
    public void disable(ManagedEndpoint endpoint) {
        disabledEndpoints.add(endpoint);
        endpoint.getGroup().removeManagedEndpoint(endpoint);
    }

    @Override
    public void enable(ManagedEndpoint endpoint) {
        endpoint.getGroup().addManagedEndpoint(endpoint);
        disabledEndpoints.remove(endpoint);
    }

    @Override
    protected void doStart() throws Exception {
        for (EndpointGroup endpointGroup : api.getEndpointGroups()) {
            final ManagedEndpointGroup managedEndpointGroup = createAndStartGroup(endpointGroup);

            if (defaultGroup == null) {
                defaultGroup = managedEndpointGroup;
            }
        }
    }

    @Override
    public DefaultEndpointManager preStop() {
        for (ManagedEndpoint managedEndpoint : endpointsByName.values()) {
            try {
                managedEndpoint.getConnector().preStop();
            } catch (Exception e) {
                log.warn("An error occurred when pre-stopping endpoint connector [{}].", managedEndpoint.getDefinition().getName());
            }
        }

        return this;
    }

    @Override
    public void doStop() throws Exception {
        for (String endpointName : endpointsByName.keySet()) {
            removeEndpoint(endpointName);
        }

        endpointsByName.clear();
        groupsByName.clear();
    }

    @Override
    public void provide(TemplateContext templateContext) {
        templateContext.setVariable(TEMPLATE_VARIABLE_ENDPOINTS, endpointVariables);
    }

    @Override
    public void addOrUpdateEndpoint(String groupName, Endpoint endpoint) {
        var group = groupsByName.get(groupName);
        var existingEndpoint = endpointsByName.get(endpoint.getName());
        if (existingEndpoint != null) {
            if (existingEndpoint.getDefinition().getConfiguration().equals(endpoint.getConfiguration())) {
                return;
            }

            log.info("The configuration of the endpoint [{}] of group [{}] has changed", endpoint.getName(), groupName);
            removeEndpoint(endpoint.getName());
        }

        log.info("Start endpoint [{}] for group [{}]", endpoint.getName(), groupName);
        createAndStartEndpoint(group, endpoint);
    }

    @Override
    public void removeEndpoint(final String name) {
        log.info("Remove endpoint [{}]", name);
        try {
            final ManagedEndpoint managedEndpoint = endpointsByName.remove(name);
            endpointVariables.remove(name);

            if (managedEndpoint != null) {
                managedEndpoint.getGroup().removeManagedEndpoint(managedEndpoint);
                managedEndpoint.getConnector().stop();

                listeners.values().forEach(l -> l.accept(Event.REMOVE, managedEndpoint));
            }
        } catch (Exception e) {
            log.warn("Unable to properly stop the endpoint connector {}: {}.", name, e.getMessage());
        }
    }

    private ManagedEndpointGroup createAndStartGroup(final EndpointGroup endpointGroup) {
        final ManagedEndpointGroup managedEndpointGroup = new DefaultManagedEndpointGroup(endpointGroup);
        groupsByName.put(endpointGroup.getName(), managedEndpointGroup);

        for (Endpoint endpoint : endpointGroup.getEndpoints()) {
            createAndStartEndpoint(managedEndpointGroup, endpoint);
        }

        return managedEndpointGroup;
    }

    private void createAndStartEndpoint(final ManagedEndpointGroup managedEndpointGroup, final Endpoint endpoint) {
        try {
            final String configuration = getEndpointConfiguration(endpoint);
            final String sharedConfiguration = getSharedConfiguration(managedEndpointGroup.getDefinition(), endpoint);
            final BaseEndpointConnectorFactory<BaseEndpointConnector<?>> connectorFactory = endpointConnectorPluginManager.getFactoryById(
                endpoint.getType()
            );

            if (connectorFactory == null) {
                log.warn(
                    "Endpoint connector {} cannot be instantiated (no factory of type [{}] found). Skipped.",
                    endpoint.getName(),
                    endpoint.getType()
                );
                return;
            }

            final BaseEndpointConnector<?> connector = connectorFactory.createConnector(
                deploymentContext,
                configuration,
                sharedConfiguration
            );

            if (connector == null) {
                log.warn("Endpoint connector {} cannot be started. Skipped.", endpoint.getName());
                return;
            }

            connector.start();

            final ManagedEndpoint managedEndpoint = new DefaultManagedEndpoint(endpoint, managedEndpointGroup, connector);
            managedEndpointGroup.addManagedEndpoint(managedEndpoint);
            endpointsByName.put(endpoint.getName(), managedEndpoint);
            endpointVariables.put(endpoint.getName(), endpoint.getName() + ":");
            endpointVariables.put(managedEndpointGroup.getDefinition().getName(), managedEndpointGroup.getDefinition().getName() + ":");

            listeners.values().forEach(l -> l.accept(Event.ADD, managedEndpoint));
        } catch (Exception e) {
            log.warn("Unable to properly start the endpoint connector {}: {}. Skipped.", endpoint.getName(), e.getMessage());
        }
    }

    private String getEndpointConfiguration(Endpoint endpoint) {
        return endpoint.getConfiguration();
    }

    private String getSharedConfiguration(EndpointGroup endpointGroup, Endpoint endpoint) {
        return endpoint.isInheritConfiguration() ? endpointGroup.getSharedConfiguration() : endpoint.getSharedConfigurationOverride();
    }
}
