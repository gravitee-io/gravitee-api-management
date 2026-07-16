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

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.gamma.core.domain.gravitee_plugin.model.PlatformPlugin;
import io.gravitee.gamma.core.domain.gravitee_plugin.model.PluginMoreInformation;
import io.gravitee.gamma.core.port.service_provider.gravitee_plugin.PluginProvider;
import io.gravitee.node.logging.NodeLoggerFactory;
import io.gravitee.plugin.core.api.ConfigurablePlugin;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import java.io.IOException;
import org.slf4j.Logger;

public abstract class AbstractPluginProvider<T extends ConfigurablePlugin, E extends PlatformPlugin> implements PluginProvider<E> {

    protected final Logger log = NodeLoggerFactory.getLogger(this.getClass());

    protected final ConfigurablePluginManager<T> pluginManager;

    public AbstractPluginProvider(ConfigurablePluginManager<T> pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Override
    public String getSchema(String pluginId) {
        try {
            log.debug("Find plugin schema by ID: {}", pluginId);
            return pluginManager.getSchema(pluginId, true);
        } catch (IOException ioex) {
            throw new TechnicalDomainException("An error occurs while trying to get plugin schema for plugin " + pluginId, ioex);
        }
    }

    @Override
    public String getIcon(String pluginId) {
        try {
            log.debug("Find plugin icon by ID: {}", pluginId);
            return pluginManager.getIcon(pluginId, true);
        } catch (IOException ioex) {
            throw new TechnicalDomainException("An error occurs while trying to get plugin icon for plugin " + pluginId, ioex);
        }
    }

    @Override
    public String getDocumentation(String pluginId) {
        try {
            log.debug("Find plugin documentation by ID: {}", pluginId);
            return pluginManager.getDocumentation(pluginId, true);
        } catch (IOException ioex) {
            throw new TechnicalDomainException("An error occurs while trying to get plugin documentation for plugin " + pluginId, ioex);
        }
    }

    @Override
    public PluginMoreInformation getMoreInformation(String pluginId) {
        try {
            log.debug("Find plugin more information by ID: {}", pluginId);
            var moreInformation = pluginManager.getMoreInformation(pluginId, true);
            return new PluginMoreInformation(
                moreInformation.getDescription(),
                moreInformation.getDocumentationUrl(),
                moreInformation.getSchemaImg()
            );
        } catch (IOException ioex) {
            throw new TechnicalDomainException("An error occurs while trying to get plugin more information for plugin " + pluginId, ioex);
        }
    }

    protected PlatformPlugin convert(T plugin) {
        return new PlatformPlugin(
            plugin.id(),
            plugin.manifest().name(),
            plugin.manifest().description(),
            plugin.manifest().category(),
            plugin.manifest().version(),
            getIcon(plugin.id()),
            plugin.manifest().feature(),
            plugin.deployed()
        );
    }
}
