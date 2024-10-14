/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactive.policy;

import io.gravitee.gateway.policy.PolicyManifest;
import java.util.Iterator;
import java.util.Set;

/**
 * A manager of Policy factories. It allows to select a particular policy factory depending on specific criteria tested against the {@link PolicyManifest}.
 * If no custom implementation of {@link PolicyFactory} has been found, then it will fallback to {@link HttpPolicyFactory} or a user defined implementation of {@link PolicyFactory}
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyFactoryManager {

    private final Class<? extends PolicyFactory> defaultPolicyFactoryClass;
    private final Set<PolicyFactory> policyFactories;
    private final PolicyFactory defaultPolicyFactory;

    /**
     * Build the manager with a set of {@link PolicyFactory} and select {@link HttpPolicyFactory} as fall back factory
     * @param policyFactories
     */
    public PolicyFactoryManager(Set<PolicyFactory> policyFactories) {
        this.policyFactories = policyFactories;
        defaultPolicyFactoryClass = HttpPolicyFactory.class;
        defaultPolicyFactory = extractDefaultPolicyFactory(policyFactories);
    }

    /**
     * Build the manager with a set of {@link PolicyFactory} and select {@param defaultPolicyFactoryClass} as fall back factory
     * @param defaultPolicyFactoryClass the class that will operate as the default policy factory
     * @param policyFactories
     */
    public PolicyFactoryManager(Class<? extends PolicyFactory> defaultPolicyFactoryClass, Set<PolicyFactory> policyFactories) {
        this.defaultPolicyFactoryClass = defaultPolicyFactoryClass;
        this.policyFactories = policyFactories;
        defaultPolicyFactory = extractDefaultPolicyFactory(policyFactories);
    }

    /**
     * Get the most appropriate {@link PolicyFactory} depending on the {@link PolicyManifest}
     * @param manifest to select the appropriate {@link PolicyFactory}
     * @return the {@link PolicyFactory}
     */
    public PolicyFactory get(PolicyManifest manifest) {
        return policyFactories.stream().filter(policyFactory -> policyFactory.accept(manifest)).findFirst().orElse(defaultPolicyFactory);
    }

    public void cleanup(PolicyManifest policyManifest) {
        get(policyManifest).cleanup(policyManifest);
    }

    private PolicyFactory extractDefaultPolicyFactory(Set<PolicyFactory> policyFactories) {
        final Iterator<PolicyFactory> iterator = policyFactories.iterator();
        PolicyFactory toExtract = null;
        while (iterator.hasNext()) {
            toExtract = iterator.next();
            if (defaultPolicyFactoryClass.equals(toExtract.getClass())) {
                iterator.remove();
                return toExtract;
            }
        }
        throw new IllegalStateException("Unable to find an instance of DefaultPolicyFactory");
    }
}
