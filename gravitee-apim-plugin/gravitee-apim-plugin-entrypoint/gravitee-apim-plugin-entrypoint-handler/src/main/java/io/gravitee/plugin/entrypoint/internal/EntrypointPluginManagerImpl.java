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
package io.gravitee.plugin.entrypoint.internal;

import io.gravitee.gateway.jupiter.api.entrypoint.EntrypointConnectorFactory;
import io.gravitee.plugin.core.api.AbstractConfigurablePluginManager;
import io.gravitee.plugin.core.api.PluginClassLoader;
import io.gravitee.plugin.entrypoint.EntrypointClassLoaderFactory;
import io.gravitee.plugin.entrypoint.EntrypointPlugin;
import io.gravitee.plugin.entrypoint.EntrypointPluginManager;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EntrypointPluginManagerImpl extends AbstractConfigurablePluginManager<EntrypointPlugin> implements EntrypointPluginManager {

    private static final Logger logger = LoggerFactory.getLogger(EntrypointPluginManagerImpl.class);
    private final EntrypointClassLoaderFactory classLoaderFactory;
    private final Map<String, EntrypointConnectorFactory<?>> factories = new HashMap<>();

    public EntrypointPluginManagerImpl(final EntrypointClassLoaderFactory classLoaderFactory) {
        this.classLoaderFactory = classLoaderFactory;
    }

    @Override
    public void register(final EntrypointPlugin plugin) {
        super.register(plugin);

        // Create entrypoint
        PluginClassLoader pluginClassLoader = classLoaderFactory.getOrCreateClassLoader(plugin);
        try {
            final Class<EntrypointConnectorFactory<?>> connectorFactoryClass = (Class<EntrypointConnectorFactory<?>>) pluginClassLoader.loadClass(
                plugin.clazz()
            );
            final EntrypointConnectorFactory<?> factory = connectorFactoryClass.getDeclaredConstructor().newInstance();
            factories.put(plugin.id(), factory);
        } catch (Exception ex) {
            logger.error("Unexpected error while loading entrypoint plugin: {}", plugin.clazz(), ex);
        } finally {
            if (pluginClassLoader != null) {
                try {
                    pluginClassLoader.close();
                } catch (IOException e) {
                    logger.error("Unexpected exception while trying to release the entrypoint plugin classloader", e);
                }
            }
        }
    }

    @Override
    public EntrypointConnectorFactory<?> getFactoryById(final String entrypointPluginId) {
        return factories.get(entrypointPluginId);
    }
}
