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
package io.gravitee.gateway.handlers.sharedpolicygroup.policy;

import io.gravitee.gateway.policy.PolicyManifest;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.policy.PolicyPluginFactory;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.gateway.reactive.core.condition.ExpressionLanguageConditionFilter;
import io.gravitee.gateway.reactive.policy.ConditionalPolicy;
import io.gravitee.gateway.reactive.policy.DefaultPolicyFactory;
import io.gravitee.policy.api.PolicyConfiguration;

public class SharedPolicyGroupPolicyFactory extends DefaultPolicyFactory {

    public SharedPolicyGroupPolicyFactory(
        PolicyPluginFactory policyPluginFactory,
        ExpressionLanguageConditionFilter<ConditionalPolicy> filter
    ) {
        super(policyPluginFactory, filter);
    }

    @Override
    public boolean accept(PolicyManifest policyManifest) {
        return SharedPolicyGroupPolicy.POLICY_ID.equals(policyManifest.id());
    }

    @Override
    protected Policy createPolicy(
        ExecutionPhase phase,
        PolicyManifest policyManifest,
        PolicyConfiguration policyConfiguration,
        PolicyMetadata policyMetadata
    ) {
        Policy policy = new SharedPolicyGroupPolicy(policyMetadata.getName(), (SharedPolicyGroupPolicyConfiguration) policyConfiguration);
        policy = createConditionalPolicy(policyMetadata, policy);
        return policy;
    }
}
