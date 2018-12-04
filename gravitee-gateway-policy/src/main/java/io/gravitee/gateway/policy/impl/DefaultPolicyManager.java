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

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.definition.model.Policy;
import io.gravitee.gateway.policy.*;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.PluginClassLoader;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.policy.internal.PolicyMethodResolver;
import io.gravitee.policy.api.PolicyConfiguration;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.api.annotations.OnResponse;
import io.gravitee.policy.api.annotations.OnResponseContent;
import io.gravitee.resource.api.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultPolicyManager extends AbstractLifecycleComponent<PolicyManager>
        implements PolicyManager {

    private final Logger logger = LoggerFactory.getLogger(DefaultPolicyManager.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private PolicyFactory policyFactory;

    @Autowired
    private PolicyConfigurationFactory policyConfigurationFactory;

    private final Map<String, RegisteredPolicy> policies = new HashMap<>();

    @Override
    protected void doStart() throws Exception {
        // Init required policies
        initialize();
    }

    @Override
    protected void doStop() throws Exception {
        // Close policy classloaders
        policies.values().forEach(policy -> {
            ClassLoader policyClassLoader = policy.classLoader;
            if (policyClassLoader instanceof PluginClassLoader) {
                try {
                    ((PluginClassLoader)policyClassLoader).close();
                } catch (IOException e) {
                    logger.error("Unable to close policy classloader for policy {}", policy.metadata.id());
                }
            }
        });

        // Be sure to remove all references to policies
        policies.clear();
    }

    private void initialize() {
        String[] beanNamesForType = applicationContext.getParent().getBeanNamesForType(
                ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, PolicyPlugin.class));

        ConfigurablePluginManager<PolicyPlugin> ppm = (ConfigurablePluginManager<PolicyPlugin>) applicationContext
                .getParent().getBean(beanNamesForType[0]);
        PolicyClassLoaderFactory pclf = applicationContext.getBean(PolicyClassLoaderFactory.class);
        ReactorHandler rh = applicationContext.getBean(ReactorHandler.class);
        ResourceLifecycleManager rm = applicationContext.getBean(ResourceLifecycleManager.class);
        Reactable reactable = rh.reactable();

        Set<Policy> requiredPlugins = reactable.dependencies(Policy.class);

        requiredPlugins.forEach(policy -> {
            final PolicyPlugin policyPlugin = ppm.get(policy.getName());
            if (policyPlugin == null) {
                logger.error("Policy [{}] can not be found in policy registry", policy.getName());
                throw new IllegalStateException("Policy ["+policy.getName()+"] can not be found in policy registry");
            }

            PluginClassLoader policyClassLoader = null;

            // Load dependant resources to enhance policy classloader
            Collection<? extends Resource> resources = rm.getResources();
            if (! resources.isEmpty()) {
                ClassLoader[] resourceClassLoaders = rm.getResources().stream().map(new Function<Resource, ClassLoader>() {
                    @Override
                    public ClassLoader apply(Resource resource) {
                        return resource.getClass().getClassLoader();
                    }
                }).toArray(ClassLoader[]::new);

                DelegatingClassLoader parentClassLoader = new DelegatingClassLoader(rh.classloader(), resourceClassLoaders);
                policyClassLoader = pclf.getOrCreateClassLoader(policyPlugin, parentClassLoader);
            } else {
                policyClassLoader = pclf.getOrCreateClassLoader(policyPlugin, rh.classloader());
            }

            logger.debug("Loading policy {} for {}", policy.getName(), rh);

            PolicyMetadataBuilder builder = new PolicyMetadataBuilder();
            builder.setId(policyPlugin.id());

            try {
                // Prepare metadata
                Class<?> policyClass = ClassUtils.forName(policyPlugin.policy().getName(), policyClassLoader);

                builder
                        .setPolicy(policyClass)
                        .setMethods(new PolicyMethodResolver().resolve(policyClass));

                if (policyPlugin.configuration() != null) {
                    builder.setConfiguration((Class<? extends PolicyConfiguration>) ClassUtils.forName(policyPlugin.configuration().getName(), policyClassLoader));
                }

                RegisteredPolicy registeredPolicy = new RegisteredPolicy();
                registeredPolicy.classLoader = policyClassLoader;
                registeredPolicy.metadata = builder.build();

                policies.put(policy.getName(), registeredPolicy);
            } catch (Exception ex) {
                logger.error("Unable to load policy metadata", ex);

                if (policyClassLoader != null) {
                    try {
                        policyClassLoader.close();
                    } catch (IOException ioe) {
                        logger.error("Unable to close classloader for policy", ioe);
                    }
                }
            }
        });
    }

    @Override
    public io.gravitee.gateway.policy.Policy create(StreamType streamType, String policy, String configuration) {
        RegisteredPolicy registeredPolicy = policies.get(policy);
        PolicyMetadata policyMetadata = (registeredPolicy != null) ? registeredPolicy.metadata : null;

        if ((streamType == StreamType.ON_REQUEST &&
                (policyMetadata.method(OnRequest.class) != null || policyMetadata.method(OnRequestContent.class) != null)) ||
                (streamType == StreamType.ON_RESPONSE && (
                        policyMetadata.method(OnResponse.class) != null || policyMetadata.method(OnResponseContent.class) != null))) {
            PolicyConfiguration policyConfiguration = policyConfigurationFactory.create(
                    policyMetadata.configuration(), configuration);

            Object policyInst = policyFactory.create(policyMetadata, policyConfiguration);

            logger.debug("Policy {} has been added to the policy chain", policyMetadata.id());
            return PolicyImpl
                    .target(policyInst)
                    .definition(policyMetadata)
                    .build();
        }

        return null;
    }

    private static class RegisteredPolicy {
        PolicyMetadata metadata;
        ClassLoader classLoader;
    }
}
