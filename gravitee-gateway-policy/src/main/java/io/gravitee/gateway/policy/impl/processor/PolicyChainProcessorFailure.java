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

import io.gravitee.gateway.core.processor.ProcessorFailure;
import io.gravitee.policy.api.PolicyResult;

import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyChainProcessorFailure implements ProcessorFailure {

    private final PolicyResult policyResult;

    public PolicyChainProcessorFailure(final PolicyResult policyResult) {
        this.policyResult = policyResult;
    }

    @Override
    public int statusCode() {
        return policyResult.statusCode();
    }

    @Override
    public String message() {
        return policyResult.message();
    }

    @Override
    public String key() {
        return policyResult.key();
    }

    @Override
    public Map<String, Object> parameters() {
        return policyResult.parameters();
    }
}
