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
package io.gravitee.rest.api.idp.core.plugin.impl;

import io.gravitee.common.util.RelaxedPropertySource;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginContextFactory;
import io.gravitee.plugin.core.internal.AnnotationBasedPluginContextConfigurer;
import io.gravitee.rest.api.idp.api.IdentityProvider;
import io.gravitee.rest.api.idp.api.authentication.AuthenticationProvider;
import io.gravitee.rest.api.idp.api.identity.IdentityLookup;
import io.gravitee.rest.api.idp.core.authentication.impl.CompositeIdentityManager;
import io.gravitee.rest.api.idp.core.plugin.IdentityProviderDefinition;
import io.gravitee.rest.api.idp.core.plugin.IdentityProviderManager;
import io.gravitee.secrets.api.core.Secret;
import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@Slf4j
public class IdentityProviderManagerImpl implements IdentityProviderManager {

    private final Map<String, IdentityProvider> identityProviders = new HashMap<>();
    private final Map<IdentityProvider, Plugin> identityProviderPlugins = new HashMap<>();

    @Autowired
    private PluginContextFactory pluginContextFactory;

    @Autowired
    private CompositeIdentityManager compositeIdentityManager;

    @Override
    public void register(IdentityProviderDefinition identityProviderPluginDefinition) {
        identityProviders.putIfAbsent(
            identityProviderPluginDefinition.getIdentityProvider().type(),
            identityProviderPluginDefinition.getIdentityProvider()
        );

        identityProviderPlugins.putIfAbsent(
            identityProviderPluginDefinition.getIdentityProvider(),
            identityProviderPluginDefinition.getPlugin()
        );
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
        log.debug("Looking for an authentication provider for [{}]", identityProviderType);
        IdentityProvider identityProvider = identityProviders.get(identityProviderType);

        if (identityProvider != null) {
            return create(identityProviderPlugins.get(identityProvider), identityProvider.authenticationProvider(), properties);
        } else {
            log.error("No identity provider is registered for type {}", identityProviderType);
            throw new IllegalStateException("No identity provider is registered for type " + identityProviderType);
        }
    }

    private IdentityLookup identityLookup(String identityProviderType, Map<String, Object> properties) {
        log.debug("Looking for an identity lookup for [{}]", identityProviderType);
        IdentityProvider identityProvider = identityProviders.get(identityProviderType);

        if (identityProvider != null) {
            return create(identityProviderPlugins.get(identityProvider), identityProvider.identityLookup(), properties);
        } else {
            log.error("No identity provider is registered for type {}", identityProviderType);
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
            Set<Class<?>> configurations = (annImport != null) ? new HashSet<>(Arrays.asList(annImport.value())) : Collections.emptySet();

            ApplicationContext idpApplicationContext = pluginContextFactory.create(
                new AnnotationBasedPluginContextConfigurer(plugin) {
                    @Override
                    public Set<Class<?>> configurations() {
                        return configurations;
                    }

                    @Override
                    public ConfigurableEnvironment environment() {
                        return new StandardEnvironment() {
                            @Override
                            protected void customizePropertySources(MutablePropertySources propertySources) {
                                propertySources.addFirst(new RelaxedPropertySource(plugin.id(), properties));
                                super.customizePropertySources(propertySources);

                                // add missing converters in this newly created environment
                                // this syntax allows a property of any kind to be converted from a secret. eg. Secret +> String -> Double
                                this.getConversionService()
                                    .addConverterFactory(
                                        new ConverterFactory<Secret, Object>() {
                                            final ConversionService conversionService = DefaultConversionService.getSharedInstance();

                                            @Nonnull
                                            public <C> Converter<Secret, C> getConverter(@Nonnull Class<C> targetType) {
                                                return source -> conversionService.convert(source.asString(), targetType);
                                            }
                                        }
                                    );
                                // byte[] has to be created separately
                                this.getConversionService().addConverter(Secret.class, String.class, Secret::asString);
                            }
                        };
                    }
                }
            );

            idpApplicationContext.getAutowireCapableBeanFactory().autowireBean(identityObj);

            if (identityObj instanceof InitializingBean) {
                ((InitializingBean) identityObj).afterPropertiesSet();
            }

            return identityObj;
        } catch (Exception ex) {
            log.error("An unexpected error occurs while loading identity provider", ex);
            return null;
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
