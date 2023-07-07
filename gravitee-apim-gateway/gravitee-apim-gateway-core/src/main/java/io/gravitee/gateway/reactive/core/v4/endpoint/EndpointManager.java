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
package io.gravitee.gateway.reactive.core.v4.endpoint;

import io.gravitee.common.component.LifecycleComponent;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface EndpointManager extends LifecycleComponent<EndpointManager> {
    /**
     * Add a new endpoint to the group or update it if an endpoint with the same name already exists.
     *
     * <p>
     *     Updating an endpoint leads to removing the existing endpoint and then adding the "new" endpoint with updated
     *     configuration.
     * </p>
     * @param groupName The group name where to add or update the endpoint
     * @param endpoint The endpoint to add or update
     */
    void addOrUpdateEndpoint(String groupName, Endpoint endpoint);

    /**
     * Remove an endpoint
     * @param name The endpoint name to remove
     */
    void removeEndpoint(String name);

    /**
     * Get the next available endpoint for the default group.
     *
     * @return the endpoint found or <code>null</code> if no available endpoint has been found.
     */
    ManagedEndpoint next();

    /**
     * Get all managed endpoints whatever the current status or secondary flag.
     * The return list is ready-only.
     *
     * @return all the managed endpoints.
     */
    List<ManagedEndpoint> all();

    String addListener(BiConsumer<Event, ManagedEndpoint> endpointConsumer);

    void removeListener(String listenerId);

    /**
     * Get the next available endpoint matching with the provided criteria for the default group.
     * Criteria are applied both on group and endpoint in order to select the next available endpoint.
     *
     * @param criteria the criteria to apply on the endpoint.
     * @return the endpoint found or <code>null</code> if no endpoint has been found.
     */
    ManagedEndpoint next(EndpointCriteria criteria);

    void disable(ManagedEndpoint endpoint);

    void enable(ManagedEndpoint endpoint);

    enum Event {
        ADD,
        REMOVE,
        ENABLE,
        DISABLE,
    }
}
