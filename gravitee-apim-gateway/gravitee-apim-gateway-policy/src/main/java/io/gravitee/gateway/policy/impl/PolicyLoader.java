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
package io.gravitee.gateway.policy.impl;

import io.gravitee.definition.model.Policy;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.policy.PolicyManifest;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.PluginClassLoader;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.policy.internal.PolicyMethodResolver;
import io.gravitee.policy.api.PolicyConfiguration;
import io.gravitee.policy.api.PolicyContext;
import io.gravitee.policy.api.PolicyContextProviderAware;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyLoader {

    private final Logger logger = LoggerFactory.getLogger(PolicyLoader.class);
    private final ConfigurablePluginManager<PolicyPlugin<? extends PolicyConfiguration>> policyPluginManager;
    private final PolicyClassLoaderFactory policyClassLoaderFactory;
    private final ComponentProvider componentProvider;
    private final DefaultClassLoader classLoader;

    public PolicyLoader(
        DefaultClassLoader classLoader,
        ConfigurablePluginManager<PolicyPlugin<?>> policyPluginManager,
        PolicyClassLoaderFactory policyClassLoaderFactory,
        ComponentProvider componentProvider
    ) {
        this.classLoader = classLoader;
        this.policyPluginManager = policyPluginManager;
        this.policyClassLoaderFactory = policyClassLoaderFactory;
        this.componentProvider = componentProvider;
    }

    public Map<String, PolicyManifest> load(final Set<Policy> dependencies) {
        return dependencies
            .stream()
            .map(
                policy -> {
                    final PolicyPlugin<?> policyPlugin = policyPluginManager.get(policy.getName());
                    if (policyPlugin == null) {
                        logger.error("Policy [{}] can not be found in policy registry", policy.getName());
                        throw new IllegalStateException("Policy [" + policy.getName() + "] can not be found in policy registry");
                    }

                    classLoader.addClassLoader(
                        policyPlugin.policy().getCanonicalName(),
                        () -> policyClassLoaderFactory.getOrCreateClassLoader(policyPlugin)
                    );

                    logger.debug("Loading policy {}", policy.getName());

                    PolicyManifestBuilder builder = new PolicyManifestBuilder();
                    builder.setId(policyPlugin.id());

                    try {
                        // Prepare metadata
                        Class<?> policyClass = ClassUtils.forName(policyPlugin.policy().getName(), classLoader);

                        builder
                            .setPolicy(policyClass)
                            .setClassLoader(classLoader)
                            .setMethods(new PolicyMethodResolver().resolve(policyClass));

                        if (policyPlugin.configuration() != null) {
                            builder.setConfiguration(
                                (Class<? extends PolicyConfiguration>) ClassUtils.forName(
                                    policyPlugin.configuration().getName(),
                                    classLoader
                                )
                            );
                        }

                        // Prepare context if defined
                        if (policyPlugin.context() != null) {
                            Class<? extends PolicyContext> policyContextClass = (Class<? extends PolicyContext>) ClassUtils.forName(
                                policyPlugin.context().getName(),
                                classLoader
                            );
                            // Create policy context instance and initialize context provider (if used)
                            PolicyContext context = new PolicyContextFactory().create(policyContextClass);

                            if (context instanceof PolicyContextProviderAware) {
                                ((PolicyContextProviderAware) context).setPolicyContextProvider(
                                        new DefaultPolicyContextProvider(componentProvider)
                                    );
                            }

                            builder.setContext(context);
                        }
                        return builder.build();
                    } catch (Exception ex) {
                        logger.error("Unable to load policy metadata", ex);
                        try {
                            classLoader.removeClassLoader(policyPlugin.policy().getCanonicalName());
                        } catch (IOException ioe) {
                            logger.error("Unable to close classloader for policy", ioe);
                        }
                    } catch (Error error) {
                        logger.error(
                            "Unable to load policy id[" +
                            policyPlugin.id() +
                            "]. This error mainly occurs when the policy is linked to a missing resource, for example a cache or an oauth2 resource. Please check your policy configuration!",
                            error
                        );
                        try {
                            classLoader.removeClassLoader(policyPlugin.policy().getCanonicalName());
                        } catch (IOException ioe) {
                            logger.error("Unable to close classloader for policy", ioe);
                        }
                    }
                    return null;
                }
            )
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(PolicyManifest::id, Function.identity()));
    }

    public void activatePolicyContext(final Map<String, PolicyManifest> policies) {
        // Activate policy context
        policies
            .values()
            .stream()
            .filter(registeredPolicy -> registeredPolicy.context() != null)
            .forEach(
                registeredPolicy -> {
                    try {
                        logger.info(
                            "Activating context for {} [{}]",
                            registeredPolicy.id(),
                            registeredPolicy.context().getClass().getName()
                        );

                        registeredPolicy.context().onActivation();
                    } catch (Exception ex) {
                        logger.error("Unable to activate policy context", ex);
                    }
                }
            );
    }

    public void disablePolicyContext(final Map<String, PolicyManifest> policies, final Consumer<PolicyManifest> cleanSupplier) {
        // Deactivate policy context
        policies
            .values()
            .stream()
            .filter(registeredPolicy -> registeredPolicy.context() != null)
            .forEach(
                registeredPolicy -> {
                    try {
                        logger.info(
                            "De-activating context for {} [{}]",
                            registeredPolicy.id(),
                            registeredPolicy.context().getClass().getName()
                        );
                        registeredPolicy.context().onDeactivation();
                    } catch (Exception ex) {
                        logger.error("Unable to deactivate policy context", ex);
                    }
                }
            );

        // Close policy classloaders
        policies
            .values()
            .forEach(
                policy -> {
                    // Cleanup everything possible in PolicyFactory.
                    cleanSupplier.accept(policy);

                    ClassLoader policyClassLoader = policy.classloader();
                    if (policyClassLoader instanceof PluginClassLoader) {
                        try {
                            ((PluginClassLoader) policyClassLoader).close();
                        } catch (IOException e) {
                            logger.error("Unable to close policy classloader for policy {}", policy.id());
                        }
                    }
                }
            );
    }
}
