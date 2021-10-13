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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.gateway.connector.ConnectorRegistry;
import io.gravitee.gateway.core.endpoint.factory.EndpointFactory;
import io.gravitee.gateway.core.endpoint.lifecycle.GroupLifecycleManager;
import io.gravitee.gateway.core.endpoint.lifecycle.LoadBalancedEndpointGroup;
import io.gravitee.gateway.core.endpoint.lifecycle.impl.tenant.MultiTenantAwareEndpointLifecycleManager;
import io.gravitee.gateway.core.endpoint.ref.GroupReference;
import io.gravitee.gateway.core.endpoint.ref.ReferenceRegister;
import io.gravitee.node.api.configuration.Configuration;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultGroupLifecycleManager extends AbstractLifecycleComponent<GroupLifecycleManager> implements GroupLifecycleManager {

    private final Logger logger = LoggerFactory.getLogger(DefaultGroupLifecycleManager.class);

    private final Api api;

    private final ReferenceRegister referenceRegister;

    private final EndpointFactory endpointFactory;

    private final Map<String, EndpointGroupLifecycleManager> groups = new HashMap<>();

    private final ConnectorRegistry connectorRegistry;

    private final Configuration configuration;

    private final ObjectMapper mapper;

    private final Optional<String> tenant;

    private EndpointGroupLifecycleManager defaultGroup;

    public DefaultGroupLifecycleManager(
        final Api api,
        final ReferenceRegister referenceRegister,
        final EndpointFactory endpointFactory,
        final ConnectorRegistry connectorRegistry,
        final Configuration configuration,
        final ObjectMapper mapper,
        final Optional<String> tenant
    ) {
        this.api = api;
        this.referenceRegister = referenceRegister;
        this.endpointFactory = endpointFactory;
        this.connectorRegistry = connectorRegistry;
        this.configuration = configuration;
        this.mapper = mapper;
        this.tenant = tenant;
    }

    @Override
    public LoadBalancedEndpointGroup get(String groupName) {
        EndpointGroupLifecycleManager group = groups.get(groupName);
        return (group != null) ? group.getLBGroup() : null;
    }

    @Override
    public LoadBalancedEndpointGroup getDefault() {
        return defaultGroup.getLBGroup();
    }

    @Override
    public Collection<LoadBalancedEndpointGroup> groups() {
        return groups.values().stream().map(EndpointGroupLifecycleManager::getLBGroup).collect(Collectors.toSet());
    }

    @Override
    protected void doStart() throws Exception {
        if (api.getProxy().getGroups() != null) {
            // Start all the groups
            api
                .getProxy()
                .getGroups()
                .stream()
                .map(
                    new Function<EndpointGroup, EndpointGroupLifecycleManager>() {
                        @Override
                        public EndpointGroupLifecycleManager apply(EndpointGroup group) {
                            EndpointGroupLifecycleManager groupLifecycleManager;

                            if (tenant.isPresent()) {
                                groupLifecycleManager =
                                    new MultiTenantAwareEndpointLifecycleManager(
                                        api,
                                        group,
                                        endpointFactory,
                                        referenceRegister,
                                        connectorRegistry,
                                        configuration,
                                        mapper,
                                        tenant.get()
                                    );
                            } else {
                                groupLifecycleManager =
                                    new EndpointGroupLifecycleManager(
                                        api,
                                        group,
                                        endpointFactory,
                                        referenceRegister,
                                        connectorRegistry,
                                        configuration,
                                        mapper
                                    );
                            }

                            groups.put(group.getName(), groupLifecycleManager);

                            // Set the first group as the default group
                            if (defaultGroup == null) {
                                defaultGroup = groupLifecycleManager;
                            }

                            return groupLifecycleManager;
                        }
                    }
                )
                .forEach(
                    new Consumer<EndpointGroupLifecycleManager>() {
                        @Override
                        public void accept(EndpointGroupLifecycleManager groupLifecycleManager) {
                            try {
                                groupLifecycleManager.start();
                                referenceRegister.add(new GroupReference(groupLifecycleManager.getLBGroup()));
                            } catch (Exception ex) {
                                logger.error(
                                    "An error occurs while starting a group of endpoints: " + groupLifecycleManager.getGroup().getName(),
                                    ex
                                );
                            }
                        }
                    }
                );
        }
    }

    @Override
    protected void doStop() throws Exception {
        Iterator<EndpointGroupLifecycleManager> ite = groups.values().iterator();
        while (ite.hasNext()) {
            EndpointGroupLifecycleManager group = ite.next();
            group.stop();
            ite.remove();
            //skip if no LbGroup (in case of an empty endpoint group)
            if (group.getGroup() != null) {
                referenceRegister.remove(group.getGroup().getName());
            }
        }

        groups.clear();
        defaultGroup = null;
    }
}
