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
package io.gravitee.apim.core.event.query_service;

import io.gravitee.apim.core.event.model.Event;
import io.gravitee.rest.api.model.EventType;
import java.util.Optional;

/**
 * Port for querying the event_latest store, which holds exactly one
 * "current" event per entity (keyed by entity ID).
 */
public interface EventLatestQueryService {
    /**
     * Returns the latest event of the given type recorded for the specified entity.
     *
     * @param entityId     the entity ID (e.g. an API Product ID)
     * @param eventType    the event type to filter on
     * @param propertyKey  the event property that holds the entity ID
     * @return the single latest event, or empty if none has been recorded yet
     */
    Optional<Event> findLatestByEntityId(String entityId, EventType eventType, Event.EventProperties propertyKey);
}
