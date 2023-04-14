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
package io.gravitee.gateway.jupiter.handlers.internal;

import io.gravitee.gateway.jupiter.reactor.v4.reactor.ReactorPlugin;
import io.gravitee.gateway.jupiter.reactor.v4.reactor.ReactorFactoryManager;
import io.gravitee.plugin.core.api.AbstractSimplePluginHandler;
import io.gravitee.plugin.core.api.Plugin;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URLClassLoader;

/**
 * @author GraviteeSource Team
 */
public class ReactorPluginHandler extends AbstractSimplePluginHandler<ReactorPlugin<?>> {

    @Autowired
    private ReactorFactoryManager reactorFactoryManager;

    @Override
    public boolean canHandle(final Plugin plugin) {
        return DefaultReactorPlugin.PLUGIN_TYPE.equalsIgnoreCase(plugin.type());
    }

    @Override
    protected String type() {
        return DefaultReactorPlugin.PLUGIN_TYPE;
    }

    @Override
    protected ReactorPlugin<?> create(final Plugin plugin, final Class<?> pluginClass) {
        return new DefaultReactorPlugin(plugin, pluginClass);
    }

    @Override
    protected void register(ReactorPlugin<?> reactorPlugin) {
        reactorFactoryManager.register(reactorPlugin);

        // Once registered, the classloader should be released
        final ClassLoader policyClassLoader = reactorPlugin.reactorFactory().getClassLoader();

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
