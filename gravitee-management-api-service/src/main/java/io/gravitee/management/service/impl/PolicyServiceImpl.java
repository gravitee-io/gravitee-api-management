/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.management.service.impl;

import io.gravitee.management.model.PolicyEntity;
import io.gravitee.management.service.PolicyService;
import io.gravitee.plugin.api.Plugin;
import io.gravitee.plugin.api.PluginRegistry;
import io.gravitee.plugin.api.PluginType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class PolicyServiceImpl extends TransactionalService implements PolicyService {

    @Autowired
    private PluginRegistry pluginRegistry;

    @Override
    public Set<PolicyEntity> findAll() {
        final Collection<Plugin> plugins = pluginRegistry.plugins(PluginType.POLICY);

        if (plugins == null || plugins.isEmpty()) {
            return emptySet();
        }

        return plugins.stream()
                .map(plugin -> convert(plugin))
                .collect(Collectors.toSet());
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
