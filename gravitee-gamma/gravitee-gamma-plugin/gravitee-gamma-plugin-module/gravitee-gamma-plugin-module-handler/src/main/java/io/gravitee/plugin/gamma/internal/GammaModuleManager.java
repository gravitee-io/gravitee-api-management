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
package io.gravitee.plugin.gamma.internal;

import io.gravitee.plugin.core.api.AbstractPluginManager;
import io.gravitee.plugin.core.api.PluginDocumentation;
import io.gravitee.plugin.core.api.PluginMoreInformation;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;

/**
 * Manager for Gamma module plugins.
 * It is responsible for loading and managing Gamma module plugins, as well as providing access to their REST resource classes.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GammaModuleManager extends AbstractPluginManager<GammaModulePlugin> {

    @Getter
    private static final GammaModuleManager instance = new GammaModuleManager();

    private final Map<String, Class<?>> restResourceClasses = new ConcurrentHashMap<>();

    private GammaModuleManager() {
        // Private constructor to enforce singleton pattern.
    }

    public void registerResourceClass(String pluginId, Class<?> resourceClass) {
        restResourceClasses.put(pluginId, resourceClass);
    }

    public Class<?> getResourceClass(String pluginId) {
        return restResourceClasses.get(pluginId);
    }

    public Map<String, Class<?>> getRestResourceClasses() {
        return Collections.unmodifiableMap(restResourceClasses);
    }

    @Override
    public String getIcon(String pluginId) {
        return null;
    }

    @Override
    public String getIcon(String pluginId, boolean includeNotDeployed) {
        return null;
    }

    @Override
    public String getDocumentation(String pluginId) {
        return null;
    }

    @Override
    public PluginDocumentation getPluginDocumentation(String pluginId) {
        return null;
    }

    @Override
    public String getDocumentation(String pluginId, boolean includeNotDeployed) {
        return null;
    }

    @Override
    public PluginDocumentation getPluginDocumentation(String pluginId, boolean includeNotDeployed) {
        return null;
    }

    @Override
    public String getDocumentation(String pluginId, String propertyKey, boolean fallbackToDocumentation, boolean includeNotDeployed) {
        return null;
    }

    @Override
    public PluginDocumentation getPluginDocumentation(
        String pluginId,
        String propertyKey,
        boolean fallbackToDocumentation,
        boolean includeNotDeployed
    ) {
        return null;
    }

    @Override
    public String getCategory(String pluginId) {
        return null;
    }

    @Override
    public String getCategory(String pluginId, boolean includeNotDeployed) {
        return null;
    }

    @Override
    public PluginMoreInformation getMoreInformation(String pluginId) {
        return null;
    }

    @Override
    public PluginMoreInformation getMoreInformation(String pluginId, boolean includeNotDeployed) {
        return null;
    }
}
