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

import static io.gravitee.plugin.gamma.internal.GammaModulePlugin.PLUGIN_TYPE;

import io.gravitee.apim.plugin.gamma.api.GammaModule;
import io.gravitee.plugin.core.api.AbstractPluginHandler;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginClassLoaderFactory;
import io.gravitee.plugin.core.api.PluginContextFactory;
import io.gravitee.plugin.core.internal.AnnotationBasedPluginContextConfigurer;
import io.gravitee.plugin.gamma.spring.GammaModuleConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Handler for Gamma modules. Discovers plugins of type {@code gamma-module},
 * creates a Spring ApplicationContext per plugin, and registers JAX-RS resource classes.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Import(GammaModuleConfiguration.class)
public class GammaModulePluginHandler extends AbstractPluginHandler {

    @Value("${gamma.enabled:false}")
    protected boolean gammaEnabled = false;

    @Autowired
    protected PluginClassLoaderFactory<Plugin> pluginClassLoaderFactory;

    @Autowired
    protected PluginContextFactory pluginContextFactory;

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    private GammaModuleManager gammaModuleManager;

    @Override
    protected ClassLoader getClassLoader(Plugin plugin) {
        return pluginClassLoaderFactory.getOrCreateClassLoader(
            plugin,
            applicationContext.getBean("managementMongoTemplate").getClass().getClassLoader()
        );
    }

    @Override
    public void handle(Plugin plugin) {
        if (!gammaEnabled) {
            logger.warn("Gamma is disabled. Plugin {} not loaded", plugin.id());
            return;
        }
        super.handle(plugin);
    }

    @Override
    protected void handle(Plugin plugin, Class<?> pluginClass) {
        try {
            GammaModule gammaModule = (GammaModule) pluginClass.getDeclaredConstructor().newInstance();
            Class<?> restResource = gammaModule.restResource();

            // Init Spring context so that any dependencies can be injected.
            initPluginSpringContext(plugin);
            gammaModuleManager.register(new GammaModulePlugin(plugin));

            if (restResource != null) {
                gammaModuleManager.registerResourceClass(plugin.id(), restResource);
            }
        } catch (Exception iae) {
            logger.error("Unexpected error while create repository instance", iae);
        }
    }

    @Override
    public boolean canHandle(Plugin plugin) {
        return PLUGIN_TYPE.equalsIgnoreCase(plugin.type());
    }

    @Override
    protected String type() {
        return PLUGIN_TYPE;
    }

    private void initPluginSpringContext(Plugin plugin) {
        try {
            ApplicationContext pluginCtx = pluginContextFactory.create(new GammaModulePluginContextConfigurer(plugin));

            // Register plugin beans into parent context so jersey-spring6 can resolve them via @Inject
            if (applicationContext instanceof GenericApplicationContext parentCtx) {
                for (String name : pluginCtx.getBeanDefinitionNames()) {
                    if (!parentCtx.containsBeanDefinition(name)) {
                        Object bean = pluginCtx.getBean(name);
                        String namespacedName = plugin.id() + "." + name;
                        parentCtx.getBeanFactory().registerSingleton(namespacedName, bean);
                        logger.debug("Registered bean '{}' from plugin '{}' into parent context", namespacedName, plugin.id());
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Failed to create plugin context for Gamma module '{}'", plugin.id(), ex);
            pluginContextFactory.remove(plugin);
        }
    }

    /**
     * Plugin context configurer for Gamma modules.
     * Skips registering the plugin class as a bean since it is a JAX-RS resource managed by Jersey, not Spring.
     *
     * @author GraviteeSource Team
     */
    public static class GammaModulePluginContextConfigurer extends AnnotationBasedPluginContextConfigurer {

        public GammaModulePluginContextConfigurer(Plugin plugin) {
            super(plugin);
        }

        @Override
        public void registerBeans() {
            // Skip — the plugin class is a JAX-RS resource managed by Jersey, not Spring
        }
    }
}
