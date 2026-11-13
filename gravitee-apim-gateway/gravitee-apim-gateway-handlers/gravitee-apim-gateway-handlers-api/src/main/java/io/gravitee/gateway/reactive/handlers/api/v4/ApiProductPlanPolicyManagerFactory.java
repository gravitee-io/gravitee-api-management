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
 * Kept for reactor plugins compiled against the per-API product plan policy manager, which no longer exists: product
 * plans now run from the product runtime, and the gateway never calls this factory.
 *
 * @deprecated build the reactor with the {@code ApiProductRuntimeRegistry} instead
 */
@Deprecated(forRemoval = true)
@FunctionalInterface
public interface ApiProductPlanPolicyManagerFactory {
    PolicyManager create(Api api);
}
