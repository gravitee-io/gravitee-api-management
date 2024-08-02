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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
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
     * Search for {@link Event} with {@link Pageable} feature.
     *
     * <p>
     *  Note that events must be ordered by update date in DESC mode.
     * </p>
     *
     * @param filter Event criteria to search for {@link Event}.
     * @param pageable If user wants a paginable result. Can be <code>null</code>.
     * @return the list of events.
     */
    Page<Event> search(EventCriteria filter, Pageable pageable);
    /**
     * Search for {@link Event}.
     *
     * <p>
     *  Note that events must be ordered by update date in DESC mode.
     * </p>
     * @param filter Event criteria to search for {@link Event}.
     * @return the list of events.
     */
    List<Event> search(EventCriteria filter);

    /**
     * This method allows to create an event if it does not exist in database or patch it if it's present.
     *
     * <p>
     * The patch will do a partial update: <br/>
     * - Root fields of {@link Event} object will be updated only if non-null, i.e it won't removed or update to <code>null</code> any of <code>null</code> fields<br/>
     * - Updated properties map will be used to update existing property or added based on its key, i.e it won't removed or update to <code>null</code> any of <code>null</code> non existing properties.<br/>
     * - Old environments will be removed and replaced only if the new properties map is non-null.
     * </p>
     *
     * createdAt field is not updatable
     * @param event
     * @return the event passed as parameter.
     * @throws TechnicalException
     */
    Event createOrPatch(Event event) throws TechnicalException;

    /**
     * Delete events of a specific API.
     *
     * @param apiId the API id.
     * @return the number of deleted events.
     * @throws TechnicalException
     */
    long deleteApiEvents(String apiId) throws TechnicalException;

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
