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

import io.gravitee.gateway.policy.impl.processor.PolicyContextProviderProcessor;
import io.gravitee.gateway.policy.impl.processor.PropertiesProcessor;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.policy.api.PolicyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class PolicyContextFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyContextFactory.class);

    private final Reactable reactable;

    private static final List<PolicyContextProcessor> policyContextProcessors = new ArrayList<>();
    {
        policyContextProcessors.add(new PolicyContextProviderProcessor());
        policyContextProcessors.add(new PropertiesProcessor());
    }

    public PolicyContextFactory(Reactable reactable) {
        this.reactable = reactable;
    }

    public PolicyContext create(Class<? extends PolicyContext> policyContextClass) throws Exception {
        if (policyContextClass == null) {
            return null;
        }

        try {
            LOGGER.debug("Creating a new instance of policy context of type {}", policyContextClass.getName());
            PolicyContext policyContext = policyContextClass.newInstance();

            for (PolicyContextProcessor processor : policyContextProcessors) {
                LOGGER.debug("Processing policy context instance with {}", processor.getClass().getName());
                processor.process(policyContext, reactable);
            }

            return policyContext;
        } catch (Exception ex) {
            LOGGER.error("Unable to create a policy context", ex);
            throw ex;
        }
    }
}
