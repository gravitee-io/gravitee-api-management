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
package io.gravitee.gateway.core.policy.impl;

import io.gravitee.gateway.api.policy.PolicyConfiguration;
import io.gravitee.gateway.api.policy.annotations.OnRequest;
import io.gravitee.gateway.api.policy.annotations.OnResponse;
import io.gravitee.gateway.core.plugin.PluginContextFactory;
import io.gravitee.gateway.core.plugin.PluginHandler;
import io.gravitee.gateway.core.policy.PolicyDefinition;
import io.gravitee.gateway.core.policy.PolicyManager;
import io.gravitee.gateway.core.policy.PolicyMethodResolver;
import io.gravitee.plugin.api.Plugin;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyManagerImpl implements PolicyManager, PluginHandler {

    protected final Logger LOGGER = LoggerFactory.getLogger(PolicyManagerImpl.class);

    @Autowired
    private PluginContextFactory pluginContextFactory;

    @Autowired
    private PolicyMethodResolver policyMethodResolver;

    private final Map<String, PolicyDefinition> definitions = new HashMap<>();

    @Override
    public boolean canHandle(Plugin plugin) {
        return ! (policyMethodResolver.resolvePolicyMethods(plugin.clazz()).isEmpty());
    }

    @Override
    public void handle(Plugin plugin) {
        try {
            final Class<?> policyClass = plugin.clazz();
            pluginContextFactory.create(plugin);

            Map<Class<? extends Annotation>, Method> methods = policyMethodResolver.resolvePolicyMethods(plugin.clazz());

            final Method onRequestMethod = methods.get(OnRequest.class);
            final Method onResponseMethod = methods.get(OnResponse.class);

            if (onRequestMethod == null && onResponseMethod == null) {
                LOGGER.error("No method annotated with @OnRequest or @OnResponse found, skip policy registration for {}", policyClass.getName());
            } else {
                LOGGER.info("Looking for a policy configuration class in package {}", plugin.clazz().getPackage().getName());

                Reflections reflections = new Reflections(new ConfigurationBuilder()
                        .addClassLoader(plugin.clazz().getClassLoader())
                        .setUrls(ClasspathHelper.forClass(plugin.clazz(), plugin.clazz().getClassLoader()))
                        .setScanners(new SubTypesScanner(false), new TypeAnnotationsScanner())
                        .filterInputsBy(new FilterBuilder().includePackage(plugin.clazz().getPackage().getName())));


                Set<Class<? extends PolicyConfiguration>> policyConfigurations =
                        reflections.getSubTypesOf(PolicyConfiguration.class);

                Class<PolicyConfiguration> policyConfiguration = null;

                if (policyConfigurations.isEmpty()) {
                    LOGGER.info("\tNo policy configuration class defined for policy {}", plugin.id());
                } else {
                    policyConfiguration = (Class<PolicyConfiguration>) policyConfigurations.iterator().next();
                    LOGGER.info("\tPolicy configuration class found for plugin {} : {}", plugin.id(), policyConfiguration.getName());
                }

                final Class<PolicyConfiguration> finalPolicyConfiguration = policyConfiguration;

                PolicyDefinition definition = new PolicyDefinition() {
                    @Override
                    public String id() {
                        return plugin.id();
                    }

                    @Override
                    public Class<?> policy() {
                        return policyClass;
                    }

                    @Override
                    public Class<PolicyConfiguration> configuration() {
                        return finalPolicyConfiguration;
                    }

                    @Override
                    public Method onRequestMethod() {
                        return onRequestMethod;
                    }

                    @Override
                    public Method onResponseMethod() {
                        return onResponseMethod;
                    }
                };

                definitions.putIfAbsent(plugin.id(), definition);
            }
        } catch (Exception iae) {
            LOGGER.error("Unexpected error while create reporter instance", iae);

            pluginContextFactory.remove(plugin);
        }
    }

    @Override
    public Collection<PolicyDefinition> getPolicyDefinitions() {
        return definitions.values();
    }

    @Override
    public PolicyDefinition getPolicyDefinition(String id) {
        return definitions.get(id);
    }

    public void setPolicyMethodResolver(PolicyMethodResolver policyMethodResolver) {
        this.policyMethodResolver = policyMethodResolver;
    }

    public void setPluginContextFactory(PluginContextFactory pluginContextFactory) {
        this.pluginContextFactory = pluginContextFactory;
    }
}
