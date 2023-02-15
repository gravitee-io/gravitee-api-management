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
package io.gravitee.gateway.resource.internal.v4;

import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.jupiter.api.context.DeploymentContext;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.resource.ResourceConfigurationFactory;
import io.gravitee.gateway.resource.internal.ResourceLoader;
import io.gravitee.gateway.resource.internal.legacy.LegacyResourceManagerImpl;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.resource.ResourceClassLoaderFactory;
import io.gravitee.plugin.resource.ResourcePlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class DefaultResourceManager extends LegacyResourceManagerImpl {

    private final ResourceLoader resourceLoader;

    public DefaultResourceManager(
        final DefaultClassLoader classLoader,
        final Reactable reactable,
        final ConfigurablePluginManager<ResourcePlugin<?>> resourcePluginManager,
        final ResourceClassLoaderFactory resourceClassLoaderFactory,
        final ResourceConfigurationFactory resourceConfigurationFactory,
        final ApplicationContext applicationContext,
        final DeploymentContext deploymentContext
    ) {
        super(reactable, resourcePluginManager, resourceClassLoaderFactory, resourceConfigurationFactory, applicationContext);
        this.resourceLoader =
            new ResourceLoader(
                classLoader,
                resourcePluginManager,
                resourceClassLoaderFactory,
                resourceConfigurationFactory,
                applicationContext,
                deploymentContext
            );
    }

    protected void initialize() {
        reactable
            .dependencies(Resource.class)
            .stream()
            .filter(Resource::isEnabled)
            .forEach(
                resource -> {
                    log.debug("Loading resource {} for {}", resource.getName(), reactable);
                    final io.gravitee.resource.api.Resource resourceInstance = resourceLoader.load(
                        resource.getType(),
                        resource.getConfiguration()
                    );

                    if (resourceInstance != null) {
                        resources.put(resource.getName(), resourceInstance);
                    }
                }
            );
    }
}
