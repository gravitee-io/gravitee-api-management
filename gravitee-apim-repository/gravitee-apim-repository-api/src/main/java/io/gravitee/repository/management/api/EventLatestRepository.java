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
package io.gravitee.repository.management.api;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Event;
import java.util.List;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface EventLatestRepository {
    /**
     * Search for latest {@link Event} matching the corresponding criteria for each event related to the specified group criteria (ex: 'api_id, 'dictionary_id').
     *
     * @param criteria Event criteria to search for {@link Event}.
     *                 Only one event is stored so strictMode is ignored.
     * @param group the property to search for in order to retrieve the latest event. Can be {@link io.gravitee.repository.management.model.Event.EventProperties#API_ID} to retrieve latest event for each api
     *              or {@link io.gravitee.repository.management.model.Event.EventProperties#DICTIONARY_ID} to retrieve latest event for each dictionary.
     * @param page optional page number starting from 0, <code>null</code> means no paging.
     * @param size optional number of events to retrieve, <code>null</code> means no limit.
     *
     * @return the list of the latest events.
     */
    List<Event> search(EventCriteria criteria, Event.EventProperties group, Long page, Long size);

    /**
     * This method allows to create an event if it does not exist in database or update it if it's present (replace old values by new ones).
     *
     * @param event
     * @return the event passed as parameter.
     * @throws TechnicalException
     */
    Event createOrUpdate(Event event) throws TechnicalException;

    /**
     * This method allows to delete an event from its id
     *
     * @param eventId
     * @throws TechnicalException
     */
    void delete(String eventId) throws TechnicalException;

    /**
     * Find all events by environmentId
     * @param environmentId
     * @return List of events
     */
    List<Event> findByEnvironmentId(String environmentId);

    /**
     * Find all events by organizationId
     * @param organizationId
     * @return List of events
     */
    List<Event> findByOrganizationId(String organizationId);
}
