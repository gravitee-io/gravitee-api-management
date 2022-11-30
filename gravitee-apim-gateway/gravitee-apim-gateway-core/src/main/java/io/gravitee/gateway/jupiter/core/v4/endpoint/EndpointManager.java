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
package io.gravitee.gateway.jupiter.core.v4.endpoint;

import io.gravitee.common.component.LifecycleComponent;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface EndpointManager extends LifecycleComponent<EndpointManager> {
    /**
     * Get the next available endpoint for the default group.
     *
     * @return the endpoint found or <code>null</code> if no available endpoint has been found.
     */
    ManagedEndpoint next();

    /**
     * Get the next available endpoint matching with the provided criteria for the default group.
     * Criteria are applied both on group and endpoint in order to select the next available endpoint.
     *
     * @param criteria the criteria to apply on the endpoint.
     * @return the endpoint found or <code>null</code> if no endpoint has been found.
     */
    ManagedEndpoint next(EndpointCriteria criteria);
}
