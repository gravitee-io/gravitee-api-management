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

    public PolicyFactoryImpl(final PolicyPluginFactory policyPluginFactory) {
        this.policyPluginFactory = policyPluginFactory;
    }

    @Override
    public void cleanup(PolicyMetadata policyMetadata) {
        policyPluginFactory.cleanup(policyMetadata);
    }

    @Override
    public Policy create(StreamType streamType, PolicyMetadata policyMetadata, PolicyConfiguration policyConfiguration, String condition) {
        Object policy = policyPluginFactory.create(policyMetadata.policy(), policyConfiguration);

        Method headMethod, streamMethod;
        if (streamType == StreamType.ON_REQUEST) {
            headMethod = policyMetadata.method(OnRequest.class);
            streamMethod = policyMetadata.method(OnRequestContent.class);
        } else {
            headMethod = policyMetadata.method(OnResponse.class);
            streamMethod = policyMetadata.method(OnResponseContent.class);
        }

        if (condition != null) {
            return new ConditionalExecutablePolicy(policyMetadata.id(), policy, headMethod, streamMethod, condition);
        }
        return new ExecutablePolicy(policyMetadata.id(), policy, headMethod, streamMethod);
    }
}
