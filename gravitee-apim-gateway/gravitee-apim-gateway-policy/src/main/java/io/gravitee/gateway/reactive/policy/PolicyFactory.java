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
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.policy.base.BasePolicy;
import io.gravitee.policy.api.PolicyConfiguration;

/**
 * A factory to create an instance of {@link BasePolicy} with its {@link PolicyConfiguration}.
 * This factory is called during request processing while creating the {@link io.gravitee.policy.api.PolicyChain}
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PolicyFactory {
    /**
     * Verify if a policy can be built by the current implementation of PolicyFactory
     * @return true if the factory can build a policy depending on its manifest
     */
    boolean accept(PolicyManifest policyManifest);

    <P extends BasePolicy> P create(
        final ExecutionPhase phase,
        final PolicyManifest policyManifest,
        final PolicyConfiguration policyConfiguration,
        final PolicyMetadata policyMetadata
    );

    void cleanup(final PolicyManifest policyManifest);
}
