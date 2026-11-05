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
package io.gravitee.apim.infra.crud_service.event;

import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.infra.adapter.EventAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class EventLatestCrudServiceLegacyWrapper implements EventLatestCrudService {

    private final EventService eventService;
    private final EventLatestRepository eventLatestRepository;

    public EventLatestCrudServiceLegacyWrapper(EventService eventService, @Lazy EventLatestRepository eventLatestRepository) {
        this.eventService = eventService;
        this.eventLatestRepository = eventLatestRepository;
    }

    @Override
    public Event createOrPatchLatestEvent(String organizationId, String latestEventId, Event event) {
        eventService.createOrPatchLatestEvent(latestEventId, organizationId, EventAdapter.INSTANCE.toEntity(event));
        return event;
    }

    @Override
    public void delete(String latestEventId) {
        try {
            eventLatestRepository.delete(latestEventId);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to delete latest event " + latestEventId, e);
        }
    }
}
