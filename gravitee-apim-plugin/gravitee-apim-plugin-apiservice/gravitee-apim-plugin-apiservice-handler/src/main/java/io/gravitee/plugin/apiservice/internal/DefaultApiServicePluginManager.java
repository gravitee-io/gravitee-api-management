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
package io.gravitee.plugin.apiservice.internal;

import io.gravitee.gateway.reactive.api.apiservice.ApiService;
import io.gravitee.gateway.reactive.api.apiservice.ApiServiceFactory;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.plugin.apiservice.ApiServiceClassLoaderFactory;
import io.gravitee.plugin.apiservice.ApiServicePlugin;
import io.gravitee.plugin.apiservice.ApiServicePluginManager;
import io.gravitee.plugin.core.api.AbstractConfigurablePluginManager;
import io.gravitee.plugin.core.api.PluginClassLoader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@SuppressWarnings("unchecked")
@Slf4j
public class DefaultApiServicePluginManager
    extends AbstractConfigurablePluginManager<ApiServicePlugin<?, ?>>
    implements ApiServicePluginManager {

    private final ApiServiceClassLoaderFactory classLoaderFactory;
    private final Map<String, ApiServiceFactory<?>> factories = new HashMap<>();
    private final Map<String, ApiServiceFactory<?>> notDeployedPluginFactories = new HashMap<>();
    private final PluginConfigurationHelper pluginConfigurationHelper;

    public DefaultApiServicePluginManager(
        final ApiServiceClassLoaderFactory classLoaderFactory,
        PluginConfigurationHelper pluginConfigurationHelper
    ) {
        this.classLoaderFactory = classLoaderFactory;
        this.pluginConfigurationHelper = pluginConfigurationHelper;
    }

    @Override
    public void register(final ApiServicePlugin<?, ?> plugin) {
        super.register(plugin);

        // Create api service.
        PluginClassLoader pluginClassLoader = classLoaderFactory.getOrCreateClassLoader(plugin);
        try {
            final Class<ApiServiceFactory<?>> connectorFactoryClass = (Class<ApiServiceFactory<?>>) pluginClassLoader.loadClass(
                plugin.clazz()
            );
            ApiServiceFactory<?> factory = createFactory(connectorFactoryClass);
            if (plugin.deployed()) {
                factories.put(plugin.id(), factory);
            } else {
                notDeployedPluginFactories.put(plugin.id(), factory);
            }
        } catch (Exception ex) {
            log.error("Unexpected error while loading api service plugin: {}", plugin.clazz(), ex);
        }
    }

    private ApiServiceFactory<?> createFactory(final Class<ApiServiceFactory<?>> connectorFactoryClass)
        throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        ApiServiceFactory<?> factory;
        try {
            Constructor<ApiServiceFactory<?>> constructorWithFactoryHelper = connectorFactoryClass.getDeclaredConstructor(
                PluginConfigurationHelper.class
            );
            factory = constructorWithFactoryHelper.newInstance(pluginConfigurationHelper);
        } catch (NoSuchMethodException e) {
            Constructor<ApiServiceFactory<?>> constructorWithFactoryHelper = connectorFactoryClass.getDeclaredConstructor();
            factory = constructorWithFactoryHelper.newInstance();
        }

        return factory;
    }

    @Override
    public ApiServiceFactory<?> getFactoryById(final String apiServicePluginId) {
        return getFactoryById(apiServicePluginId, false);
    }

    @Override
    public ApiServiceFactory<?> getFactoryById(String apiServicePluginId, boolean includeNotDeployed) {
        ApiServiceFactory<?> factory = factories.get(apiServicePluginId);
        if (factory == null && includeNotDeployed) {
            return notDeployedPluginFactories.get(apiServicePluginId);
        }
        return factory;
    }

    @Override
    public List<?> getAllFactories() {
        return getAllFactories(false);
    }

    @Override
    public List<?> getAllFactories(boolean includeNotDeployed) {
        return getAllFactories(includeNotDeployed, ApiServiceFactory.class);
    }

    @Override
    public <T extends ApiServiceFactory<?>> List<T> getAllFactories(Class<? extends ApiServiceFactory> clazz) {
        return getAllFactories(false, clazz);
    }

    @Override
    public <T extends ApiServiceFactory<?>> List<T> getAllFactories(boolean includeNotDeployed, Class<?> clazz) {
        List<ApiServiceFactory<?>> allFactories = new ArrayList<>(
            factories
                .values()
                .stream()
                .filter(f -> clazz.isAssignableFrom(f.getClass()))
                .toList()
        );
        if (includeNotDeployed) {
            allFactories.addAll(notDeployedPluginFactories.values());
        }
        return (List<T>) allFactories;
    }
}
