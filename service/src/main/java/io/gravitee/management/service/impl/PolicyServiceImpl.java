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
package io.gravitee.management.service.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.management.model.PolicyEntity;
import io.gravitee.management.service.PolicyService;
import io.gravitee.plugin.api.Plugin;
import io.gravitee.plugin.api.PluginRegistry;
import io.gravitee.plugin.api.PluginType;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class PolicyServiceImpl extends TransactionalService implements PolicyService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(PolicyServiceImpl.class);

    @Autowired
    private PluginRegistry pluginRegistry;

    @Override
    public Set<PolicyEntity> findAll() {
        Collection<Plugin> plugins = pluginRegistry.plugins(PluginType.POLICY);
        Set<PolicyEntity> policies = new HashSet<>(plugins.size());

        for(Plugin plugin : plugins) {
            policies.add(convert(plugin));
        }

        return policies;
    }

    private PolicyEntity convert(Plugin plugin) {
        PolicyEntity entity = new PolicyEntity();

        entity.setId(plugin.manifest().id());
        entity.setName(plugin.manifest().name());
        entity.setDescription(plugin.manifest().description());
        entity.setVersion(plugin.manifest().version());

        return entity;
    }
}
