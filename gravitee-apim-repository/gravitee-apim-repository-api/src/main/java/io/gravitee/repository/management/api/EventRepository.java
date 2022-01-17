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
package io.gravitee.repository.management.api;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Event;
import java.util.List;

/**
 * Event API.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface EventRepository extends CrudRepository<Event, String> {
    /**
     * Search for latest {@link Event} matching the corresponding criteria for each event related to the specified group criteria (ex: 'api_id, 'dictionary_id').
     *
     * @param criteria the criteria to apply.
     *                 When strictMode in criteria is enabled, the last event of the entity must be one of the types specified to be returned.
     *                 Otherwise, when strictMode is disabled, events will be first filtered by type before finding the latest.
     *                 Default strictMode is false.
     *                 Example:
     *                 Let assume we have an API with these events in this order: START, PUBLISH and UNPUBLISH.
     *                 If strictMode == true and we search with types START & PUBLISH, then no event will be returned. Indeed, the current latest is UNPUBLISH and not in the search list.
     *                 If strictMode == false and we search with types START & PUBLISH, then the PUBLISH event will be returned.
     * @param group the property to group on in order to retrieve the latest event. Can be {@link io.gravitee.repository.management.model.Event.EventProperties#API_ID} to retrieve latest event for each api
     *              or {@link io.gravitee.repository.management.model.Event.EventProperties#DICTIONARY_ID} to retrieve latest event for each dictionary.
     * @param page optional page number starting from 0, <code>null</code> means no paging.
     * @param size optional number of events to retrieve, <code>null</code> means no limit.
     *
     * @returnthe list of latest events.
     */
    List<Event> searchLatest(EventCriteria criteria, Event.EventProperties group, Long page, Long size);

    /**
     * Search for {@link Event} with {@link Pageable} feature.
     *
     * <p>
     *  Note that events must be ordered by update date in DESC mode.
     * </p>
     *
     * @param filter Event criteria to search for {@link Event}.
     * @param pageable If user wants a paginable result. Can be <code>null</code>.
     * @return
     */
    Page<Event> search(EventCriteria filter, Pageable pageable);

    /**
     * Search for {@link Event}.
     *
     * <p>
     *  Note that events must be ordered by update date in DESC mode.
     * </p>
     * @param filter Event criteria to search for {@link Event}.
     * @return
     */
    List<Event> search(EventCriteria filter);
}
