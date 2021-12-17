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

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.resource.ResourceConfigurationFactory;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.gateway.resource.internal.legacy.LegacyResourceManagerImpl;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.PluginClassLoader;
import io.gravitee.plugin.resource.ResourceClassLoaderFactory;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.resource.api.ResourceConfiguration;
import io.gravitee.resource.api.ResourceManager;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ClassUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResourceManagerImpl extends LegacyResourceManagerImpl {

    private final Logger logger = LoggerFactory.getLogger(ResourceManagerImpl.class);
    private final boolean legacyMode;
    private final DefaultClassLoader classLoader;

    public ResourceManagerImpl(
        final boolean legacyMode,
        final DefaultClassLoader classLoader,
        final Reactable reactable,
        final ConfigurablePluginManager<ResourcePlugin<?>> resourcePluginManager,
        final ResourceClassLoaderFactory resourceClassLoaderFactory,
        final ResourceConfigurationFactory resourceConfigurationFactory,
        final ApplicationContext applicationContext
    ) {
        super(reactable, resourcePluginManager, resourceClassLoaderFactory, resourceConfigurationFactory, applicationContext);
        this.legacyMode = legacyMode;
        this.classLoader = classLoader;
    }

    protected void initialize() {
        if (legacyMode) {
            super.initialize();
        } else {
            Set<Resource> resourceDeps = reactable.dependencies(Resource.class);

            resourceDeps.forEach(
                resource -> {
                    final ResourcePlugin resourcePlugin = resourcePluginManager.get(resource.getType());
                    if (resourcePlugin == null) {
                        logger.error("Resource [{}] can not be found in plugin registry", resource.getType());
                        throw new IllegalStateException("Resource [" + resource.getType() + "] can not be found in plugin registry");
                    }

                    classLoader.addClassLoader(
                        resourcePlugin.resource().getCanonicalName(),
                        () -> resourceClassLoaderFactory.getOrCreateClassLoader(resourcePlugin, reactable.getClass().getClassLoader())
                    );

                    logger.debug("Loading resource {} for {}", resource.getName(), reactable);

                    try {
                        Class<? extends io.gravitee.resource.api.Resource> resourceClass = (Class<? extends io.gravitee.resource.api.Resource>) ClassUtils.forName(
                            resourcePlugin.resource().getName(),
                            classLoader
                        );
                        Map<Class<?>, Object> injectables = new HashMap<>();

                        if (resourcePlugin.configuration() != null) {
                            Class<? extends ResourceConfiguration> resourceConfigurationClass = (Class<? extends ResourceConfiguration>) ClassUtils.forName(
                                resourcePlugin.configuration().getName(),
                                classLoader
                            );
                            injectables.put(
                                resourceConfigurationClass,
                                resourceConfigurationFactory.create(resourceConfigurationClass, resource.getConfiguration())
                            );
                        }

                        io.gravitee.resource.api.Resource resourceInstance = new ResourceFactory().create(resourceClass, injectables);

                        if (resourceInstance instanceof ApplicationContextAware) {
                            ((ApplicationContextAware) resourceInstance).setApplicationContext(applicationContext);
                        }

                        resources.put(resource.getName(), resourceInstance);
                    } catch (Exception ex) {
                        logger.error("Unable to create resource", ex);
                        try {
                            classLoader.removeClassLoader(resourcePlugin.resource().getCanonicalName());
                        } catch (IOException ioe) {
                            logger.error("Unable to close classloader for resource", ioe);
                        }
                    }
                }
            );
        }
    }
}
