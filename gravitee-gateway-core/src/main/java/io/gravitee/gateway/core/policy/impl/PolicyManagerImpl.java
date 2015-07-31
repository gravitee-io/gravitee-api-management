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
import io.gravitee.gateway.core.plugin.Plugin;
import io.gravitee.gateway.core.plugin.PluginContextFactory;
import io.gravitee.gateway.core.plugin.PluginHandler;
import io.gravitee.gateway.core.policy.PolicyDefinition;
import io.gravitee.gateway.core.policy.PolicyManager;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withModifier;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyManagerImpl implements PolicyManager, PluginHandler {

    protected final Logger LOGGER = LoggerFactory.getLogger(PolicyManagerImpl.class);

    @Autowired
    private PluginContextFactory pluginContextFactory;

    private final Map<String, PolicyDefinition> definitions = new HashMap<>();
    private final Map<String, ApplicationContext> contexts = new HashMap<>();

    @Override
    public boolean canHandle(Plugin plugin) {
        final Class<?> instanceClass = plugin.clazz();

        final Method onRequestMethod = resolvePolicyMethod(instanceClass, OnRequest.class);
        final Method onResponseMethod = resolvePolicyMethod(instanceClass, OnResponse.class);

        return (onRequestMethod != null || onResponseMethod != null);
    }

    @Override
    public void handle(Plugin plugin) {
        try {
            final Class<?> policyClass = plugin.clazz();
            ApplicationContext context = pluginContextFactory.create(plugin);
            contexts.putIfAbsent(plugin.id(), context);
            final Method onRequestMethod = resolvePolicyMethod(policyClass, OnRequest.class);
            final Method onResponseMethod = resolvePolicyMethod(policyClass, OnResponse.class);

            if (onRequestMethod == null && onResponseMethod == null) {
                LOGGER.error("No method annotated with @OnRequest or @OnResponse found, skip policy registration for {}", policyClass.getName());
            } else {
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
                        return null;
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
            // Be sure that the context does not exist anymore.
            ApplicationContext ctx =  contexts.remove(plugin.id());
            if (ctx != null) {
                ((ConfigurableApplicationContext)ctx).close();
            }
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

    @Override
    public ApplicationContext getApplicationContext(String id) {
        return null;
    }

    private Method resolvePolicyMethod(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        Set<Method> methods = ReflectionUtils.getMethods(
                clazz,
                withModifier(Modifier.PUBLIC),
                withAnnotation(annotationClass));

        if (methods.isEmpty()) {
            return null;
        }

        return methods.iterator().next();
    }
}
