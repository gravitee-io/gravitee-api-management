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
package io.gravitee.repository.noop.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Event;
import java.util.Collections;
import java.util.List;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpEventRepository extends AbstractNoOpManagementRepository<Event, String> implements EventRepository {

    @Override
    public Page<Event> search(EventCriteria filter, Pageable pageable) {
        return new Page<>(List.of(), 0, 0, 0L);
    }

    @Override
    public List<Event> search(EventCriteria filter) {
        return List.of();
    }

    @Override
    public Event createOrPatch(Event event) throws TechnicalException {
        return null;
    }

    @Override
    public long deleteApiEvents(String apiId) throws TechnicalException {
        return 0;
    }

    @Override
    public List<Event> findByEnvironmentId(String environmentId) {
        return Collections.emptyList();
    }

    @Override
    public List<Event> findByOrganizationId(String organizationId) {
        return Collections.emptyList();
    }
}
