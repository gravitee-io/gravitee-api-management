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
package io.gravitee.plugin.console.internal;

import io.gravitee.plugin.console.ConsoleExtension;
import io.gravitee.plugin.console.ConsoleExtensionManager;
import io.gravitee.plugin.console.ConsoleExtensionServletFactory;
import io.gravitee.plugin.core.api.AbstractPluginManager;
import io.gravitee.plugin.core.api.PluginDocumentation;
import io.gravitee.plugin.core.api.PluginMoreInformation;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;

/**
 * @author GraviteeSource Team
 */
public class DefaultConsoleExtensionManager extends AbstractPluginManager<ConsoleExtension> implements ConsoleExtensionManager {

    @Getter
    private static final DefaultConsoleExtensionManager instance = new DefaultConsoleExtensionManager();

    private final Map<String, Class<?>> pluginResourceClasses = new ConcurrentHashMap<>();
    private final Map<String, ConsoleExtensionServletFactory> servletFactories = new ConcurrentHashMap<>();

    private DefaultConsoleExtensionManager() {}

    @Override
    public void registerResourceClass(String pluginId, Class<?> resourceClass) {
        pluginResourceClasses.put(pluginId, resourceClass);
    }

    @Override
    public Class<?> getResourceClass(String pluginId) {
        return pluginResourceClasses.get(pluginId);
    }

    public Map<String, Class<?>> getPluginResourceClasses() {
        return Collections.unmodifiableMap(pluginResourceClasses);
    }

    @Override
    public void registerServletFactory(String pluginId, ConsoleExtensionServletFactory factory) {
        servletFactories.put(pluginId, factory);
    }

    @Override
    public ConsoleExtensionServletFactory getServletFactory(String pluginId) {
        return servletFactories.get(pluginId);
    }

    @Override
    public Collection<Map.Entry<String, ConsoleExtensionServletFactory>> getServletFactories() {
        return Collections.unmodifiableSet(servletFactories.entrySet());
    }

    @Override
    public String getIcon(String pluginId) throws IOException {
        return null;
    }

    @Override
    public String getIcon(String pluginId, boolean includeNotDeployed) throws IOException {
        return null;
    }

    @Override
    public String getDocumentation(String pluginId) throws IOException {
        return null;
    }

    @Override
    public PluginDocumentation getPluginDocumentation(String pluginId) throws IOException {
        return null;
    }

    @Override
    public String getDocumentation(String pluginId, boolean includeNotDeployed) throws IOException {
        return null;
    }

    @Override
    public PluginDocumentation getPluginDocumentation(String pluginId, boolean includeNotDeployed) throws IOException {
        return null;
    }

    @Override
    public String getDocumentation(String pluginId, String propertyKey, boolean fallbackToDocumentation, boolean includeNotDeployed)
        throws IOException {
        return null;
    }

    @Override
    public PluginDocumentation getPluginDocumentation(
        String pluginId,
        String propertyKey,
        boolean fallbackToDocumentation,
        boolean includeNotDeployed
    ) throws IOException {
        return null;
    }

    @Override
    public String getCategory(String pluginId) throws IOException {
        return null;
    }

    @Override
    public String getCategory(String pluginId, boolean includeNotDeployed) throws IOException {
        return null;
    }

    @Override
    public PluginMoreInformation getMoreInformation(String pluginId) throws IOException {
        return null;
    }

    @Override
    public PluginMoreInformation getMoreInformation(String pluginId, boolean includeNotDeployed) throws IOException {
        return null;
    }
}
