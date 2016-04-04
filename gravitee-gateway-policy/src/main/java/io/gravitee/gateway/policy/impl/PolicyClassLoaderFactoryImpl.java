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

import io.gravitee.gateway.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLClassLoader;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class PolicyClassLoaderFactoryImpl implements PolicyClassLoaderFactory {

    protected final Logger LOGGER = LoggerFactory.getLogger(PolicyClassLoaderFactoryImpl.class);

    @Override
    public ClassLoader createPolicyClassLoader(PolicyDefinition policyDefinition, ClassLoader parent) {
        try {
            URLClassLoader cl = new URLClassLoader(policyDefinition.plugin().dependencies(), parent);

            LOGGER.debug("Created plugin classloader for {} with classpath {}",
                    policyDefinition.id(), policyDefinition.plugin().dependencies());

            return cl;
        }
        catch (Throwable t) {
            LOGGER.error("Unexpected error while creating plugin classloader", t);
            return null;
        }
    }
}