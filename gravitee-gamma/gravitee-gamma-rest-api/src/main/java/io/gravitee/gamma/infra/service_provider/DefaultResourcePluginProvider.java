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
package io.gravitee.gamma.infra.service_provider;

import io.gravitee.gamma.core.domain.gravitee_plugin.model.PlatformPlugin;
import io.gravitee.gamma.core.port.service_provider.gravitee_plugin.ResourcePluginProvider;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.resource.ResourcePlugin;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DefaultResourcePluginProvider
    extends AbstractPluginProvider<ResourcePlugin<?>, PlatformPlugin>
    implements ResourcePluginProvider {

    public DefaultResourcePluginProvider(ConfigurablePluginManager<ResourcePlugin<?>> pluginManager) {
        super(pluginManager);
    }

    @Override
    public Set<PlatformPlugin> findAll() {
        return pluginManager.findAll().stream().map(super::convert).collect(Collectors.toSet());
    }

    @Override
    public PlatformPlugin findById(String id) {
        return super.convert(pluginManager.get(id));
    }
}
