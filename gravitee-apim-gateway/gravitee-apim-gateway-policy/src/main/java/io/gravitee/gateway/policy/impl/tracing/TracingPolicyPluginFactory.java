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
package io.gravitee.gateway.policy.impl.tracing;

import io.gravitee.gateway.core.condition.ConditionEvaluator;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyManifest;
import io.gravitee.gateway.policy.PolicyPluginFactory;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.gateway.policy.impl.PolicyFactoryImpl;
import io.gravitee.policy.api.PolicyConfiguration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TracingPolicyPluginFactory extends PolicyFactoryImpl {

    public TracingPolicyPluginFactory(PolicyPluginFactory policyPluginFactory, ConditionEvaluator<String> conditionEvaluator) {
        super(policyPluginFactory, conditionEvaluator);
    }

    @Override
    public Policy create(StreamType streamType, PolicyManifest policyManifest, PolicyConfiguration policyConfiguration, String condition) {
        Policy policy = super.create(streamType, policyManifest, policyConfiguration, condition);

        return new TracingPolicy(policy);
    }
}
