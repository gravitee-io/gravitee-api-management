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
 * @author David BRASSELY (david at graviteesource.com)
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
