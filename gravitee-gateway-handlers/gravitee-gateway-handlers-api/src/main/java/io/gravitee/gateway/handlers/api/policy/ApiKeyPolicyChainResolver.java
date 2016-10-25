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
package io.gravitee.gateway.handlers.api.policy;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.StreamType;

import java.util.Collections;
import java.util.List;

/**
 * A policy resolver used to create an api-key policy chain.
 * This chain is only executed for request stream.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeyPolicyChainResolver extends AbstractPolicyChainResolver {

    private final static String API_KEY_POLICY = "api-key";
    private final static String API_KEY_POLICY_CONFIGURATION = "{}";

    @Override
    protected List<Policy> calculate(StreamType streamType, Request request, Response response, ExecutionContext executionContext) {
        // Apply api-key policy only for ON_REQUEST
        if (streamType == StreamType.ON_REQUEST) {
            Policy apiKeyPolicy = create(streamType, API_KEY_POLICY, API_KEY_POLICY_CONFIGURATION);
            return Collections.singletonList(apiKeyPolicy);
        }

        return Collections.emptyList();
    }
}
