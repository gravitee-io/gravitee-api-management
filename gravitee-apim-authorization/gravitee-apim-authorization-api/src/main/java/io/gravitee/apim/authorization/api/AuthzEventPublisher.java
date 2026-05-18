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
package io.gravitee.apim.authorization.api;

import io.gravitee.apim.authorization.domain.Entity;
import io.gravitee.apim.authorization.domain.Policy;

public interface AuthzEventPublisher {
    void publishPolicyDeployed(Policy policy);

    /**
     * Emits an UNPUBLISH event with full policy context so the gateway-side
     * mapper can route the undeploy to the right registry bucket without
     * falling back to a hard-coded {@code GLOBAL} kind.
     */
    void unpublishPolicy(Policy policy);

    void publishEntityUpserted(Entity entity);

    /**
     * Emits an UNPUBLISH event with full entity context (incl. {@code kind})
     * so the gateway-side mapper removes the entity from the right registry
     * bucket without hard-coding {@code RESOURCE}.
     */
    void unpublishEntity(Entity entity);
}
