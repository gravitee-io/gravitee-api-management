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
package io.gravitee.gateway.policy.impl.processor;

import io.gravitee.gateway.policy.PolicyContextProviderFactory;
import io.gravitee.gateway.policy.impl.PolicyContextProcessor;
import io.gravitee.gateway.policy.impl.processor.spring.SpringPolicyContextProviderFactory;
import io.gravitee.gateway.reactor.Reactable;
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
public class PolicyContextProviderProcessor implements PolicyContextProcessor {

    private final Logger LOGGER = LoggerFactory.getLogger(PolicyContextProviderProcessor.class);

    private static final List<PolicyContextProviderFactory> policyContextProviderFactories = new ArrayList<>();
    {
        policyContextProviderFactories.add(new SpringPolicyContextProviderFactory());
    }

    @Override
    public void process(PolicyContext policyContext, Reactable reactable) {
        for (PolicyContextProviderFactory providerFactory : policyContextProviderFactories) {
            if (providerFactory.canHandle(policyContext)) {
                LOGGER.debug("Initializing policy context provider with {}", providerFactory.getClass().getName());

                PolicyContextProvider contextProvider = providerFactory.create(policyContext);

                if (ClassUtils.isAssignableValue(PolicyContextProviderAware.class, policyContext)) {
                    ((PolicyContextProviderAware)policyContext).setPolicyContextProvider(contextProvider);
                }

                break;
            }
        }
    }
}
