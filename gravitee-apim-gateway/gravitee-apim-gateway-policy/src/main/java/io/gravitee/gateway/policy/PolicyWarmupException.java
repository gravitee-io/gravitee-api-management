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
package io.gravitee.gateway.policy;

/**
 * Thrown when a {@link io.gravitee.gateway.reactive.api.policy.http.WarmablePolicy} fails to warm up
 * during API deployment. This exception fails the API deployment (fail-fast) so that the API does not
 * start with a cold or broken policy configuration.
 *
 * @author GraviteeSource Team
 */
public class PolicyWarmupException extends RuntimeException {

    private final String policyId;

    public PolicyWarmupException(final String policyId, final String message, final Throwable cause) {
        super(message, cause);
        this.policyId = policyId;
    }

    public String getPolicyId() {
        return policyId;
    }
}
