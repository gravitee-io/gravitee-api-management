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
package io.gravitee.plugin.entrypoint.internal;

import io.gravitee.gateway.jupiter.api.connector.AbstractConnectorFactory;
import io.gravitee.gateway.jupiter.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.plugin.core.api.AbstractConfigurablePluginManager;
import io.gravitee.plugin.core.api.PluginClassLoader;
import io.gravitee.plugin.entrypoint.EntrypointConnectorClassLoaderFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultEntrypointConnectorPluginManager
    extends AbstractConfigurablePluginManager<EntrypointConnectorPlugin<?>>
    implements EntrypointConnectorPluginManager {

    private static final Logger logger = LoggerFactory.getLogger(DefaultEntrypointConnectorPluginManager.class);
    private final EntrypointConnectorClassLoaderFactory classLoaderFactory;
    private final Map<String, AbstractConnectorFactory<? extends EntrypointConnector>> factories = new HashMap<>();

    public DefaultEntrypointConnectorPluginManager(final EntrypointConnectorClassLoaderFactory classLoaderFactory) {
        this.classLoaderFactory = classLoaderFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void register(final EntrypointConnectorPlugin<?> plugin) {
        super.register(plugin);

        // Create entrypoint
        PluginClassLoader pluginClassLoader = classLoaderFactory.getOrCreateClassLoader(plugin);
        try {
            final Class<AbstractConnectorFactory<? extends EntrypointConnector>> connectorFactoryClass =
                (Class<AbstractConnectorFactory<? extends EntrypointConnector>>) pluginClassLoader.loadClass(plugin.clazz());
            final AbstractConnectorFactory<? extends EntrypointConnector> factory = connectorFactoryClass
                .getDeclaredConstructor()
                .newInstance();
            factories.put(plugin.id(), factory);
        } catch (Exception ex) {
            logger.error("Unexpected error while loading entrypoint plugin: {}", plugin.clazz(), ex);
        }
    }

    @Override
    public <T extends AbstractConnectorFactory<U>, U extends EntrypointConnector> T getFactoryById(final String entrypointPluginId) {
        return (T) factories.get(entrypointPluginId);
    }
}
