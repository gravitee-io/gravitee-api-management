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
import io.gravitee.plugin.console.spring.ConsoleExtensionConfiguration;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginHandler;
import java.net.URLClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Handler for console extensions. Unlike other plugin handlers, this one does NOT
 * extend AbstractPluginHandler because console extensions do not require class loading.
 * They only carry static UI assets (JS, CSS, manifest).
 *
 * @author GraviteeSource Team
 */
@Import(ConsoleExtensionConfiguration.class)
public class ConsoleExtensionHandler implements PluginHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleExtensionHandler.class);

    @Autowired
    private ConsoleExtensionManager consoleExtensionManager;

    @Override
    public boolean canHandle(Plugin plugin) {
        return ConsoleExtension.PLUGIN_TYPE.equalsIgnoreCase(plugin.type());
    }

    @Override
    public void handle(Plugin plugin) {
        logger.info("Install console extension: {} [{}]", plugin.id(), plugin.manifest().version());

        String className = plugin.clazz();
        if (className != null && plugin.dependencies() != null && plugin.dependencies().length > 0) {
            try {
                URLClassLoader classLoader = new URLClassLoader(plugin.dependencies(), getClass().getClassLoader());
                Class<?> pluginClass = classLoader.loadClass(className);
                if (ConsoleExtensionServletFactory.class.isAssignableFrom(pluginClass)) {
                    ConsoleExtensionServletFactory factory =
                        (ConsoleExtensionServletFactory) pluginClass.getDeclaredConstructor().newInstance();
                    consoleExtensionManager.registerServletFactory(plugin.id(), factory);
                } else {
                    consoleExtensionManager.registerResourceClass(plugin.id(), pluginClass);
                }
            } catch (Exception e) {
                logger.warn("Failed to load class for console extension '{}'", plugin.id(), e);
            }
        }

        consoleExtensionManager.register(new DefaultConsoleExtension(plugin));
        logger.info("Console extension '{}' installed.", plugin.id());
    }
}
