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
package io.gravitee.rest.api.service.impl;

import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.rest.api.model.platform.plugin.PlatformPluginEntity;
import io.gravitee.rest.api.service.JsonSchemaService;
import io.gravitee.rest.api.service.ResourceService;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ResourceServiceImpl extends AbstractPluginService<ResourcePlugin<?>, PlatformPluginEntity> implements ResourceService {

    public ResourceServiceImpl(JsonSchemaService jsonSchemaService, ConfigurablePluginManager<ResourcePlugin<?>> pluginManager) {
        super(jsonSchemaService, pluginManager);
    }

    @Override
    public Set<PlatformPluginEntity> findAll() {
        return super.list().stream().map(this::convert).collect(Collectors.toSet());
    }

    @Override
    public PlatformPluginEntity findById(String resource) {
        return convert(super.get(resource));
    }

    @Override
    public void validateResourceConfiguration(Resource resource) {
        validateConfiguration(resource.getType(), resource.getConfiguration());
    }

    @Override
    public void validateResourceConfiguration(io.gravitee.definition.model.v4.resource.Resource resource) {
        validateConfiguration(resource.getType(), resource.getConfiguration());
    }

    @Override
    protected PlatformPluginEntity convert(Plugin plugin) {
        PlatformPluginEntity entity = super.convert(plugin);
        entity.setIcon(getIcon(plugin.id()));
        return entity;
    }
}
