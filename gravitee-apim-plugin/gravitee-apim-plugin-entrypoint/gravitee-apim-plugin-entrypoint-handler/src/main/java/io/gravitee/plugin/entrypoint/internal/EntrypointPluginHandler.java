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

import io.gravitee.gateway.jupiter.api.entrypoint.EntrypointConnectorConfiguration;
import io.gravitee.plugin.core.api.AbstractSimplePluginHandler;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.entrypoint.EntrypointPlugin;
import java.net.URLClassLoader;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EntrypointPluginHandler extends AbstractSimplePluginHandler<EntrypointPlugin<?>> {

    @Autowired
    private ConfigurablePluginManager<EntrypointPlugin<?>> entrypointPluginManager;

    @Override
    public boolean canHandle(final Plugin plugin) {
        return EntrypointPlugin.PLUGIN_TYPE.equalsIgnoreCase(plugin.type());
    }

    @Override
    protected String type() {
        return EntrypointPlugin.PLUGIN_TYPE;
    }

    @Override
    protected EntrypointPlugin<?> create(final Plugin plugin, final Class<?> pluginClass) {
        Class<? extends EntrypointConnectorConfiguration> configurationClass = new EntrypointConfigurationClassFinder()
        .lookupFirst(pluginClass);

        return new EntrypointPluginImpl(plugin, pluginClass, configurationClass);
    }

    @Override
    protected void register(EntrypointPlugin<?> entrypointPlugin) {
        entrypointPluginManager.register(entrypointPlugin);
    }

    @Override
    protected ClassLoader getClassLoader(Plugin plugin) {
        return new URLClassLoader(plugin.dependencies(), this.getClass().getClassLoader());
    }
}
