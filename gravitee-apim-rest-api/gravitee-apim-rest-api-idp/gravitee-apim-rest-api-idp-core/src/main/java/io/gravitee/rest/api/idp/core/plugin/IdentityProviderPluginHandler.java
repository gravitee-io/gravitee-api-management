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
package io.gravitee.rest.api.idp.core.plugin;

import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginClassLoaderFactory;
import io.gravitee.plugin.core.api.PluginHandler;
import io.gravitee.rest.api.idp.api.IdentityProvider;
import io.gravitee.rest.api.idp.core.spring.IdentityProviderPluginConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.util.Assert;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Import(IdentityProviderPluginConfiguration.class)
public class IdentityProviderPluginHandler implements PluginHandler {

    private static final String PLUGIN_TYPE = "identity_provider";

    @Autowired
    private PluginClassLoaderFactory pluginClassLoaderFactory;

    @Autowired
    private IdentityProviderManager identityProviderManager;

    @Override
    public boolean canHandle(Plugin plugin) {
        return PLUGIN_TYPE.equalsIgnoreCase(plugin.type());
    }

    @Override
    public void handle(Plugin plugin) {
        try {
            ClassLoader classloader = pluginClassLoaderFactory.getOrCreateClassLoader(plugin, this.getClass().getClassLoader());

            final Class<?> identityProviderClass = classloader.loadClass(plugin.clazz());
            log.info("Register a new identity provider plugin: {} [{}]", plugin.id(), plugin.clazz());

            Assert.isAssignable(IdentityProvider.class, identityProviderClass);

            IdentityProvider identityIdentityProvider = createInstance((Class<IdentityProvider>) identityProviderClass);
            identityProviderManager.register(new IdentityProviderDefinition(identityIdentityProvider, plugin));
        } catch (Exception iae) {
            log.error("Unexpected error while create identity provider instance", iae);
        }
    }

    private <T> T createInstance(Class<T> clazz) throws Exception {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            log.error("Unable to instantiate class: {}", ex);
            throw ex;
        }
    }
}
