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
package io.gravitee.gateway.reactive.handlers.api.v4;

import io.gravitee.gateway.reactive.policy.PolicyManager;

/**
 * Factory for creating ApiProductPlanPolicyManager instances.
 * Used by DefaultApiReactor to create fresh policy managers when API Products change.
 *
 * @author GraviteeSource Team
 */
@FunctionalInterface
public interface ApiProductPlanPolicyManagerFactory {
    /**
     * Create a new ApiProductPlanPolicyManager for the given API.
     *
     * @param api the API (must have getId(), getEnvironmentId())
     * @return new PolicyManager instance, or null if not applicable
     */
    PolicyManager create(Api api);
}
