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
package io.gravitee.gateway.resource.internal;

import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.jupiter.api.context.DeploymentContext;
import io.gravitee.gateway.resource.ResourceConfigurationFactory;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.resource.ResourceClassLoaderFactory;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.resource.api.Resource;
import io.gravitee.resource.api.ResourceConfiguration;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ClassUtils;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class ResourceLoader {

    private final DefaultClassLoader classLoader;
    private final ConfigurablePluginManager<ResourcePlugin<?>> resourcePluginManager;
    private final ResourceClassLoaderFactory resourceClassLoaderFactory;
    private final ResourceConfigurationFactory resourceConfigurationFactory;
    private final ApplicationContext applicationContext;
    private final DeploymentContext deploymentContext;

    public ResourceLoader(
        final DefaultClassLoader classLoader,
        final ConfigurablePluginManager<ResourcePlugin<?>> resourcePluginManager,
        final ResourceClassLoaderFactory resourceClassLoaderFactory,
        final ResourceConfigurationFactory resourceConfigurationFactory,
        final ApplicationContext applicationContext,
        final DeploymentContext deploymentContext
    ) {
        this.classLoader = classLoader;
        this.resourcePluginManager = resourcePluginManager;
        this.resourceClassLoaderFactory = resourceClassLoaderFactory;
        this.resourceConfigurationFactory = resourceConfigurationFactory;
        this.applicationContext = applicationContext;
        this.deploymentContext = deploymentContext;
    }

    @SuppressWarnings("unchecked")
    public Resource load(final String resourceType, final String resourceConfiguration) {
        final ResourcePlugin<?> resourcePlugin = resourcePluginManager.get(resourceType);

        if (resourcePlugin == null) {
            throw new IllegalStateException("Resource [" + resourceType + "] can not be found in plugin registry");
        }

        classLoader.addClassLoader(
            resourcePlugin.resource().getCanonicalName(),
            () -> resourceClassLoaderFactory.getOrCreateClassLoader(resourcePlugin, classLoader)
        );

        try {
            Class<? extends Resource> resourceClass = (Class<? extends Resource>) ClassUtils.forName(
                resourcePlugin.resource().getName(),
                classLoader
            );
            Map<Class<?>, Object> injectables = new HashMap<>();
            injectables.put(DeploymentContext.class, deploymentContext);

            if (resourcePlugin.configuration() != null) {
                Class<? extends ResourceConfiguration> resourceConfigurationClass = (Class<? extends ResourceConfiguration>) ClassUtils.forName(
                    resourcePlugin.configuration().getName(),
                    classLoader
                );
                injectables.put(
                    resourceConfigurationClass,
                    resourceConfigurationFactory.create(resourceConfigurationClass, resourceConfiguration)
                );
            }

            Resource resourceInstance = new ResourceFactory().create(resourceClass, injectables);

            if (resourceInstance instanceof ApplicationContextAware) {
                ((ApplicationContextAware) resourceInstance).setApplicationContext(applicationContext);
            }

            return resourceInstance;
        } catch (Exception ex) {
            log.error("Unable to create resource", ex);
            try {
                classLoader.removeClassLoader(resourcePlugin.resource().getCanonicalName());
            } catch (IOException ioe) {
                log.error("Unable to close classloader for resource", ioe);
            }
        }

        return null;
    }
}
