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
package io.gravitee.management.idp.core.plugin.impl;

import io.gravitee.management.idp.api.IdentityProvider;
import io.gravitee.management.idp.api.authentication.AuthenticationProvider;
import io.gravitee.management.idp.api.identity.IdentityLookup;
import io.gravitee.management.idp.core.authentication.impl.CompositeIdentityManager;
import io.gravitee.management.idp.core.plugin.IdentityProviderDefinition;
import io.gravitee.management.idp.core.plugin.IdentityProviderManager;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginContextFactory;
import io.gravitee.plugin.core.internal.AnnotationBasedPluginContextConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

import java.util.*;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class IdentityProviderManagerImpl implements IdentityProviderManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(IdentityProviderManagerImpl.class);

    private final Map<String, IdentityProvider> identityProviders = new HashMap<>();
    private final Map<IdentityProvider, Plugin> identityProviderPlugins = new HashMap<>();

    @Autowired
    private PluginContextFactory pluginContextFactory;

    @Autowired
    private CompositeIdentityManager compositeIdentityManager;

    @Override
    public void register(IdentityProviderDefinition identityProviderPluginDefinition) {
        identityProviders.putIfAbsent(identityProviderPluginDefinition.getIdentityProvider().type(),
                identityProviderPluginDefinition.getIdentityProvider());

        identityProviderPlugins.putIfAbsent(identityProviderPluginDefinition.getIdentityProvider(),
                identityProviderPluginDefinition.getPlugin());
    }

    @Override
    public Collection<IdentityProvider> getAll() {
        return identityProviders.values();
    }

    @Override
    public AuthenticationProvider loadIdentityProvider(String identityProvider, Map<String, Object> properties) {
        // By loading an identity provider we are mounting both authentication provider and identity lookup
        AuthenticationProvider authenticationProvider = authenticationProvider(identityProvider, properties);
        IdentityLookup identityLookup = identityLookup(identityProvider, properties);
        compositeIdentityManager.addIdentityLookup(identityLookup);

        return authenticationProvider;
    }

    private AuthenticationProvider authenticationProvider(String identityProviderType, Map<String, Object> properties) {
        LOGGER.debug("Looking for an authentication provider for [{}]", identityProviderType);
        IdentityProvider identityProvider = identityProviders.get(identityProviderType);

        if (identityProvider != null) {
            return create(
                    identityProviderPlugins.get(identityProvider),
                    identityProvider.authenticationProvider(),
                    properties);
        } else {
            LOGGER.error("No identity provider is registered for type {}", identityProviderType);
            throw new IllegalStateException("No identity provider is registered for type " + identityProviderType);
        }
    }

    private IdentityLookup identityLookup(String identityProviderType, Map<String, Object> properties) {
        LOGGER.debug("Looking for an identity lookup for [{}]", identityProviderType);
        IdentityProvider identityProvider = identityProviders.get(identityProviderType);

        if (identityProvider != null) {
            return create(
                    identityProviderPlugins.get(identityProvider),
                    identityProvider.identityLookup(),
                    properties);
        } else {
            LOGGER.error("No identity provider is registered for type {}", identityProviderType);
            throw new IllegalStateException("No identity provider is registered for type " + identityProviderType);
        }
    }

    private <T> T create(Plugin plugin, Class<T> identityClass, Map<String, Object> properties) {
        if (identityClass == null) {
            return null;
        }

        try {
            T identityObj = createInstance(identityClass);
            final Import annImport = identityClass.getAnnotation(Import.class);
            Set<Class<?>> configurations = (annImport != null) ?
                    new HashSet<>(Arrays.asList(annImport.value())) : Collections.emptySet();

            ApplicationContext idpApplicationContext = pluginContextFactory.create(new AnnotationBasedPluginContextConfigurer(plugin) {
                @Override
                public Set<Class<?>> configurations() {
                    return configurations;
                }

                @Override
                public ConfigurableEnvironment environment() {
                    return new StandardEnvironment() {
                        @Override
                        protected void customizePropertySources(MutablePropertySources propertySources) {
                            propertySources.addFirst(new MapPropertySource(plugin.id(), properties));
                            super.customizePropertySources(propertySources);
                        }
                    };
                }
            });

            idpApplicationContext.getAutowireCapableBeanFactory().autowireBean(identityObj);

            if (identityObj instanceof InitializingBean) {
                ((InitializingBean) identityObj).afterPropertiesSet();
            }

            return identityObj;
        } catch (Exception ex) {
            LOGGER.error("An unexpected error occurs while loading identity provider", ex);
            return null;
        }
    }

    private <T> T createInstance(Class<T> clazz) throws Exception {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            LOGGER.error("Unable to instantiate class: {}", ex);
            throw ex;
        }
    }
}
