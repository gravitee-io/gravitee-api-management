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
import io.gravitee.gateway.policy.PolicyClassDefinition;
import io.gravitee.gateway.policy.PolicyClassLoaderFactory;
import io.gravitee.gateway.policy.PolicyManager;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.plugin.policy.PolicyDefinition;
import io.gravitee.policy.api.PolicyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class DefaultPolicyManager extends AbstractLifecycleComponent<PolicyManager>
        implements PolicyManager {

    private final Logger LOGGER = LoggerFactory.getLogger(DefaultPolicyManager.class);

    private final ReactorHandler reactorHandler;

    @Autowired
    private io.gravitee.plugin.policy.PolicyManager policyManager;

    @Autowired
    private PolicyClassLoaderFactory policyClassLoaderFactory;

    private final Map<PolicyClassDefinition, ClassLoader> policiesClassloader = new HashMap<>();
    private final Map<String, PolicyClassDefinition> policiesDefinition = new HashMap<>();

    public DefaultPolicyManager(ReactorHandler reactorHandler) {
        this.reactorHandler = reactorHandler;
    }

    @Override
    protected void doStart() throws Exception {
        Set<Policy> requiredPlugins = reactorHandler.dependencies();

        requiredPlugins.forEach(policy -> {
            final PolicyDefinition policyDefinition = policyManager.getPolicyDefinition(policy.getName());
            if (policyDefinition == null) {
                LOGGER.error("Policy [{}] can no be found in registry", policy.getName());
                throw new IllegalStateException("Policy ["+policy.getName()+"] can no be found in registry");
            }

            ClassLoader policyClassLoader = policyClassLoaderFactory.createPolicyClassLoader(policyDefinition,
                    reactorHandler.classloader());

            LOGGER.debug("Loading a contextualized policy: {}", policyDefinition);

            DefaultPolicyClassDefinition policyClassDefinition = new DefaultPolicyClassDefinition(policyDefinition.id());

            try {
                policyClassDefinition.setPolicy(policyClassLoader.loadClass(policyDefinition.policy().getName()));
                if (policyDefinition.configuration() != null) {
                    policyClassDefinition.setConfiguration((Class<? extends PolicyConfiguration>)
                            policyClassLoader.loadClass(policyDefinition.configuration().getName()));
                }

                policyClassDefinition.setOnRequestMethod(getMethod(policyClassDefinition.policy(), policyDefinition.onRequestMethod()));
                policyClassDefinition.setOnResponseMethod(getMethod(policyClassDefinition.policy(), policyDefinition.onResponseMethod()));
                policyClassDefinition.setOnRequestContentMethod(getMethod(policyClassDefinition.policy(), policyDefinition.onRequestContentMethod()));
                policyClassDefinition.setOnResponseContentMethod(getMethod(policyClassDefinition.policy(), policyDefinition.onResponseContentMethod()));

                policiesClassloader.put(policyClassDefinition, policyClassLoader);
                policiesDefinition.put(policyClassDefinition.id(), policyClassDefinition);
            } catch (ClassNotFoundException cnfe) {
                LOGGER.error("Unable to load policy definition", cnfe);
            }
        });
    }

    private Method getMethod(Class<?> policy, Method method) {
        if (method == null) {
            return null;
        }

        try {
            return policy.getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException nsme) {
            LOGGER.error("Unable to find method", nsme);
            return null;
        }
    }

    @Override
    protected void doStop() throws Exception {
        policiesClassloader.entrySet().stream().forEach(policy -> {
            ClassLoader policyClassLoader = policy.getValue();
            if (policyClassLoader instanceof URLClassLoader) {
                try {
                    ((URLClassLoader)policyClassLoader).close();
                } catch (IOException e) {
                    LOGGER.error("Unable to close policy classloader for policy {}", policy.getKey().id());
                }
            }
        });

        // Be sure to remove all references to policy definitions and their respective classloaders
        policiesClassloader.clear();
        policiesDefinition.clear();
    }

    @Override
    public PolicyClassDefinition getPolicyClassDefinition(String policy) {
        return policiesDefinition.get(policy);
    }
}
