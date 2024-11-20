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
package io.gravitee.plugin.endpoint.internal;

import io.gravitee.gateway.reactive.api.connector.endpoint.EndpointConnectorFactory;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.plugin.core.api.AbstractConfigurablePluginManager;
import io.gravitee.plugin.core.api.PluginClassLoader;
import io.gravitee.plugin.endpoint.EndpointConnectorClassLoaderFactory;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GraviteeSource Team
 */
@SuppressWarnings("unchecked")
public class DefaultEndpointConnectorPluginManager
    extends AbstractConfigurablePluginManager<EndpointConnectorPlugin<?, ?>>
    implements EndpointConnectorPluginManager {

    private static final Logger logger = LoggerFactory.getLogger(DefaultEndpointConnectorPluginManager.class);
    private final EndpointConnectorClassLoaderFactory classLoaderFactory;
    private final Map<String, EndpointConnectorFactory<?>> factories = new HashMap<>();

    private final Map<String, EndpointConnectorFactory<?>> notDeployedPluginFactories = new HashMap<>();
    private final PluginConfigurationHelper pluginConfigurationHelper;

    public DefaultEndpointConnectorPluginManager(
        final EndpointConnectorClassLoaderFactory classLoaderFactory,
        final PluginConfigurationHelper pluginConfigurationHelper
    ) {
        this.classLoaderFactory = classLoaderFactory;
        this.pluginConfigurationHelper = pluginConfigurationHelper;
    }

    @Override
    public void register(final EndpointConnectorPlugin<?, ?> plugin) {
        super.register(plugin);

        // Create endpoint
        PluginClassLoader pluginClassLoader = classLoaderFactory.getOrCreateClassLoader(plugin);
        try {
            final Class<EndpointConnectorFactory<?>> connectorFactoryClass =
                (Class<EndpointConnectorFactory<?>>) pluginClassLoader.loadClass(plugin.clazz());
            EndpointConnectorFactory<?> factory = createFactory(connectorFactoryClass);
            if (plugin.deployed()) {
                factories.put(plugin.id(), factory);
            } else {
                notDeployedPluginFactories.put(plugin.id(), factory);
            }
        } catch (Exception ex) {
            logger.error("Unexpected error while loading endpoint plugin: {}", plugin.clazz(), ex);
        }
    }

    private EndpointConnectorFactory<?> createFactory(final Class<EndpointConnectorFactory<?>> connectorFactoryClass)
        throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        EndpointConnectorFactory<?> factory;
        try {
            Constructor<EndpointConnectorFactory<?>> constructorWithConfigurationHelper = connectorFactoryClass.getDeclaredConstructor(
                PluginConfigurationHelper.class
            );
            factory = constructorWithConfigurationHelper.newInstance(pluginConfigurationHelper);
        } catch (NoSuchMethodException e) {
            Constructor<EndpointConnectorFactory<?>> emptyConstructor = connectorFactoryClass.getDeclaredConstructor();
            factory = emptyConstructor.newInstance();
        }
        return factory;
    }

    @Override
    public EndpointConnectorFactory<?> getFactoryById(final String endpointPluginId) {
        return getFactoryById(endpointPluginId, false);
    }

    @Override
    public EndpointConnectorFactory<?> getFactoryById(String endpointPluginId, boolean includeNotDeployed) {
        EndpointConnectorFactory<?> factory = factories.get(endpointPluginId);
        if (factory == null && includeNotDeployed) {
            return notDeployedPluginFactories.get(endpointPluginId);
        }
        return factory;
    }

    @Override
    public String getSharedConfigurationSchema(String pluginId, boolean includeNotDeployed) throws IOException {
        String schemaByPropertyKey = getSchema(pluginId, "schema.sharedConfiguration", false, includeNotDeployed);
        if (schemaByPropertyKey != null) {
            return schemaByPropertyKey;
        }

        // If no schema found by property key, try to find it with Deprecated method.
        // This is for backward compatibility
        return getSchema(pluginId, "sharedConfiguration", includeNotDeployed);
    }
}
