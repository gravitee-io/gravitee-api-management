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
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.gateway.core.endpoint.lifecycle.GroupLifecyleManager;
import io.gravitee.gateway.core.endpoint.lifecycle.LoadBalancedEndpointGroup;
import io.gravitee.gateway.core.endpoint.lifecycle.impl.tenant.MultiTenantAwareEndpointLifecycleManager;
import io.gravitee.gateway.core.endpoint.ref.GroupReference;
import io.gravitee.gateway.core.endpoint.ref.ReferenceRegister;
import io.gravitee.gateway.env.GatewayConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultGroupLifecycleManager extends AbstractLifecycleComponent<GroupLifecyleManager>
        implements GroupLifecyleManager, ApplicationContextAware {

    private final Logger logger = LoggerFactory.getLogger(DefaultGroupLifecycleManager.class);

    @Autowired
    private Api api;

    @Autowired
    private ReferenceRegister referenceRegister;

    @Autowired
    private GatewayConfiguration gatewayConfiguration;

    private ApplicationContext applicationContext;

    private final Map<String, EndpointGroupLifecycleManager> groups = new HashMap<>();
    private EndpointGroupLifecycleManager defaultGroup;

    @Override
    public LoadBalancedEndpointGroup get(String groupName) {
        EndpointGroupLifecycleManager group = groups.get(groupName);
        return (group != null) ? group.getGroup() : null;
    }

    @Override
    public LoadBalancedEndpointGroup getDefault() {
        return defaultGroup.getGroup();
    }

    @Override
    public Collection<LoadBalancedEndpointGroup> groups() {
        return groups.values().stream().map(EndpointGroupLifecycleManager::getGroup).collect(Collectors.toSet());
    }

    @Override
    protected void doStart() throws Exception {
        // Start all the groups
        api.getProxy()
                .getGroups()
                .stream()
                .map(new Function<EndpointGroup, EndpointGroupLifecycleManager>() {
                    @Override
                    public EndpointGroupLifecycleManager apply(EndpointGroup group) {
                        EndpointGroupLifecycleManager groupLifecycleManager;

                        if (gatewayConfiguration.tenant().isPresent()) {
                            groupLifecycleManager = new MultiTenantAwareEndpointLifecycleManager(
                                    group, gatewayConfiguration.tenant().get());
                        } else {
                            groupLifecycleManager = new EndpointGroupLifecycleManager(group);
                        }

                        applicationContext.getAutowireCapableBeanFactory().autowireBean(groupLifecycleManager);

                        groups.put(group.getName(), groupLifecycleManager);

                        // Set the first group as the default group
                        if (defaultGroup == null) {
                            defaultGroup = groupLifecycleManager;
                        }

                        return groupLifecycleManager;
                    }
                })
                .forEach(new Consumer<EndpointGroupLifecycleManager>() {
                    @Override
                    public void accept(EndpointGroupLifecycleManager groupLifecycleManager) {
                        try {
                            groupLifecycleManager.start();
                            referenceRegister.add(new GroupReference(groupLifecycleManager.getGroup()));
                        } catch (Exception ex) {
                            logger.error("An error occurs while starting a group of endpoints: name[{}]", groupLifecycleManager);
                        }
                    }
                });
    }

    @Override
    protected void doStop() throws Exception {
        Iterator<EndpointGroupLifecycleManager> ite = groups.values().iterator();
        while (ite.hasNext()) {
            EndpointGroupLifecycleManager group = ite.next();
            group.stop();
            ite.remove();
            referenceRegister.remove(GroupReference.REFERENCE_PREFIX + group.getGroup().getName());
        }

        groups.clear();
        defaultGroup = null;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
