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

import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnectorConfiguration;
import io.gravitee.plugin.core.api.AbstractSimplePluginHandler;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.spring.EntrypointConnectorPluginConfiguration;
import java.io.IOException;
import java.net.URLClassLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Import(EntrypointConnectorPluginConfiguration.class)
public class EntrypointConnectorPluginHandler extends AbstractSimplePluginHandler<EntrypointConnectorPlugin<?, ?>> {

    @Autowired
    private ConfigurablePluginManager<EntrypointConnectorPlugin<?, ?>> entrypointPluginManager;

    @Override
    public boolean canHandle(final Plugin plugin) {
        return EntrypointConnectorPlugin.PLUGIN_TYPE.equalsIgnoreCase(plugin.type());
    }

    @Override
    protected String type() {
        return EntrypointConnectorPlugin.PLUGIN_TYPE;
    }

    @Override
    protected EntrypointConnectorPlugin<?, ?> create(final Plugin plugin, final Class<?> pluginClass) {
        Class<? extends EntrypointConnectorConfiguration> configurationClass =
            new EntrypointConnectorConfigurationClassFinder().lookupFirst(pluginClass);

        return new DefaultEntrypointConnectorPlugin(plugin, pluginClass, configurationClass);
    }

    @Override
    protected void register(final EntrypointConnectorPlugin<?, ?> entrypointConnectorPlugin) {
        entrypointPluginManager.register(entrypointConnectorPlugin);

        // Once registered, the classloader should be released
        final ClassLoader policyClassLoader = entrypointConnectorPlugin.connectorFactory().getClassLoader();

        if (policyClassLoader instanceof URLClassLoader) {
            URLClassLoader classLoader = (URLClassLoader) policyClassLoader;
            try {
                classLoader.close();
            } catch (IOException e) {
                logger.error("Unexpected exception while trying to release the policy classloader");
            }
        }
    }

    @Override
    protected ClassLoader getClassLoader(Plugin plugin) {
        return new URLClassLoader(plugin.dependencies(), this.getClass().getClassLoader());
    }
}
