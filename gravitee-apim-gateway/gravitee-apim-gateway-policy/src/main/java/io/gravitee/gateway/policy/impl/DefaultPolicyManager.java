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

import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.policy.PolicyFactory;
import io.gravitee.gateway.policy.impl.legacy.LegacyPolicyManager;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.policy.internal.PolicyMethodResolver;
import io.gravitee.policy.api.PolicyConfiguration;
import io.gravitee.policy.api.PolicyContext;
import io.gravitee.policy.api.PolicyContextProviderAware;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class DefaultPolicyManager extends LegacyPolicyManager {

    private final Logger logger = LoggerFactory.getLogger(DefaultPolicyManager.class);

    private final boolean legacyMode;

    public DefaultPolicyManager(
        final boolean legacyMode,
        final DefaultClassLoader classLoader,
        final PolicyFactory policyFactory,
        final PolicyConfigurationFactory policyConfigurationFactory,
        final ConfigurablePluginManager<PolicyPlugin<?>> policyPluginManager,
        final PolicyClassLoaderFactory policyClassLoaderFactory,
        final ResourceLifecycleManager resourceLifecycleManager,
        final ComponentProvider componentProvider
    ) {
        super(
            classLoader,
            policyFactory,
            policyConfigurationFactory,
            policyPluginManager,
            policyClassLoaderFactory,
            resourceLifecycleManager,
            componentProvider
        );
        this.legacyMode = legacyMode;
    }

    @Override
    protected void initialize() {
        if (legacyMode) {
            super.initialize();
        } else {
            dependencies()
                .forEach(
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

                        PolicyMetadataBuilder builder = new PolicyMetadataBuilder();
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

                            policies.put(policy.getName(), builder.build());
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
                    }
                );
        }
    }
}
