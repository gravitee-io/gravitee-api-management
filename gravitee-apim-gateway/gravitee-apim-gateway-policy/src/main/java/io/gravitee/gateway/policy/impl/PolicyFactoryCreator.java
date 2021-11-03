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

import io.gravitee.gateway.policy.PolicyFactory;
import io.gravitee.gateway.policy.PolicyPluginFactory;
import io.gravitee.gateway.policy.impl.tracing.TracingPolicyPluginFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyFactoryCreator implements FactoryBean<PolicyFactory> {

    private final Logger logger = LoggerFactory.getLogger(PolicyFactoryCreator.class);

    private final Environment environment;
    private final PolicyPluginFactory policyPluginFactory;

    public PolicyFactoryCreator(final Environment environment, final PolicyPluginFactory policyPluginFactory) {
        this.environment = environment;
        this.policyPluginFactory = policyPluginFactory;
    }

    @Override
    public PolicyFactory getObject() {
        boolean tracing = environment.getProperty("services.tracing.enabled", Boolean.class, false);

        if (tracing) {
            logger.debug("Tracing is enabled, looking to decorate all policies...");
        }

        final PolicyFactory policyFactory = tracing
            ? new TracingPolicyPluginFactory(policyPluginFactory)
            : new PolicyFactoryImpl(policyPluginFactory);
        return new CachedPolicyFactory(policyFactory);
    }

    @Override
    public Class<?> getObjectType() {
        return PolicyFactory.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
