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

import io.gravitee.gateway.reactive.api.apiservice.ApiServiceConfiguration;
import io.gravitee.plugin.apiservice.ApiServicePlugin;
import io.gravitee.plugin.apiservice.ApiServicePluginManager;
import io.gravitee.plugin.apiservice.spring.ApiServicePluginConfiguration;
import io.gravitee.plugin.core.api.AbstractSimplePluginHandler;
import io.gravitee.plugin.core.api.Plugin;
import java.io.IOException;
import java.net.URLClassLoader;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
@NoArgsConstructor
@Import(ApiServicePluginConfiguration.class)
public class ApiServicePluginHandler extends AbstractSimplePluginHandler<ApiServicePlugin<?, ?>> {

    @Autowired
    private ApiServicePluginManager apiServicePluginManager;

    @Override
    public boolean canHandle(final Plugin plugin) {
        return ApiServicePlugin.PLUGIN_TYPE.equalsIgnoreCase(plugin.type());
    }

    @Override
    protected String type() {
        return ApiServicePlugin.PLUGIN_TYPE;
    }

    @Override
    protected ApiServicePlugin<?, ?> create(final Plugin plugin, final Class<?> pluginClass) {
        Class<? extends ApiServiceConfiguration> configurationClass = new ApiServiceConfigurationClassFinder().lookupFirst(pluginClass);

        return new DefaultApiServicePlugin(plugin, pluginClass, configurationClass);
    }

    @Override
    protected void register(ApiServicePlugin<?, ?> apiServicePlugin) {
        apiServicePluginManager.register(apiServicePlugin);

        // Once registered, the classloader should be released
        final ClassLoader connectorClassLoader = apiServicePlugin.connectorFactory().getClassLoader();

        if (connectorClassLoader instanceof URLClassLoader) {
            URLClassLoader classLoader = (URLClassLoader) connectorClassLoader;
            try {
                classLoader.close();
            } catch (IOException e) {
                logger.error("Unexpected exception while trying to release the api service plugin classloader", e);
            }
        }
    }

    @Override
    protected ClassLoader getClassLoader(Plugin plugin) {
        return new URLClassLoader(plugin.dependencies(), this.getClass().getClassLoader());
    }
}
