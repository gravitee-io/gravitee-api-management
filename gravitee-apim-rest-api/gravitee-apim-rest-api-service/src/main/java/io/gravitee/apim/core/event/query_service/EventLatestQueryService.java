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
import java.util.List;
import java.util.Set;

/**
 * Port for querying the event_latest store, which holds exactly one
 * "current" event per entity (keyed by entity ID).
 */
public interface EventLatestQueryService {
    /**
     * The latest event of a type for each of several entities, in one query.
     *
     * <p>The entity ids are pushed down to the store rather than filtered afterwards, so a caller
     * resolving a page reads that page's rows and not the whole environment's. Entities with no such
     * event are simply absent from the result.</p>
     *
     * @param entityIds   the entity ids (e.g. API Product ids)
     * @param eventType   the event type to look for
     * @param propertyKey the property the event carries the entity id under
     */
    List<Event> findLatestByEntityIds(Set<String> entityIds, EventType eventType, Event.EventProperties propertyKey);

    List<Event> findAllByTypeAndEnvironments(Set<EventType> eventTypes, Set<String> environments, Event.EventProperties groupBy);
}
