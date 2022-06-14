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

import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.gateway.core.condition.ConditionEvaluator;
import io.gravitee.gateway.policy.*;
import io.gravitee.policy.api.PolicyConfiguration;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.api.annotations.OnResponse;
import io.gravitee.policy.api.annotations.OnResponseContent;
import java.lang.reflect.Method;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyFactoryImpl implements PolicyFactory {

    private final PolicyPluginFactory policyPluginFactory;
    private final ConditionEvaluator<String> conditionEvaluator;

    public PolicyFactoryImpl(final PolicyPluginFactory policyPluginFactory) {
        this(policyPluginFactory, null);
    }

    public PolicyFactoryImpl(final PolicyPluginFactory policyPluginFactory, final ConditionEvaluator<String> conditionEvaluator) {
        this.policyPluginFactory = policyPluginFactory;
        this.conditionEvaluator = conditionEvaluator;
    }

    @Override
    public void cleanup(PolicyManifest policyManifest) {
        policyPluginFactory.cleanup(policyManifest);
    }

    @Override
    public Policy create(
        StreamType streamType,
        PolicyManifest policyManifest,
        PolicyConfiguration policyConfiguration,
        PolicyMetadata policyMetadata
    ) {
        Object policy = policyPluginFactory.create(policyManifest.policy(), policyConfiguration);

        Method headMethod, streamMethod;
        if (streamType == StreamType.ON_REQUEST) {
            headMethod = policyManifest.method(OnRequest.class);
            streamMethod = policyManifest.method(OnRequestContent.class);
        } else {
            headMethod = policyManifest.method(OnResponse.class);
            streamMethod = policyManifest.method(OnResponseContent.class);
        }

        final ExecutablePolicy executablePolicy = new ExecutablePolicy(policyManifest.id(), policy, headMethod, streamMethod);

        if (
            conditionEvaluator != null && ExecutionMode.JUPITER != policyMetadata.metadata().get(PolicyMetadata.MetadataKeys.EXECUTION_MODE)
        ) {
            // Conditional policy are directly managed by Jupiter. ConditionalExecutablePolicy must only be instantiated for v3 execution.
            final String condition = policyMetadata.getCondition();
            if (condition != null && !condition.isBlank()) {
                return new ConditionalExecutablePolicy(executablePolicy, condition, conditionEvaluator);
            }
        }
        return executablePolicy;
    }
}
