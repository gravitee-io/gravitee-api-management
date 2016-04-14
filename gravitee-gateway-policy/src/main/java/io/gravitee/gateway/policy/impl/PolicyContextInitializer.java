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

import io.gravitee.gateway.policy.PolicyContextProviderFactory;
import io.gravitee.gateway.policy.impl.spring.SpringPolicyContextProviderFactory;
import io.gravitee.policy.api.PolicyContext;
import io.gravitee.policy.api.PolicyContextProvider;
import io.gravitee.policy.api.PolicyContextProviderAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class PolicyContextInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyContextInitializer.class);

    private final List<PolicyContextProviderFactory> policyContextProviderFactories = new ArrayList<>();
    {
        policyContextProviderFactories.add(new SpringPolicyContextProviderFactory());
    }

    public PolicyContext create(Class<? extends PolicyContext> policyContextClass) throws Exception {
        if (policyContextClass == null) {
            return null;
        }

        try {
            LOGGER.debug("Creating a new instance of policy context of type {}", policyContextClass.getName());
            PolicyContext policyContext = policyContextClass.newInstance();

            boolean init = false;

            for (PolicyContextProviderFactory providerFactory : policyContextProviderFactories) {
                if (providerFactory.canHandle(policyContext)) {
                    LOGGER.debug("Initializing policy context provider with {}", providerFactory.getClass().getName());

                    PolicyContextProvider contextProvider = providerFactory.create(policyContext);

                    if (ClassUtils.isAssignableValue(PolicyContextProviderAware.class, policyContext)) {
                        ((PolicyContextProviderAware)policyContext).setPolicyContextProvider(contextProvider);
                    }

                    init = true;
                    break;
                }
            }

            if (! init) {
                LOGGER.warn("No policy context provider for policy context {} has been found",
                        policyContextClass.getName());
            }

            return policyContext;
        } catch (Exception ex) {
            LOGGER.error("Unable to create a policy context", ex);
            throw ex;
        }
    }
}
