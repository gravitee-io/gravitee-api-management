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

import io.gravitee.gateway.reactive.api.connector.endpoint.EndpointConnectorConfiguration;
import io.gravitee.plugin.core.api.AbstractSimplePluginHandler;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.spring.EndpointConnectorPluginConfiguration;
import java.io.IOException;
import java.net.URLClassLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * @author GraviteeSource Team
 */
@Import(EndpointConnectorPluginConfiguration.class)
public class EndpointConnectorPluginHandler extends AbstractSimplePluginHandler<EndpointConnectorPlugin<?, ?>> {

    @Autowired
    private DefaultEndpointConnectorPluginManager endpointPluginManager;

    @Override
    public boolean canHandle(final Plugin plugin) {
        return EndpointConnectorPlugin.PLUGIN_TYPE.equalsIgnoreCase(plugin.type());
    }

    @Override
    protected String type() {
        return EndpointConnectorPlugin.PLUGIN_TYPE;
    }

    @Override
    protected EndpointConnectorPlugin<?, ?> create(final Plugin plugin, final Class<?> pluginClass) {
        Class<? extends EndpointConnectorConfiguration> configurationClass = new EndpointConnectorConfigurationClassFinder().lookupFirst(
            pluginClass
        );

        return new DefaultEndpointConnectorPlugin(plugin, pluginClass, configurationClass);
    }

    @Override
    protected void register(EndpointConnectorPlugin<?, ?> endpointConnectorPlugin) {
        endpointPluginManager.register(endpointConnectorPlugin);

        // Once registered, the classloader should be released
        final ClassLoader policyClassLoader = endpointConnectorPlugin.connectorFactory().getClassLoader();

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
